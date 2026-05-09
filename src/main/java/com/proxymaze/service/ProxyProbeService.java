package com.proxymaze.service;

import com.proxymaze.model.ProxyEntry;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.TimeUnit;


@Service
public class ProxyProbeService {

    private static final Logger log = LoggerFactory.getLogger(ProxyProbeService.class);
    private final OkHttpClient sharedClient = new OkHttpClient.Builder()
            .followRedirects(true)
            .retryOnConnectionFailure(true)
            .build();

    public ProbeResult probe(ProxyEntry proxy, int timeoutMs) {
        OkHttpClient callClient = sharedClient.newBuilder()
                .callTimeout(timeoutMs, TimeUnit.MILLISECONDS)
                .build();

        Request request = new Request.Builder()
                .url(proxy.getUrl())
                .header("User-Agent", "ProxyMaze/1.0.0")
                .get()
                .build();

        long start = System.nanoTime();

        try (Response response = callClient.newCall(request).execute()) {
            long elapsed = (System.nanoTime() - start) / 1_000_000;
            boolean success = response.isSuccessful(); 
            return new ProbeResult(success, elapsed);
        } catch (IOException e) {
            long elapsed = (System.nanoTime() - start) / 1_000_000;
            return new ProbeResult(false, elapsed);
        }
    }

    public record ProbeResult(boolean success, long responseTimeMs) {}
}