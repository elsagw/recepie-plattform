package com.recipenetwork.backend.recipe;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.recipenetwork.backend.common.ApiException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SsrfGuardTest {

    private final SsrfGuard guard = new SsrfGuard();

    @ParameterizedTest
    @ValueSource(strings = {
            "127.0.0.1",       // loopback
            "10.0.0.1",        // RFC 1918 private
            "192.168.1.1",     // RFC 1918 private
            "172.16.0.1",      // RFC 1918 private
            "169.254.169.254", // link-local / cloud metadata endpoint
            "100.64.0.1",      // carrier-grade NAT (RFC 6598)
            "0.0.0.0",         // any-local
            "::1",             // IPv6 loopback
            "fc00::1",         // IPv6 unique local (RFC 4193)
    })
    void blocksPrivateAndInternalAddresses(String host) {
        assertThatThrownBy(() -> guard.assertHostIsPublic(host)).isInstanceOf(ApiException.class);
    }

    @Test
    void allowsPublicAddress() {
        assertThatCode(() -> guard.assertHostIsPublic("8.8.8.8")).doesNotThrowAnyException();
    }

    @Test
    void blocksUnresolvableHost() {
        assertThatThrownBy(() -> guard.assertHostIsPublic("this-host-does-not-exist.invalid"))
                .isInstanceOf(ApiException.class);
    }
}
