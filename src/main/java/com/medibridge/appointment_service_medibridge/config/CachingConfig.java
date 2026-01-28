package com.medibridge.appointment_service_medibridge.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Caching Configuration
 * Enables caching for doctor information to reduce API calls
 */
@Configuration
@EnableCaching
public class CachingConfig {

    /**
     * Configure cache manager for caching doctor information
     * Uses in-memory ConcurrentMapCacheManager for simplicity
     * Can be replaced with Redis for distributed caching
     */
    @Bean
    public CacheManager cacheManager() {
        return new ConcurrentMapCacheManager("doctorInfo");
    }
}
