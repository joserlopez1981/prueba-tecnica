package com.inditex.priceservice.unit;

import com.inditex.priceservice.application.usecase.GetApplicablePriceUseCaseImpl;
import com.inditex.priceservice.domain.exception.PriceNotFoundException;
import com.inditex.priceservice.domain.model.Price;
import com.inditex.priceservice.domain.port.out.FindPricePort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetApplicablePriceUseCaseTest {

    @Mock
    private FindPricePort findPricePort;

    @InjectMocks
    private GetApplicablePriceUseCaseImpl useCase;

    @Test
    void shouldReturnPrice_whenApplicablePriceExists() {
        Instant applicationDate = Instant.parse("2020-06-14T10:00:00Z");
        Long productId = 35455L;
        Long brandId = 1L;

        Price expectedPrice = new Price(
                brandId, productId, 1, 0,
                Instant.parse("2020-06-13T22:00:00Z"),
                Instant.parse("2020-12-31T21:59:59Z"),
                new BigDecimal("35.50"), "EUR"
        );

        when(findPricePort.findApplicablePrice(applicationDate, productId, brandId))
                .thenReturn(Optional.of(expectedPrice));

        Price result = useCase.execute(applicationDate, productId, brandId);

        assertThat(result).isEqualTo(expectedPrice);
    }

    @Test
    void shouldThrowPriceNotFoundException_whenNoPriceExists() {
        Instant applicationDate = Instant.now();
        Long productId = 99999L;
        Long brandId = 99L;

        when(findPricePort.findApplicablePrice(applicationDate, productId, brandId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> useCase.execute(applicationDate, productId, brandId))
                .isInstanceOf(PriceNotFoundException.class)
                .hasMessageContaining(String.valueOf(productId))
                .hasMessageContaining(String.valueOf(brandId));
    }
}
