package com.proxymaze.exception;

public class ProxyNotFoundException extends RuntimeException {
    public ProxyNotFoundException(String id) {
        super("Proxy not found: " + id);
    }
}