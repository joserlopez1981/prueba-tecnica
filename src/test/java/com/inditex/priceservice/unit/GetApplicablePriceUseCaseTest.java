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
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GetApplicablePriceUseCaseTest {

    @Mock
    private FindPricePort findPricePort;

    @InjectMocks
    private GetApplicablePriceUseCaseImpl useCase;

    private static final Instant DATE = Instant.parse("2020-06-14T14:00:00Z");
    private static final Long PRODUCT_ID = 35455L;
    private static final Long BRAND_ID = 1L;

    @Test
    void shouldReturnPrice_whenSingleCandidateExists() {
        Price expected = priceWithPriority(1, 0, "2020-06-13T22:00:00Z", "2020-12-31T21:59:59Z", "35.50");

        when(findPricePort.findCandidatePrices(DATE, PRODUCT_ID, BRAND_ID))
                .thenReturn(List.of(expected));

        assertThat(useCase.execute(DATE, PRODUCT_ID, BRAND_ID)).isEqualTo(expected);
    }

    @Test
    void shouldReturnHighestPriority_whenMultipleCandidatesExist() {
        Price lowPriority  = priceWithPriority(1, 0, "2020-06-13T22:00:00Z", "2020-12-31T21:59:59Z", "35.50");
        Price highPriority = priceWithPriority(2, 1, "2020-06-14T13:00:00Z", "2020-06-14T16:30:00Z", "25.45");

        when(findPricePort.findCandidatePrices(DATE, PRODUCT_ID, BRAND_ID))
                .thenReturn(List.of(lowPriority, highPriority));

        assertThat(useCase.execute(DATE, PRODUCT_ID, BRAND_ID)).isEqualTo(highPriority);
    }

    @Test
    void shouldThrowPriceNotFoundException_whenNoCandidatesExist() {
        when(findPricePort.findCandidatePrices(DATE, PRODUCT_ID, BRAND_ID))
                .thenReturn(List.of());

        assertThatThrownBy(() -> useCase.execute(DATE, PRODUCT_ID, BRAND_ID))
                .isInstanceOf(PriceNotFoundException.class)
                .hasMessageContaining(String.valueOf(PRODUCT_ID))
                .hasMessageContaining(String.valueOf(BRAND_ID));
    }

    @Test
    void shouldThrowPriceNotFoundException_whenAllCandidatesAreOutsideDateRange() {
        // Defensive domain filter: candidate returned by DB but not applicable at query date
        Price outOfRange = priceWithPriority(1, 0, "2020-06-15T00:00:00Z", "2020-06-15T12:00:00Z", "30.50");

        when(findPricePort.findCandidatePrices(DATE, PRODUCT_ID, BRAND_ID))
                .thenReturn(List.of(outOfRange));

        assertThatThrownBy(() -> useCase.execute(DATE, PRODUCT_ID, BRAND_ID))
                .isInstanceOf(PriceNotFoundException.class);
    }

    // --- helpers ---

    private Price priceWithPriority(int priceList, int priority, String start, String end, String amount) {
        return new Price(BRAND_ID, PRODUCT_ID, priceList, priority,
                Instant.parse(start), Instant.parse(end),
                new BigDecimal(amount), "EUR");
    }
}

