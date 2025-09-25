package com.beko.DemoBank_v1.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class NotificationHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(NotificationHandler.class);
    private final Set<WebSocketSession> sessions = Collections.synchronizedSet(new HashSet<>());
    private final int MAX_SESSIONS = 5; // simulate server limit

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        if (sessions.size() >= MAX_SESSIONS) {
            log.error("Too many open connections! Closing session {}", session.getId());
            session.close(CloseStatus.SERVICE_OVERLOAD); // triggers IOException on client
            return;
        }

        sessions.add(session);
        log.info("Connection established: {} | Total sessions: {}", session.getId(), sessions.size());
    }

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) {
        log.info("Received message from {}: {}", session.getId(), message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("Transport error on session {}: {}", session.getId(), exception.getMessage(), exception);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        sessions.remove(session);
        log.info("Connection closed: {} | Reason: {} | Total sessions: {}", session.getId(), status, sessions.size());
    }
}
