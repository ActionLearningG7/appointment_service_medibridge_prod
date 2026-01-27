package com.medibridge.appointment_service_medibridge.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.net.URI;

/**
 * Redis Configuration
 * 
 * Supports both URL format and individual properties:
 * - spring.data.redis.url (preferred)
 * - spring.data.redis.host, port, password (fallback)
 * 
 * Provides:
 * 1. RedisTemplate for queue state caching
 * 2. RedissonClient for distributed locks
 */
@Configuration
@EnableCaching
@ConditionalOnProperty(name = "spring.data.redis.enabled", havingValue = "true", matchIfMissing = true)
public class RedisConfig {

    @Value("${spring.data.redis.url:}")
    private String redisUrl;

    @Value("${spring.data.redis.host:localhost}")
    private String redisHost;

    @Value("${spring.data.redis.port:6379}")
    private int redisPort;

    @Value("${spring.data.redis.password:}")
    private String redisPassword;

    /**
     * Parse Redis connection details from URL or individual properties
     */
    private RedisConnectionDetails getConnectionDetails() {
        if (redisUrl != null && !redisUrl.isEmpty()) {
            // Parse URL format: redis://[user:password@]host:port[/database]
            try {
                URI uri = URI.create(redisUrl);
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 6379;
                String password = null;

                if (uri.getUserInfo() != null) {
                    String[] userInfo = uri.getUserInfo().split(":");
                    if (userInfo.length > 1) {
                        password = userInfo[1];
                    }
                }

                return new RedisConnectionDetails(host, port, password);
            } catch (Exception e) {
                throw new IllegalArgumentException("Invalid Redis URL: " + redisUrl, e);
            }
        } else {
            // Use individual properties
            return new RedisConnectionDetails(redisHost, redisPort,
                    redisPassword != null && !redisPassword.isEmpty() ? redisPassword : null);
        }
    }

    /**
     * Redis Template for key-value operations
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Use String for keys
        template.setKeySerializer(new StringRedisSerializer());
        template.setHashKeySerializer(new StringRedisSerializer());

        // Use JSON for values
        template.setValueSerializer(new GenericJackson2JsonRedisSerializer());
        template.setHashValueSerializer(new GenericJackson2JsonRedisSerializer());

        template.afterPropertiesSet();
        return template;
    }

    /**
     * Redisson Client for distributed locks (RLock)
     */
    @Bean
    public RedissonClient redissonClient() {
        RedisConnectionDetails details = getConnectionDetails();

        Config config = new Config();
        String address = "redis://" + details.host + ":" + details.port;

        config.useSingleServer()
                .setAddress(address)
                .setPassword(details.password);

        return Redisson.create(config);
    }

    /**
     * Internal class to hold Redis connection details
     */
    private static class RedisConnectionDetails {
        final String host;
        final int port;
        final String password;

        RedisConnectionDetails(String host, int port, String password) {
            this.host = host;
            this.port = port;
            this.password = password;
        }
    }
}
