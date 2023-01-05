package com.raycho.chat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.raycho.chat.model.Message;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChatHandler extends TextWebSocketHandler {

    private static final ConcurrentHashMap<String, WebSocketSession> CLIENTS
            = new ConcurrentHashMap<>();

    // 웹소켓 연결
    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String id = session.getId();
        CLIENTS.put(id, session);

        Message message = Message.builder().type("").sender(id).receiver("all").build();
        message.newConnect();

        CLIENTS.forEach((key, value) -> {
            if (!key.equals(id)) {
                try {
                    value.sendMessage(new TextMessage(getString(message)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // 웹소켓 종료
    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String id = session.getId();
        CLIENTS.remove(id);

        Message message = Message.builder().sender(id).receiver("all").build();
        message.newConnect();

        CLIENTS.forEach((key, value) -> {
            if (!key.equals(id)) {
                try {
                    value.sendMessage(new TextMessage(getString(message)));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    // 웹소켓 에러
    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        super.handleTransportError(session, exception);
    }

    // 데이터 통신
    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage textMessage) throws Exception {
        Message message = getObject(textMessage.getPayload());
        message.setSender(session.getId());

        WebSocketSession receiver = CLIENTS.get(message.getReceiver());

        if (receiver != null && receiver.isOpen()) {
            receiver.sendMessage(new TextMessage(getString(message)));
        }
    }

    static ObjectMapper mapper = new ObjectMapper();
    public static String getString(final Object object) throws JsonProcessingException {
        return mapper.writeValueAsString(object);
    }
    public static Message getObject(final String message) throws JsonProcessingException {
        return mapper.readValue(message, Message.class);
    }
}
