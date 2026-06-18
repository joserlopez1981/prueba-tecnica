package com.inditex.priceservice.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
public class CacheConfig {

    @Bean
    public CaffeineCache pricesCache(CacheProperties props) {
        return new CaffeineCache("prices",
                Caffeine.newBuilder()
                        .expireAfterWrite(props.ttlMinutes(), TimeUnit.MINUTES)
                        .maximumSize(props.maxSize())
                        .build());
    }
}
