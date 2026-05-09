package com.proxymaze.util;

import java.net.URI;
import java.net.URISyntaxException;


public class ProxyIdExtractor {
    private ProxyIdExtractor() {}

    public static String extractId(String url) {
        if (url == null || url.isBlank()) return "unknown";
        try {
            URI uri = new URI(url.trim());
            String path = uri.getPath();
            if (path == null || path.isEmpty()) return url.trim();

            while (path.endsWith("/")) {
                path = path.substring(0, path.length() - 1);
            }

            int lastSlash = path.lastIndexOf('/');
            if (lastSlash != -1) {
                return path.substring(lastSlash + 1);
            }
            return path;
        } catch (URISyntaxException e) {
            String normalized = url.trim();
            int queryStart = normalized.indexOf('?');
            if (queryStart != -1) normalized = normalized.substring(0, queryStart);
            while (normalized.endsWith("/")) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            int lastSlash = normalized.lastIndexOf('/');
            return (lastSlash != -1) ? normalized.substring(lastSlash + 1) : normalized;
        }
    }
}