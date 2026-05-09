package com.proxymaze.util;

import java.net.URI;

public class ProxyIdExtractor {
    private ProxyIdExtractor() {}

    public static String extractId(String url) {
        try {
            URI uri = new URI(url.trim());
            String path = uri.getPath();
            if (path != null && !path.isEmpty()) {
                String[] parts = path.split("/");
                for (int i = parts.length - 1; i >= 0; i--) {
                    String seg = parts[i].trim();
                    if (!seg.isEmpty()) return seg;
                }
            }
        } catch (Exception ignored) {}
        return IdGenerator.generateId();
    }
}
