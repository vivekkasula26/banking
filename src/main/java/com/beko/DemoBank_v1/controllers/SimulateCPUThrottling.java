package com.beko.DemoBank_v1.controllers;

import java.util.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.beko.DemoBank_v1.models.TransactionHistory;
import com.beko.DemoBank_v1.repository.TransactHistoryRepository;
import com.beko.DemoBank_v1.service.AppService;
import com.beko.DemoBank_v1.service.ReportGenerationService;

@RestController
@RequestMapping("/cpu")
public class SimulateCPUThrottling {

    private final TransactHistoryRepository transactHistoryRepository;
    private final ReportGenerationService reportService;

    @Autowired
    public SimulateCPUThrottling(TransactHistoryRepository transactHistoryRepository,ReportGenerationService reportService) {
        this.transactHistoryRepository = transactHistoryRepository;
        this.reportService = reportService;
    }

    private static final Logger log = LoggerFactory.getLogger(SimulateCPUThrottling.class);

    private static final long BASELINE_MS = 2000;

  
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


@GetMapping("/analyze_transactions_repo")
public ResponseEntity<String> analyzeTransactionsRepo(@RequestParam(defaultValue = "100") int iterations) {
    try {


    // Directly fetch all transactions for account_id = 1
    List<TransactionHistory> transactions = transactHistoryRepository.getTransactionRecordsByAccountId(1);

    long start = System.currentTimeMillis();
    double aggregateScore = 0;

    for (int iter = 0; iter < iterations; iter++) {
        double score = 0;

        // Nested loop over all transactions
        for (int i = 0; i < transactions.size(); i++) {
            double amountI = transactions.get(i).getAmount(); 

            for (int j = i + 1; j < transactions.size(); j++) {
                double amountJ = transactions.get(j).getAmount();
                double diff = Math.abs(amountI - amountJ);
                score += Math.log1p(diff) + Math.sqrt(amountI * amountJ);
            }
        }

        aggregateScore += score;
    }

    long duration = System.currentTimeMillis() - start;

    return ResponseEntity.ok("Processed " + transactions.size() + " transactions over " + iterations +
            " iteration(s). Aggregate interaction score: " + aggregateScore +
            " in " + duration + " ms");
             } 
             
    catch (Exception e) {
        return ResponseEntity.status(500).body("Error processing transactions: " + e.getMessage());    }
}



  @GetMapping("/cpu-report-threaded")
    public ResponseEntity<String> simulateCpuReport() {
        // Fetch all transactions once
        List<TransactionHistory> transactions =
                transactHistoryRepository.getTransactionRecordsByAccountId(1);

        // Split into 3 chunks for threads
        int total = transactions.size();
        int chunkSize = total / 3;

        Thread t1 = new Thread(() ->
                reportService.generateReport(transactions.subList(0, chunkSize)));
        Thread t2 = new Thread(() ->
                reportService.generateReport(transactions.subList(chunkSize, 2 * chunkSize)));
        Thread t3 = new Thread(() ->
                reportService.generateReport(transactions.subList(2 * chunkSize, total)));

        t1.start();
        t2.start();
        t3.start();

        return ResponseEntity.ok("Started CPU-heavy report generation on 3 threads with passed data.");
    }


}
