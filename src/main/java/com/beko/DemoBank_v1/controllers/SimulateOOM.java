package com.beko.DemoBank_v1.controllers;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import com.beko.DemoBank_v1.models.TransactionHistory;
import com.beko.DemoBank_v1.repository.TransactHistoryRepository;
import com.beko.DemoBank_v1.service.AppService;

import javax.servlet.http.HttpSession;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;

@Controller
@RequestMapping("/oom")
public class SimulateOOM {

     @Autowired
    private AppService appService;

    @Autowired
    private TransactHistoryRepository transactHistoryRepository;

    // === State for leaks ===
    private final Map<String, Object> sessionCache = new ConcurrentHashMap<>();
    private final List<Thread> threads = new ArrayList<>();
    private final Queue<byte[]> queue = new LinkedList<>();


    // === Scenario 3.3: File upload loads into memory ===
    @PostMapping("/upload-file")
    public ResponseEntity<?> uploadFileUnsafe(@RequestParam("file") MultipartFile multipartFile) throws Exception {
        byte[] fileBytes = multipartFile.getBytes();

        int sizeInMB = fileBytes.length / (1024 * 1024);
        return ResponseEntity.ok("Loaded file fully into memory size: " + sizeInMB + " MB");
    }

    @GetMapping("/queue-leak")
    public ResponseEntity<?> queueLeak(@RequestParam(defaultValue = "1000") int chunks) {
        for (int i = 0; i < chunks; i++) {
            queue.add(new byte[1024 * 1024]); // 1 MB
        }
        return ResponseEntity.ok("Queue size: " + queue.size());
    }

    @GetMapping("/cache-leak")
    public ResponseEntity<?> cacheLeak(@RequestParam(defaultValue = "10000") int sessions) {
        for (int i = 0; i < sessions; i++) {
            String key = UUID.randomUUID().toString();
            byte[] payload = new byte[1024 * 100]; // 100KB
            sessionCache.put(key, payload);
        }
        return ResponseEntity.ok("Added " + sessions + " sessions. Total cache size: " + sessionCache.size());
    }

    @PostMapping("/account_transaction_history")
    public ResponseEntity<?> getAccountTransactionHistory(@RequestBody Map<String, String> requestMap, HttpSession session) {

        return appService.getAccountTransactionHistory(requestMap);
    }


    private static final Map<String, Map<String, Object>> sessions = new HashMap<>();

    private Map<String, Object> generateBigSession(int cartItems, int notesSizeKb) {
        Map<String, Object> sessionData = new HashMap<>();
        sessionData.put("userId", UUID.randomUUID().toString());
        sessionData.put("username", "user_" + ThreadLocalRandom.current().nextInt(100000));

        // Big cart
        List<Map<String, Object>> cart = new ArrayList<>();
        for (int i = 0; i < cartItems; i++) {
            Map<String, Object> item = new HashMap<>();
            item.put("productId", i);
            item.put("name", "Product_" + i);
            item.put("price", ThreadLocalRandom.current().nextDouble(10, 500));
            // Add a "big" string field to inflate memory
            char[] notes = new char[notesSizeKb * 1024];
            Arrays.fill(notes, 'X');
            item.put("notes", new String(notes));
            cart.add(item);
        }
        sessionData.put("cart", cart);

        return sessionData;
    }


    @PostMapping("/simulate-login")
    public ResponseEntity<String> simulateLogin(@RequestParam(defaultValue = "1000") int users) {
        for (int i = 0; i < users; i++) {
            sessions.put("user-" + i, generateBigSession(15, 50)); // 15 items × 50KB each
        }
        return ResponseEntity.ok("Added " + users + " users to session cache. Current size: " + sessions.size());
    }



    @PostMapping("/export-transactions-csv")
    public ResponseEntity<String> exportTransactionsCsv(@RequestBody Map<String, String> requestMap) {
        try {
            String account_id = requestMap.get("account_id");
            int accountId = Integer.parseInt(account_id);

            // Fetch all transactions for the account
            List<TransactionHistory> transactions = transactHistoryRepository
                    .getTransactionRecordsByAccountId(accountId);

            if (transactions.isEmpty()) {
                return ResponseEntity.ok("No transactions found for account: " + accountId);
            }

            // Build CSV in memory
            StringBuilder csvBuilder = new StringBuilder();
            csvBuilder.append("TransactionID,AccountID,Type,Amount,Source,Status,ReasonCode,CreatedAt\n");

            for (TransactionHistory t : transactions) {
                csvBuilder.append(t.getTransaction_id()).append(",")
                          .append(t.getAccount_id()).append(",")
                          .append(t.getTransaction_type()).append(",")
                          .append(t.getAmount()).append(",")
                          .append(t.getSource()).append(",")
                          .append(t.getStatus()).append(",")
                          .append(t.getReason_code()).append(",")
                          .append(t.getCreated_at()).append("\n");
            }

            // Return CSV
            return ResponseEntity.ok()
                    .header("Content-Type", "text/csv")
                    .header("Content-Disposition", "attachment; filename=transaction_history.csv")
                    .body(csvBuilder.toString());

        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Error generating transaction CSV: " + e.getMessage());
        }
    }

    @GetMapping("/thread-leak")
    public ResponseEntity<?> threadLeak(@RequestParam(defaultValue = "100") int count) {
        for (int i = 0; i < count; i++) {
            Thread t = new Thread(() -> {
                try {
                    Thread.sleep(Long.MAX_VALUE);
                } catch (InterruptedException ignored) {}
            });
            t.start();
            threads.add(t);
        }
        return ResponseEntity.ok("Started " + count + " new threads. Total threads: " + threads.size());
    }



    @GetMapping("/multi-thread-oom")
    public ResponseEntity<String> multiThreadOOM() {

        // Thread 1: Fetch all transaction history for account 1
        Thread t1 = new Thread(() -> {
            try {
                Map<String, String> requestMap = new HashMap<>();
                requestMap.put("account_id", "1");
                getAccountTransactionHistory(requestMap, null);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Thread 2: Generate CSV report of transaction history
        Thread t2 = new Thread(() -> {
            try {
                Map<String, String> requestMap = new HashMap<>();
                requestMap.put("account_id", "1");
                exportTransactionsCsv(requestMap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        // Thread 3: Simulate OOM by generating big sessions
        Thread t3 = new Thread(() -> simulateLogin(50)); // 250 users

        // Thread 4: Queue memory leak
        Thread t4 = new Thread(() -> queueLeak(50)); // 100 MB

        // Start all threads
        t1.start();
        t2.start();
        t3.start();
        t4.start();

        // Keep track of threads
        threads.addAll(Arrays.asList(t1, t2, t3, t4));

        return ResponseEntity.ok("Started 4 threads: transaction fetch, report generation, simulate OOM, queue leak.");
    }
  

 

    // === Cleanup Endpoint ===
    @DeleteMapping("/cleanup")
    public ResponseEntity<?> cleanup() {
        int cacheSize = sessionCache.size();
        int queueSize = queue.size();
        int threadCount = threads.size();

        sessionCache.clear();
        queue.clear();

        for (Thread t : threads) {
            t.interrupt();
        }
        threads.clear();

        System.gc();

        return ResponseEntity.ok(String.format(
            "Cleanup done. Cleared %d cache entries, %d queue items, %d threads.",
            cacheSize, queueSize, threadCount
        ));
    }
}
