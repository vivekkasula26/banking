package com.beko.DemoBank_v1.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class LoanService {

    // Unbounded cache for all loan computations
    private static final Map<String, Map<String, Object>> loanComputationCache = new HashMap<>();

    public Map<String, Object> calculateLoan(String customerId, double principal, double rate, int years) {
        double interest = principal * rate * years / 100.0;
        double totalAmount = principal + interest;

        Map<String, Object> result = new HashMap<>();
        result.put("customerId", customerId);
        result.put("principal", principal);
        result.put("rate", rate);
        result.put("years", years);
        result.put("interest", interest);
        result.put("totalAmount", totalAmount);

        // Store result in global cache (OOM risk)
        String key = UUID.randomUUID().toString();
        loanComputationCache.put(key, result);

        return result;
    }

    public int getCacheSize() {
        return loanComputationCache.size();
    }

    public void clearCache() {
        loanComputationCache.clear();
    }
}