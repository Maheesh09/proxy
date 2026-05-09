package com.proxymaze.service;

import com.proxymaze.model.ProxyEntry;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * Makes real HTTP probes to proxy URLs.
 * Returns true if proxy is UP (2xx response), false for any failure.
 */
@Service
public class ProxyProbeService {

    private static final Logger log = LoggerFactory.getLogger(ProxyProbeService.class);

    /**
     * Probe a proxy URL with given timeout.
     * @param proxy the proxy entry to check
     * @param timeoutMs HTTP request timeout in milliseconds
     * @return probe result containing success flag and response time
     */
    public ProbeResult probe(ProxyEntry proxy, int timeoutMs) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .readTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .writeTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .followRedirects(true)
                .build();

        Request request = new Request.Builder()
                .url(proxy.getUrl())
                .get()
                .build();

        long start = System.currentTimeMillis();

        try (Response response = client.newCall(request).execute()) {
            long elapsed = System.currentTimeMillis() - start;
            boolean success = response.isSuccessful(); // 2xx
            log.debug("Probe {} → HTTP {} ({}ms)", proxy.getUrl(), response.code(), elapsed);
            return new ProbeResult(success, elapsed);
        } catch (IOException e) {
            long elapsed = System.currentTimeMillis() - start;
            log.debug("Probe {} → FAILED: {} ({}ms)", proxy.getUrl(), e.getMessage(), elapsed);
            return new ProbeResult(false, elapsed);
        }
    }

    public record ProbeResult(boolean success, long responseTimeMs) {}
}
