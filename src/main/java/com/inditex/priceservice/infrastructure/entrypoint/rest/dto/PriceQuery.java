package com.inditex.priceservice.infrastructure.entrypoint.rest.dto;

import java.time.Instant;

public record PriceQuery(
        Instant applicationDate,
        Long productId,
        Long brandId
) {
}
