package com.inditex.priceservice.application.usecase;

import com.inditex.priceservice.domain.exception.PriceNotFoundException;
import com.inditex.priceservice.domain.model.Price;
import com.inditex.priceservice.domain.port.in.GetApplicablePriceUseCase;
import com.inditex.priceservice.domain.port.out.FindPricePort;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

@Slf4j
public class GetApplicablePriceUseCaseImpl implements GetApplicablePriceUseCase {

    private final FindPricePort findPricePort;

    public GetApplicablePriceUseCaseImpl(FindPricePort findPricePort) {
        this.findPricePort = findPricePort;
    }

    @Override
    public Price execute(Instant applicationDate, Long productId, Long brandId) {
        List<Price> candidates = Objects.requireNonNullElse(
                findPricePort.findCandidatePrices(applicationDate, productId, brandId), List.of());
        log.debug("Found {} candidate price(s) for productId={}, brandId={}", candidates.size(), productId, brandId);

        return candidates.stream()
                .filter(p -> p.isApplicableAt(applicationDate))
                .max(Comparator.comparingInt(Price::priority)
                        .thenComparing(Price::startDate))
                .map(price -> {
                    log.info("Applicable price selected: priceList={}, priority={}, amount={}",
                            price.priceList(), price.priority(), price.amount());
                    return price;
                })
                .orElseThrow(() -> {
                    log.info("No applicable price found for productId={}, brandId={}, date={}",
                            productId, brandId, applicationDate);
                    return new PriceNotFoundException(productId, brandId);
                });
    }
}

