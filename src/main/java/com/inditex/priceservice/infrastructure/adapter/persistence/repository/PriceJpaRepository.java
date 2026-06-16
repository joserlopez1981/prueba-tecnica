package com.inditex.priceservice.infrastructure.adapter.persistence.repository;

import com.inditex.priceservice.infrastructure.adapter.persistence.entity.PriceEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;

public interface PriceJpaRepository extends JpaRepository<PriceEntity, Long> {

    @Query("""
            SELECT p FROM PriceEntity p
            WHERE p.productId = :productId
              AND p.brandId = :brandId
              AND p.startDate <= :applicationDate
              AND p.endDate >= :applicationDate
            ORDER BY p.priority DESC
            LIMIT 1
            """)
    Optional<PriceEntity> findApplicablePrice(
            @Param("productId") Long productId,
            @Param("brandId") Long brandId,
            @Param("applicationDate") Instant applicationDate
    );
}
