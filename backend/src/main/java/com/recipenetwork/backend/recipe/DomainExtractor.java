package com.recipenetwork.backend.recipe;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class DomainExtractor {

    public String extract(String url) {
        try {
            String host = new URI(url).getHost();
            if (host == null) {
                return null;
            }
            String lower = host.toLowerCase(Locale.ROOT);
            return lower.startsWith("www.") ? lower.substring(4) : lower;
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
