package com.beko.DemoBank_v1.controllers;

import com.beko.DemoBank_v1.models.User;
import com.beko.DemoBank_v1.repository.TransactRepository;
import com.beko.DemoBank_v1.repository.UserRepository;
import com.beko.DemoBank_v1.service.AppService;
import com.beko.DemoBank_v1.service.TransactService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Controller
@RequestMapping("/dbpool")
public class SimulateDBPool {

    @Autowired
    private AppService appService;

    private final UserRepository userRepository;

     User user;

    @Autowired
    private TransactService transactService;

     @Autowired
    private TransactRepository transactRepository;

    @Autowired
    public SimulateDBPool(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    // Hardcoded request map for all calls
    private final Map<String, String> fixedRequestMap = new HashMap<String, String>() {{
        put("account_id", "1");
    }};

    private final Map<String, String> email = new HashMap<String, String>() {{
        put("email", "vk@gmail.com");
    }};



     @PostMapping("/exhaust_db")
    public ResponseEntity<String> concurrentLoad(@RequestParam(defaultValue = "50") int threads) {
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    userRepository.holdConnection();

                } catch (Exception e) {
                    System.out.println("ERROR: " + e.getMessage());
                }
            });
        }

        executor.shutdown();
        return ResponseEntity.ok("Launched " + threads + " concurrent account_transaction_history calls");
    }
    

    /**
        Concurrent load - multiple threads hitting account_transaction_history
     */
    @PostMapping("/exhaust_db_and_OOM")
    public ResponseEntity<String> concurrentLoadAndOOM(@RequestParam(defaultValue = "50") int threads) {
        ExecutorService executor = Executors.newFixedThreadPool(threads);

        for (int i = 0; i < threads; i++) {
            executor.submit(() -> {
                try {
                    appService.getAccountTransactionHistory(fixedRequestMap);

                } catch (Exception e) {
                    System.out.println("ERROR: " + e.getMessage());
                }
            });
        }

        executor.shutdown();
        return ResponseEntity.ok("Launched " + threads + " concurrent account_transaction_history calls");
    }

    /**
        Multiple services simulation
     */
    @PostMapping("/multi-service-load")
public ResponseEntity<String> LoadSimulationService(@RequestParam(defaultValue = "30") int threadsPerService) {

     Logger log = LoggerFactory.getLogger(this.getClass());
    int totalThreads = threadsPerService * 2;
    ExecutorService executor = Executors.newFixedThreadPool(totalThreads);

    Runnable insertTask = () -> {
        try {
            User user = new User();
            user.setUser_id(1);

            Map<String, String> requestMap = new HashMap<>();
            requestMap.put("deposit_amount", "10000");
            requestMap.put("account_id", "1");

            transactService.deposit(requestMap, user);
            Thread.sleep(3000);
        } catch (Exception e) {
             log.error("Error in insertTask thread: {}", e.getMessage(), e);
        }
    };

    Runnable updateTask = () -> {
        try {
            transactRepository.complexQuery(900000.0); // use your repo method
            Thread.sleep(5000);
        } catch (Exception e) {
            log.error("Error in updateTask thread: {}", e.getMessage(), e);
        }
    };

    for (int i = 0; i < threadsPerService; i++) {
        executor.submit(insertTask);
        executor.submit(updateTask);
    }

    executor.shutdown();

    return ResponseEntity.ok("Launched load simulation with total threads: " + totalThreads);
}


}
