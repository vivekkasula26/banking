package com.beko.DemoBank_v1.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;


@RestController
@RequestMapping("/network")
public class SimulateNetworkTimeouts {

    private static final Logger log = LoggerFactory.getLogger(SimulateNetworkTimeouts.class);

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * 1. External API call without timeout → hangs if server is slow/unresponsive.
     * Example: GET /network/external_api
     * Fix: Always set connectTimeout and readTimeout
     */
    @GetMapping("/external_api")
    public ResponseEntity<String> simulateExternalApiTimeout() {
        try {
            // Calling a non-routable IP (10.255.255.1) → guaranteed to hang/timeout
            String url = "http://10.255.255.1:8080/unreachable";
            log.info("Making external API call to {}", url);

            // No timeout configured → may hang indefinitely
            String response = restTemplate.getForObject(url, String.class);

            return ResponseEntity.ok("Response: " + response);
        } catch (Exception e) {
            log.error("External API timeout simulation triggered", e);
            return ResponseEntity.status(504).body("Gateway Timeout: " + e.getMessage());
        }
    }

    /**
     * 2. Slow streaming response → client stuck waiting for server to finish.
     * Example: GET /network/slow_stream
     * Add read timeouts or switch to async streaming.
     */
    @GetMapping("/slow_stream")
    public ResponseEntity<String> simulateNetwork(@RequestParam(defaultValue = "timeout") String mode,
                                                  @RequestParam(defaultValue = "6") int readTimeoutSeconds,
                                                  @RequestParam(defaultValue = "10") int maxWaitSeconds) {
        ExecutorService executor = Executors.newSingleThreadExecutor();

        Future<String> future = executor.submit(() -> {
            URL url;
            if ("hang".equalsIgnoreCase(mode)) {
                // Endpoint that hangs indefinitely (no response until killed)
                url = new URL("https://httpbin.org/delay/300"); // 5 min hang
            } else {
                // Endpoint that streams slowly → will trigger read timeout
                url = new URL("https://httpbin.org/drip?numbytes=5000&duration=30&delay=5");
            }

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(readTimeoutSeconds * 1000);

            try (Scanner scanner = new Scanner(conn.getInputStream())) {
                StringBuilder sb = new StringBuilder();
                while (scanner.hasNext()) {
                    sb.append(scanner.next());
                }
                return "Downloaded: " + sb.length() + " bytes";
            }
        });

        try {
            // Force max wait — if exceeded, treat as hang
            String result = future.get(maxWaitSeconds, TimeUnit.SECONDS);
            return ResponseEntity.ok(result);
        } catch (TimeoutException te) {
            log.error("Simulation [{}] exceeded {}s → marking as hang", mode, maxWaitSeconds, te);
            return ResponseEntity.status(504).body("Gateway Timeout (hang detected)");
        } catch (Exception e) {
            log.error("Simulation [{}] failed with error", mode, e);
            return ResponseEntity.status(408).body("Request Timeout: " + e.getMessage());
        } finally {
            executor.shutdownNow();
        }
    }


    /**
     * 3. Retry storm → keeps retrying instead of failing fast.
     * Example: GET /network/retry_storm
     * Fix: Use exponential backoff + circuit breakers (Resilience4j/Hystrix)
     */
    @GetMapping("/retry_storm")
    public ResponseEntity<String> simulateRetryStorm() {
        int retries = 0;
        while (retries < 5) {  // misconfigured: should stop earlier
            try {
                URL url = new URL("http://10.255.255.1:9090/unreachable"); 
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(2000);
                conn.connect();
                return ResponseEntity.ok("Connected to unreachable host?");
            } catch (Exception e) {
                retries++;
                log.warn("Retry attempt {} failed: {}", retries, e.getMessage());
            }
        }
        return ResponseEntity.status(504).body("Exhausted retries, service unavailable");
    }
}
