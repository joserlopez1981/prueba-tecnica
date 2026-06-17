package com.inditex.priceservice.infrastructure.adapter.persistence.adapter;

import com.inditex.priceservice.domain.model.Price;
import com.inditex.priceservice.domain.port.out.FindPricePort;
import com.inditex.priceservice.infrastructure.adapter.persistence.mapper.PricePersistenceMapper;
import com.inditex.priceservice.infrastructure.adapter.persistence.repository.PriceJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PricePersistenceAdapter implements FindPricePort {

    private final PriceJpaRepository priceJpaRepository;
    private final PricePersistenceMapper pricePersistenceMapper;

    @Override
    @Cacheable(value = "prices")
    public List<Price> findCandidatePrices(Instant applicationDate, Long productId, Long brandId) {
        log.debug("Querying DB for candidate prices: productId={}, brandId={}, date={}", productId, brandId, applicationDate);
        return priceJpaRepository.findCandidatePrices(productId, brandId, applicationDate)
                .stream()
                .map(pricePersistenceMapper::toDomain)
                .toList();
    }
}
