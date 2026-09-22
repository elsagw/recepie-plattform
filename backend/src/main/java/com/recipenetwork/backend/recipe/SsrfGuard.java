package com.recipenetwork.backend.recipe;

import com.recipenetwork.backend.common.ApiException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Blocks scraping requests from reaching private/internal network addresses.
 *
 * Resolves the host and validates every returned address up front, and the caller
 * (SafeHtmlFetcher) re-runs this check on every redirect hop, which blocks the common
 * "public URL redirects to an internal one" attack outright. What this does NOT fully
 * close is a true DNS-rebinding race: the JDK HttpClient re-resolves the hostname itself
 * a moment after we validate it here, so an attacker controlling the domain's DNS server
 * could in theory swap the answer between our check and the actual connection. Closing
 * that completely requires pinning the connection to the resolved IP (custom TLS/SNI
 * handling), which is a deliberately deferred hardening item, not implemented in the MVP.
 */
@Component
public class SsrfGuard {

    public void assertHostIsPublic(String host) {
        InetAddress[] addresses;
        try {
            addresses = InetAddress.getAllByName(host);
        } catch (UnknownHostException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_URL", "Kunde inte slå upp värdnamnet.");
        }

        if (addresses.length == 0) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_URL", "Kunde inte slå upp värdnamnet.");
        }

        for (InetAddress address : addresses) {
            if (!isPublic(address)) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "URL_NOT_ALLOWED",
                        "URL:en pekar på en privat eller intern adress och kan inte hämtas.");
            }
        }
    }

    private boolean isPublic(InetAddress address) {
        if (address.isLoopbackAddress()
                || address.isLinkLocalAddress() // includes the 169.254.169.254 cloud metadata endpoint
                || address.isSiteLocalAddress() // RFC 1918 IPv4 private ranges
                || address.isMulticastAddress()
                || address.isAnyLocalAddress()) {
            return false;
        }

        byte[] bytes = address.getAddress();
        if (bytes.length == 4) {
            return !isCarrierGradeNat(bytes);
        }
        if (bytes.length == 16) {
            return !isUniqueLocalIpv6(bytes);
        }
        return true;
    }

    private boolean isCarrierGradeNat(byte[] ipv4) {
        // 100.64.0.0/10 (RFC 6598) - not covered by InetAddress's own checks.
        int first = ipv4[0] & 0xFF;
        int second = ipv4[1] & 0xFF;
        return first == 100 && second >= 64 && second <= 127;
    }

    private boolean isUniqueLocalIpv6(byte[] ipv6) {
        // fc00::/7 (RFC 4193) - InetAddress#isSiteLocalAddress() only checks the older,
        // deprecated fec0::/10 range, not the one actually used for internal IPv6 today.
        return (ipv6[0] & 0xFE) == 0xFC;
    }
}
