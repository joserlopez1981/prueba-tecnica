package com.inditex.priceservice.infrastructure.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.cache.prices")
@Validated
public record CacheProperties(
        @Min(1) int ttlMinutes,
        @Min(1) long maxSize
) {
}
