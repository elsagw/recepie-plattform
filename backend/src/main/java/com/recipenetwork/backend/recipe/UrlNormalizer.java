package com.recipenetwork.backend.recipe;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Normalizes a pasted URL before it's used as the {@code source_url} cache key:
 * trims whitespace, drops the fragment, and strips known tracking query params.
 */
@Component
public class UrlNormalizer {

    private static final Pattern TRACKING_PARAM =
            Pattern.compile("^(utm_[a-z0-9_]+|fbclid|gclid)$", Pattern.CASE_INSENSITIVE);

    public String normalize(String rawUrl) {
        String trimmed = rawUrl == null ? "" : rawUrl.trim();

        String withoutFragment = trimmed.split("#", 2)[0];

        int queryIndex = withoutFragment.indexOf('?');
        if (queryIndex < 0) {
            return withoutFragment;
        }

        String base = withoutFragment.substring(0, queryIndex);
        String query = withoutFragment.substring(queryIndex + 1);
        String filteredQuery = stripTrackingParams(query);

        return filteredQuery.isEmpty() ? base : base + "?" + filteredQuery;
    }

    private String stripTrackingParams(String rawQuery) {
        return Arrays.stream(rawQuery.split("&"))
                .filter(pair -> !pair.isEmpty())
                .filter(pair -> {
                    String key = pair.contains("=") ? pair.substring(0, pair.indexOf('=')) : pair;
                    return !TRACKING_PARAM.matcher(key).matches();
                })
                .collect(Collectors.joining("&"));
    }
}
