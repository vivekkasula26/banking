package com.beko.DemoBank_v1.controllers;

import com.beko.DemoBank_v1.service.LoanService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/loan")
public class LoanController {

    @Autowired
    private LoanService loanService;

    // Calculate loan for a customer and store result in memory
    @PostMapping("/calculate")
    public ResponseEntity<?> calculateLoan(@RequestBody Map<String, Object> payload) {
        String customerId = payload.getOrDefault("customerId", UUID.randomUUID().toString()).toString();
        double principal = Double.parseDouble(payload.getOrDefault("principal", "10000").toString());
        double rate = Double.parseDouble(payload.getOrDefault("rate", "7.5").toString());
        int years = Integer.parseInt(payload.getOrDefault("years", "5").toString());

        Map<String, Object> result = loanService.calculateLoan(customerId, principal, rate, years);
        return ResponseEntity.ok(result);
    }

    // OOM Simulation: Trigger many loan calculations to fill memory
    @PostMapping("/simulate-oom")
    public ResponseEntity<?> simulateOOM(@RequestParam(defaultValue = "10000") int count) {
        for (int i = 0; i < count; i++) {
            loanService.calculateLoan(
                "customer_" + i,
                10000 + i,
                7.5 + (i % 5),
                5 + (i % 10)
            );
        }
        return ResponseEntity.ok("Simulated " + count + " loan computations. Cache size: " + loanService.getCacheSize());
    }

    // Cleanup endpoint
    @DeleteMapping("/cleanup")
    public ResponseEntity<?> cleanup() {
        loanService.clearCache();
        return ResponseEntity.ok("Loan computation cache cleared.");
    }
}