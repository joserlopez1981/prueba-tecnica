package com.inditex.priceservice.domain.model;

import java.math.BigDecimal;
import java.time.Instant;

public record Price(
        Long brandId,
        Long productId,
        Integer priceList,
        Integer priority,
        Instant startDate,
        Instant endDate,
        BigDecimal price,
        String currency
) {
}