package com.inditex.priceservice.infrastructure.adapter.persistence.adapter;

import com.inditex.priceservice.domain.model.Price;
import com.inditex.priceservice.domain.port.out.FindPricePort;
import com.inditex.priceservice.infrastructure.adapter.persistence.mapper.PricePersistenceMapper;
import com.inditex.priceservice.infrastructure.adapter.persistence.repository.PriceJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class PricePersistenceAdapter implements FindPricePort {

    private final PriceJpaRepository priceJpaRepository;
    private final PricePersistenceMapper pricePersistenceMapper;

    @Override
    public Optional<Price> findApplicablePrice(Instant applicationDate, Long productId, Long brandId) {
        log.debug("Querying DB: productId={}, brandId={}, date={}", productId, brandId, applicationDate);
        return priceJpaRepository.findApplicablePrice(productId, brandId, applicationDate)
                .map(pricePersistenceMapper::toDomain);
    }
}
