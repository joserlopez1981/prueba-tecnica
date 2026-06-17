package com.inditex.priceservice.domain.port.out;

import com.inditex.priceservice.domain.model.Price;

import java.time.Instant;
import java.util.List;

public interface FindPricePort {

    List<Price> findCandidatePrices(Instant applicationDate, Long productId, Long brandId);
}
