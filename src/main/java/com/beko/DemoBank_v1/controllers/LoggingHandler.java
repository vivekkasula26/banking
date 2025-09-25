package com.beko.DemoBank_v1.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.*;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class LoggingHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(LoggingHandler.class);
    private final String name;

    public LoggingHandler(String name) {
        this.name = name;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("{} connected: {}", name, session.getId());
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.warn("{} disconnected: {} ({})", name, session.getId(), status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        log.info("{} received: {}", name, message.getPayload());
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("{} transport error: {}", name, exception.getMessage(), exception);
    }
}
