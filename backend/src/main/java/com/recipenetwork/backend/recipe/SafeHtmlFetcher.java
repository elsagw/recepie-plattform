package com.recipenetwork.backend.recipe;

import com.recipenetwork.backend.common.ApiException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

/**
 * Fetches a page's HTML while enforcing the scraping safety rules from
 * docs/ARCHITECTURE.md: HTTP/HTTPS only, a resolved-IP check (SsrfGuard) re-run on every
 * redirect hop, a request timeout, a response size cap, and an HTML content-type check.
 */
@Component
public class SafeHtmlFetcher {

    private static final int MAX_REDIRECTS = 5;
    private static final long MAX_BODY_BYTES = 2 * 1024 * 1024; // 2 MB
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);

    private final SsrfGuard ssrfGuard;
    private final HttpClient httpClient;

    public SafeHtmlFetcher(SsrfGuard ssrfGuard) {
        this.ssrfGuard = ssrfGuard;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(REQUEST_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    public record FetchResult(String html, String finalUrl) {
    }

    public FetchResult fetch(String url) {
        String currentUrl = url;

        for (int hop = 0; hop < MAX_REDIRECTS; hop++) {
            URI uri = parseAndValidate(currentUrl);
            ssrfGuard.assertHostIsPublic(uri.getHost());

            HttpResponse<InputStream> response = send(uri);
            int status = response.statusCode();

            if (status >= 300 && status < 400) {
                String location = response.headers().firstValue("Location")
                        .orElseThrow(() -> new ApiException(HttpStatus.BAD_GATEWAY, "FETCH_FAILED",
                                "Kunde inte hämta sidan (omdirigering utan mål)."));
                currentUrl = resolveRedirect(uri, location);
                continue;
            }

            if (status < 200 || status >= 300) {
                throw new ApiException(HttpStatus.BAD_GATEWAY, "FETCH_FAILED",
                        "Kunde inte hämta sidan (status " + status + ").");
            }

            String contentType = response.headers().firstValue("Content-Type").orElse("");
            if (!contentType.toLowerCase(Locale.ROOT).contains("text/html")) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "NOT_HTML", "URL:en pekar inte på en HTML-sida.");
            }

            String html = readBounded(response.body());
            return new FetchResult(html, uri.toString());
        }

        throw new ApiException(HttpStatus.BAD_GATEWAY, "TOO_MANY_REDIRECTS", "För många omdirigeringar.");
    }

    private URI parseAndValidate(String url) {
        URI uri;
        try {
            uri = URI.create(url);
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_URL", "Ogiltig URL.");
        }

        String scheme = uri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_URL", "Endast HTTP/HTTPS tillåts.");
        }
        if (uri.getHost() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_URL", "Ogiltig URL.");
        }
        return uri;
    }

    private String resolveRedirect(URI base, String location) {
        try {
            return base.resolve(location).toString();
        } catch (IllegalArgumentException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "FETCH_FAILED", "Ogiltig omdirigerings-URL.");
        }
    }

    private HttpResponse<InputStream> send(URI uri) {
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(REQUEST_TIMEOUT)
                .header("User-Agent", "receptfeed-bot/1.0")
                .GET()
                .build();
        try {
            return httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "FETCH_FAILED", "Kunde inte hämta sidan.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(HttpStatus.BAD_GATEWAY, "FETCH_FAILED", "Kunde inte hämta sidan.");
        }
    }

    private String readBounded(InputStream inputStream) {
        try (inputStream) {
            byte[] buffer = new byte[8192];
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            long total = 0;
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                total += read;
                if (total > MAX_BODY_BYTES) {
                    throw new ApiException(HttpStatus.BAD_GATEWAY, "RESPONSE_TOO_LARGE", "Sidan var för stor.");
                }
                out.write(buffer, 0, read);
            }
            return out.toString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_GATEWAY, "FETCH_FAILED", "Kunde inte läsa sidan.");
        }
    }
}
