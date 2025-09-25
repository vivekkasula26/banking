package com.beko.DemoBank_v1.websocket;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

public class ChatHandler extends TextWebSocketHandler {

    @Override
    public void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        System.out.println("ChatHandler received: " + message.getPayload());
        session.sendMessage(new TextMessage("Echo from ChatHandler: " + message.getPayload()));
    }
}
