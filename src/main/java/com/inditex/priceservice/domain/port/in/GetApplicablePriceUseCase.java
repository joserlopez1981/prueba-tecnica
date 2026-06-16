package com.inditex.priceservice.domain.port.in;

import com.inditex.priceservice.domain.model.Price;

import java.time.Instant;

public interface GetApplicablePriceUseCase {

    Price execute(
            Instant applicationDate,
            Long productId,
            Long brandId
    );
}