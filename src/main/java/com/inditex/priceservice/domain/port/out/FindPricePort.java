package com.inditex.priceservice.domain.port.out;

import com.inditex.priceservice.domain.model.Price;

import java.time.Instant;
import java.util.Optional;

public interface FindPricePort {

    Optional<Price> findApplicablePrice(Instant applicationDate, Long productId, Long brandId);
}
