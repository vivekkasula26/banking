package com.beko.DemoBank_v1.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Random;

@RestController
@RequestMapping("/ephimeral")
public class SimulateEphimeral {

    @GetMapping("/fill-disk")
    public ResponseEntity<String> fillDisk(@RequestParam(defaultValue = "1") int fileCount) {
        File tempDir = new File(System.getProperty("java.io.tmpdir"));
        Random random = new Random();

        try {
            for (int i = 0; i < fileCount; i++) {
                File file = new File(tempDir, "fill_" + i + ".tmp");
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    byte[] buffer = new byte[1024 * 1024]; // 1 MB buffer
                    for (int j = 0; j < 50; j++) { // ~50 MB per file
                        random.nextBytes(buffer);
                        fos.write(buffer);
                    }
                }
                System.out.println("Created file #" + i + ": " + file.getAbsolutePath());
            }
            return ResponseEntity.ok("Created " + fileCount + " files in temp dir.");
        } catch (IOException e) {
            return ResponseEntity.status(500)
                    .body("Disk might be full! Error: " + e.getMessage());
        }
    }
}
