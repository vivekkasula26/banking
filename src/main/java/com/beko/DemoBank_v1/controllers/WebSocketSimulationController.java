package com.beko.DemoBank_v1.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.TextMessage;


@RestController
@RequestMapping("/ws-sim")
public class WebSocketSimulationController {

    private static final Logger log = LoggerFactory.getLogger(WebSocketSimulationController.class);

    @GetMapping("/idle-socket")
    public ResponseEntity<String> idleSocket(@RequestParam(defaultValue = "16") long durationSeconds) {
        new Thread(() -> {
            try {
                StandardWebSocketClient client = new StandardWebSocketClient();
                WebSocketSession session = client
                        .doHandshake(new LoggingHandler("IdleSocket"), "ws://localhost:8070/ws/notifications")
                        .get();

                log.info("Idle socket connected. Sleeping {} seconds...", durationSeconds);
                Thread.sleep(durationSeconds * 1000); // leave idle
                log.info("Idle socket finished waiting.");
            } catch (Exception e) {
                log.error("Idle socket error (real): {}", e.getMessage(), e);
            }
        }).start();
        return ResponseEntity.ok("Idle socket started. Watch logs for Connection reset errors.");
    }

    // ================================
    // 2️⃣ Multiple Sockets
    // ================================
    @GetMapping("/multi-socket")
    public ResponseEntity<String> multiSocket(@RequestParam(defaultValue = "10") int count) {
        new Thread(() -> {
            StandardWebSocketClient client = new StandardWebSocketClient();
            for (int i = 0; i < count; i++) {
                try {
                    WebSocketSession session = client
                            .doHandshake(new LoggingHandler("MultiSocket-" + i), "ws://localhost:8070/ws/notifications")
                            .get();
                    log.info("Connected session {}", session.getId());
                } catch (Exception e) {
                    log.error("Multi-socket error (real): {}", e.getMessage(), e); // triggers IOException
                }
            }
        }).start();
        return ResponseEntity.ok("Opening " + count + " sockets. Watch logs for errors.");
    }

    // ================================
    // 3️⃣ Heartbeat Disabled (trigger SocketTimeoutException / Connection reset)
    // ================================
    @GetMapping("/heartbeat-disabled")
    public ResponseEntity<String> heartbeatDisabled() {
        new Thread(() -> {
            try {
                StandardWebSocketClient client = new StandardWebSocketClient();
                WebSocketSession session = client
                        .doHandshake(new LoggingHandler("NoHeartbeat"), "ws://localhost:8070/ws/notifications")
                        .get();

                // Do not send ping, server randomly closes connection
                Thread.sleep(30000);
            } catch (Exception e) {
                log.error("Heartbeat disabled error (real): {}", e.getMessage(), e);
            }
        }).start();
        return ResponseEntity.ok("Heartbeat disabled simulation started. Watch logs for SocketTimeoutException.");
    }

    // ================================
    // 4️⃣ Flooding messages (trigger IOException / Connection reset)
    // ================================
    @GetMapping("/flood-messages")
    public ResponseEntity<String> floodMessages(@RequestParam(defaultValue = "1") long intervalMs,
                                                @RequestParam(defaultValue = "10000") int messages,
                                                @RequestParam(defaultValue = "1000") int sizePerMessage) {
        new Thread(() -> {
            try {
                StandardWebSocketClient client = new StandardWebSocketClient();
                WebSocketSession session = client
                        .doHandshake(new LoggingHandler("FloodWS"), "ws://localhost:8070/ws/notifications")
                        .get();

                for (int i = 0; i < messages; i++) {
                    if (session.isOpen()) {
                        try {
                            session.sendMessage(new TextMessage(("Flood" + i).repeat(sizePerMessage)));
                        } catch (Exception e) {
                            log.error("Flood message error (real): {}", e.getMessage(), e); // real IOException
                        }
                    }
                    Thread.sleep(intervalMs);
                }
            } catch (Exception e) {
                log.error("Flood WS setup error: {}", e.getMessage(), e);
            }
        }).start();
        return ResponseEntity.ok("Flooding messages started. Watch logs for IOException / Connection reset.");
    }
}
