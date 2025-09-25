package com.beko.DemoBank_v1.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;


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
    public ResponseEntity<String> simulateSlowStream() {
        try {
            URL url = new URL("https://httpbin.org/drip?numbytes=5000&duration=30&delay=5"); 
            // This API drips bytes slowly over 30 seconds → simulates a slow DB/ETL export
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(8_000); // Try changing this to see timeout vs hang

            Scanner scanner = new Scanner(conn.getInputStream());
            StringBuilder sb = new StringBuilder();
            while (scanner.hasNext()) {
                sb.append(scanner.next());
            }
            scanner.close();

            return ResponseEntity.ok("Downloaded: " + sb.length() + " bytes");
        } catch (Exception e) {
            log.error("Slow stream timeout simulation triggered", e);
            return ResponseEntity.status(408).body("Request Timeout: " + e.getMessage());
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
