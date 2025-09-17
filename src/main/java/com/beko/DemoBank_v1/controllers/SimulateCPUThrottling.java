package com.beko.DemoBank_v1.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/cpu")
public class SimulateCPUThrottling {

    private static final Logger log = LoggerFactory.getLogger(SimulateCPUThrottling.class);

    // Baseline (ms) for normal execution, tune this to your instance
    private static final long BASELINE_MS = 2000;

    /**
     * Run a CPU-intensive prime calculation.
     * Example: GET /cpu/calculate_primes?count=50
     */
    @GetMapping("/calculate_primes")
    public ResponseEntity<String> simulateCPU(@RequestParam(defaultValue = "50000000") int count) {
        long start = System.currentTimeMillis();
        calculateBigPrimes(count);
        long duration = System.currentTimeMillis() - start;

        if (duration > BASELINE_MS) {
            log.warn("⚠️ CPU throttling suspected: task took {} ms (expected < {} ms)", duration, BASELINE_MS);
        } else {
            log.info("✅ CPU task completed in {} ms (within expected range)", duration);
        }

        return ResponseEntity.ok("Calculated " + count + " big primes in " + duration + " ms");
    }

    private void calculateBigPrimes(int count) {
        int found = 0;
        long num = 10_000_000L;
        while (found < count) {
            if (isPrime(num)) {
                found++;
            }
            num++;
        }
    }

    private boolean isPrime(long n) {
        if (n < 2) return false;
        for (long i = 2; i * i <= n; i++) {
            if (n % i == 0) return false;
        }
        return true;
    }
}
