package com.inditex.priceservice.application.usecase;

import com.inditex.priceservice.domain.exception.PriceNotFoundException;
import com.inditex.priceservice.domain.model.Price;
import com.inditex.priceservice.domain.port.in.GetApplicablePriceUseCase;
import com.inditex.priceservice.domain.port.out.FindPricePort;
import lombok.extern.slf4j.Slf4j;

import java.time.Instant;

@Slf4j
public class GetApplicablePriceUseCaseImpl implements GetApplicablePriceUseCase {

    private final FindPricePort findPricePort;

    public GetApplicablePriceUseCaseImpl(FindPricePort findPricePort) {
        this.findPricePort = findPricePort;
    }

    @Override
    public Price execute(Instant applicationDate, Long productId, Long brandId) {
        log.debug("Looking up price: productId={}, brandId={}, date={}", productId, brandId, applicationDate);
        return findPricePort.findApplicablePrice(applicationDate, productId, brandId)
                .orElseThrow(() -> new PriceNotFoundException(productId, brandId));
    }
}
