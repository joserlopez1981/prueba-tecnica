package com.inditex.priceservice.infrastructure.entrypoint.rest.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record PriceResponse(
        Long productId,
        Long brandId,
        Integer priceList,
        Instant startDate,
        Instant endDate,
        BigDecimal price,
        String currency
) {
}
