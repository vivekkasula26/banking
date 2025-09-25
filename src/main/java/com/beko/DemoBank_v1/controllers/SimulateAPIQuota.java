package com.beko.DemoBank_v1.controllers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/rate-limit")
public class SimulateAPIQuota {

    private static final Logger log = LoggerFactory.getLogger(SimulateAPIQuota.class);
    private final RestTemplate restTemplate = new RestTemplate();

    // Uses X-RateLimit-Remaining and X-RateLimit-Reset 

    @GetMapping("/github-rate-limit")
    public String spamGitHub(@RequestParam(defaultValue = "100") int count) {
        String url = "https://api.github.com/users/octocat";

        for (int i = 0; i < count; i++) {
            try {
                ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                log.info("Request {} → Status: {}, Remaining: {}",
                        i + 1,
                        response.getStatusCode(),
                        response.getHeaders().getFirst("X-RateLimit-Remaining"));

                if (response.getStatusCode() == HttpStatus.FORBIDDEN ||
                    response.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                    log.warn("Rate limit hit at request {}", i + 1);
                    break;
                }
            } catch (Exception e) {
                log.error("Error on request " + (i + 1), e);
            }
        }
        return "Finished spamming GitHub API " + count + " times. Check logs for rate-limit errors.";
    }


     /**
     * Spam with retry+backoff (fix): tries multiple times if 429 or 403, with exponential backoff
     */
    @GetMapping("/github-spam-retry")
    public ResponseEntity<String> spamGitHubApiWithRetry(@RequestParam(defaultValue = "50") int threads) {
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                int attempt = 0;
                while (attempt < 5) {
                    try {
                        String url = "https://api.github.com/users/octocat";
                        ResponseEntity<String> response = restTemplate.getForEntity(url, String.class);
                        HttpStatus status = response.getStatusCode();
                        log.info("GitHub response status on attempt {}: {}", attempt + 1, status);

                        if (status == HttpStatus.OK) {
                            break;
                        } else if (status == HttpStatus.FORBIDDEN || status == HttpStatus.TOO_MANY_REQUESTS) {
                            // Read Retry-After header maybe
                            String retryAfter = response.getHeaders().getFirst("Retry-After");
                            long wait = 1000L * (long) Math.pow(2, attempt);  // exponential backoff
                            log.warn("Rate limit hit. Waiting {} ms before retry {}. Retry-After: {}", wait, attempt + 1, retryAfter);
                            Thread.sleep(wait);
                            attempt++;
                        } else {
                            // Some other error, break
                            break;
                        }
                    } catch (Exception e) {
                        log.error("Exception calling GitHub API with retry", e);
                        break;
                    }
                }
            });
        }

        executor.shutdown();
        return ResponseEntity.ok("Launched " + threads + " concurrent GitHub API requests WITH retry");
    }


}
