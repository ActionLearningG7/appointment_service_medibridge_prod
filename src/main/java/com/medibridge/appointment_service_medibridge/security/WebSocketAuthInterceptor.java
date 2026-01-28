package com.medibridge.appointment_service_medibridge.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.stereotype.Component;

/**
 * WebSocket Authentication Interceptor
 * 
 * Allows unauthenticated WebSocket connections
 * This matches other WebSocket implementations in the system
 */
@Component
@Slf4j
public class WebSocketAuthInterceptor implements ChannelInterceptor {

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        log.info("🔌 [WebSocketAuthInterceptor] preSend called");
        
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            log.warn("⚠️ [WebSocketAuthInterceptor] No StompHeaderAccessor found");
            return message;
        }

        StompCommand command = accessor.getCommand();
        log.info("🔌 [WebSocketAuthInterceptor] STOMP Command: {}", command);

        if (command == null) {
            log.warn("⚠️ [WebSocketAuthInterceptor] No STOMP command found");
            return message;
        }

        if (StompCommand.CONNECT.equals(command)) {
            log.info("🔌 WebSocket CONNECT - Unauthenticated access allowed");
            // Allow all WebSocket connections without authentication
            // This matches other WebSocket implementations in the system
        }

        return message;
    }
}
