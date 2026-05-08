package com.medibridge.appointment_service_medibridge.config;

import com.medibridge.appointment_service_medibridge.security.WebSocketAuthInterceptor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

/**
 * Enterprise WebSocket Configuration for STOMP + SockJS
 *
 * ARCHITECTURE:
 * - API Gateway handles CORS centrally
 * - This service uses allowedOriginPatterns("*") to TRUST the gateway
 * - Gateway validates origin and adds proper CORS headers
 * - Service focuses on WebSocket logic, not CORS
 *
 * ENDPOINTS:
 * - /ws/info → SockJS handshake (HTTP GET)
 * - /ws/{server}/{session}/... → SockJS transports
 * - WebSocket URL: ws://localhost:8080/ws (via gateway)
 * - Direct URL: ws://localhost:8083/api/v1/ws (bypasses gateway - not
 * recommended)
 *
 * SECURITY:
 * - JWT authentication in STOMP CONNECT headers via WebSocketAuthInterceptor
 * - Token can be provided in:
 * 1. Authorization header: "Authorization: Bearer <token>"
 * 2. Query parameter: ?token=<token>
 * 3. Gateway-forwarded headers: X-User-Id, X-User-Email, X-User-Role
 * - Spring Security CSRF disabled for /ws/** endpoints
 * - Message-level authorization in ChannelInterceptor
 * 
 * CLIENT CONNECTION EXAMPLE:
 * ```javascript
 * const socket = new SockJS('http://localhost:8080/ws');
 * const stompClient = Stomp.over(socket);
 * 
 * stompClient.connect(
 * { Authorization: `Bearer ${jwtToken}` }, // JWT in CONNECT headers
 * (frame) => { console.log('Connected:', frame); },
 * (error) => { console.error('Connection error:', error); }
 * );
 * ```
 */
@Configuration
@EnableWebSocketMessageBroker
@RequiredArgsConstructor
@Slf4j
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    private final WebSocketAuthInterceptor webSocketAuthInterceptor;

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Register WebSocket endpoint with SockJS fallback
        // CORS is handled by API Gateway - no origin patterns needed
        log.info("=== Registering WebSocket endpoint /ws ===");

        registry.addEndpoint("/ws")
                .setAllowedOrigins("http://localhost:3000","https://medibridge-prod.vercel.app/")
                .withSockJS()
                .setHeartbeatTime(45000) // Server heartbeat every 45 seconds
                .setDisconnectDelay(10000) // Wait 10s before disconnecting idle sessions
                .setStreamBytesLimit(512 * 1024)
                .setHttpMessageCacheSize(1000)
                .setSessionCookieNeeded(false)
                .setWebSocketEnabled(true);

        log.info("✅ WebSocket endpoint registered successfully at /ws");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        // In-memory simple broker for /topic and /user destinations
        // Production: Replace with external broker (RabbitMQ, ActiveMQ)
        registry.enableSimpleBroker("/topic", "/user", "/info")
                .setHeartbeatValue(new long[] { 10000, 10000 }) // 10s heartbeat
                .setTaskScheduler(webSocketHeartbeatScheduler()); // Required for heartbeat

        // Client sends messages to /app/...
        // Example: /app/queue/join → @MessageMapping("/queue/join")
        registry.setApplicationDestinationPrefixes("/app");

        // User-specific destinations: /user/{userId}/...
        // Enables point-to-point messaging
        registry.setUserDestinationPrefix("/user");
    }

    /**
     * Configure inbound channel with authentication interceptor
     * This interceptor validates JWT tokens on STOMP CONNECT
     */
    @Override
    public void configureClientInboundChannel(ChannelRegistration registration) {
        registration.interceptors(webSocketAuthInterceptor);
    }

    /**
     * TaskScheduler bean for SimpleBroker heartbeat
     * Required when heartbeat is enabled in SimpleBroker
     *
     * Note: Renamed from 'messageBrokerTaskScheduler' to avoid conflict
     * with Spring's internal bean of the same name in
     * DelegatingWebSocketMessageBrokerConfiguration
     */
    @Bean
    public TaskScheduler webSocketHeartbeatScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(1);
        scheduler.setThreadNamePrefix("ws-heartbeat-");
        scheduler.initialize();
        return scheduler;
    }
}
