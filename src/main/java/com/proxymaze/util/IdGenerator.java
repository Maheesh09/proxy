package com.proxymaze.util;

import java.util.UUID;

public class IdGenerator {
    private IdGenerator() {}

    public static String generateId() {
        return UUID.randomUUID().toString();
    }

    public static String generateShortId(String prefix) {
        return prefix + "-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}