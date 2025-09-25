package com.beko.DemoBank_v1.service;

import com.beko.DemoBank_v1.models.TransactionHistory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ReportGenerationService {

    /**
     * Extremely CPU-intensive O(n^3) sort-like operation
     */
    public void generateReport(List<TransactionHistory> transactions) {
        int n = transactions.size();

        // Extract amounts into an array
        double[] amounts = new double[n];
        for (int i = 0; i < n; i++) {
            amounts[i] = transactions.get(i).getAmount();
        }

        // 🔹 O(n^3) nested loops
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n - 1; j++) {
                for (int k = j + 1; k < n; k++) {
                    if (amounts[j] > amounts[k]) {
                        double temp = amounts[j];
                        amounts[j] = amounts[k];
                        amounts[k] = temp;
                    }
                    // Extra CPU work
                    amounts[j] = Math.pow(amounts[j] + k, 1.01) / (j + 1);
                }
            }
        }

        // Compute some stats
        double sum = 0;
        double max = Double.MIN_VALUE;
        for (double amt : amounts) {
            sum += amt;
            if (amt > max) max = amt;
        }
        double mean = sum / n;

        System.out.println("Thread " + Thread.currentThread().getName() +
                " completed heavy O(n^3) processing: mean=" + mean + ", max=" + max);
    }
}
