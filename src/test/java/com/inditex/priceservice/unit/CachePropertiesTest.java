package com.inditex.priceservice.unit;

import com.inditex.priceservice.infrastructure.config.CacheProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.caffeine.CaffeineCache;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@TestPropertySource(properties = {
        "app.cache.prices.ttl-minutes=10",
        "app.cache.prices.max-size=500"
})
class CachePropertiesTest {

    @Autowired
    private CacheProperties cacheProperties;

    @Autowired
    private CaffeineCache pricesCache;

    @Test
    @DisplayName("CacheProperties carga ttlMinutes desde application properties")
    void shouldLoadTtlMinutesFromProperties() {
        assertThat(cacheProperties.ttlMinutes()).isEqualTo(10);
    }

    @Test
    @DisplayName("CacheProperties carga maxSize desde application properties")
    void shouldLoadMaxSizeFromProperties() {
        assertThat(cacheProperties.maxSize()).isEqualTo(500L);
    }

    @Test
    @DisplayName("CaffeineCache se crea con el nombre 'prices'")
    void shouldCreateCacheWithCorrectName() {
        assertThat(pricesCache.getName()).isEqualTo("prices");
    }

    @Test
    @DisplayName("CaffeineCache se instancia correctamente con los valores de CacheProperties")
    void shouldCreateNativeCacheFromProperties() {
        assertThat(pricesCache.getNativeCache()).isNotNull();
    }
}
