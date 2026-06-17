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
        BigDecimal amount,
        String currency
) {
    /**
     * Returns true if the given date falls within this price's validity period (inclusive on both boundaries).
     */
    public boolean isApplicableAt(Instant date) {
        return !date.isBefore(startDate) && !date.isAfter(endDate);
    }
}