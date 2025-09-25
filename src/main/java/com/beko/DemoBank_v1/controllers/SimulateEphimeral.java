package com.beko.DemoBank_v1.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

@RestController
@RequestMapping("/ephimeral")
public class SimulateEphimeral {

    private static final Logger log = LoggerFactory.getLogger(SimulateEphimeral.class);

    @GetMapping("/safe-fill")
    public ResponseEntity<String> safeFillDisk(@RequestParam(defaultValue = "5") int maxFiles) {

        // Always use /var/tmp
        File tmpDir = new File("/var/tmp");
        int fileCount = 0;

        try {
            while (true) {
                // Simulate disk full once maxFiles reached
                if (fileCount >= maxFiles) {
                    throw new IOException("Simulated disk full error for testing");
                }

                // Optional: create tiny file to show normal behavior
                File file = new File(tmpDir, "simfile_" + fileCount + ".tmp");
                try (FileOutputStream fos = new FileOutputStream(file)) {
                    fos.write(("File #" + fileCount).getBytes());
                }

                log.info("Created file: {}", file.getAbsolutePath());
                fileCount++;
            }
        } catch (IOException e) {
            log.error("Caught simulated disk full error!", e);
            return ResponseEntity.status(500)
                    .body("Simulated disk full error reached! Max files: " + maxFiles);
        }
    }

    @DeleteMapping("/cleanup")
    public ResponseEntity<String> cleanupFiles() {
        File dir = new File("/var/tmp");

        int deletedCount = 0;
        File[] files = dir.listFiles((d, name) -> name.startsWith("simfile_") && name.endsWith(".tmp"));
        if (files != null) {
            for (File f : files) {
                if (f.delete()) deletedCount++;
            }
        }

        return ResponseEntity.ok("Deleted " + deletedCount + " simulated files from /var/tmp");
    }


   private static final long MAX_FOLDER_SIZE_BYTES = 1 * 1024 * 1024; // 1 MB

    @GetMapping("/log-growth")
    public ResponseEntity<String> simulateLogGrowthWithLimit(@RequestParam(defaultValue = "100000") long lines) {
        File logDir = new File("/var/tmp/logs");
        if (!logDir.exists()) logDir.mkdirs();

        File logFile = new File(logDir, "application.log");

        try (FileOutputStream fos = new FileOutputStream(logFile, true)) { // append mode
            for (int i = 0; i < lines; i++) {
                // Check folder size before writing
                long folderSize = getFolderSize(logDir);
                if (folderSize >= MAX_FOLDER_SIZE_BYTES) {
                    throw new IOException("Simulated disk full error! /var/tmp/logs exceeded 5 MB");
                }

                String logLine = "Simulated log line " + i + " - filling disk to test rollover\n";
                fos.write(logLine.getBytes());
            }
        } catch (IOException e) {
            log.error("Disk full simulation reached!", e);
            return ResponseEntity.status(500)
                    .body("Disk full simulation triggered: " + e.getMessage());
        }

        log.info("Generated {} log lines in {}", lines, logFile.getAbsolutePath());
        return ResponseEntity.ok("Generated " + lines + " log lines in /var/tmp/logs/application.log");
    }

    @DeleteMapping("/cleanup-logs")
    public ResponseEntity<String> cleanupLogs() {
        File logDir = new File("/var/tmp/logs");
        int deletedCount = 0;
        File[] files = logDir.listFiles((d, name) -> name.endsWith(".log"));
        if (files != null) {
            for (File f : files) {
                if (f.delete()) deletedCount++;
            }
        }
        return ResponseEntity.ok("Deleted " + deletedCount + " log files from /var/tmp/logs");
    }

    // Utility method to calculate folder size
    private long getFolderSize(File folder) {
        long length = 0;
        File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile()) {
                    length += f.length();
                } else if (f.isDirectory()) {
                    length += getFolderSize(f);
                }
            }
        }
        return length;
    }
}

