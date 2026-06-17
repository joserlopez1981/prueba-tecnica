package com.inditex.priceservice.unit;

import com.inditex.priceservice.domain.model.Price;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PriceTest {

    private static final Instant START = Instant.parse("2020-06-14T13:00:00Z");
    private static final Instant END   = Instant.parse("2020-06-14T16:30:00Z");

    private final Price price = new Price(1L, 35455L, 2, 1, START, END, new BigDecimal("25.45"), "EUR");

    @Test
    void isApplicableAt_returnsTrue_whenDateIsWithinRange() {
        assertThat(price.isApplicableAt(Instant.parse("2020-06-14T15:00:00Z"))).isTrue();
    }

    @Test
    void isApplicableAt_returnsTrue_whenDateIsExactlyAtStart() {
        assertThat(price.isApplicableAt(START)).isTrue();
    }

    @Test
    void isApplicableAt_returnsTrue_whenDateIsExactlyAtEnd() {
        assertThat(price.isApplicableAt(END)).isTrue();
    }

    @Test
    void isApplicableAt_returnsFalse_whenDateIsBeforeRange() {
        assertThat(price.isApplicableAt(Instant.parse("2020-06-14T12:59:59Z"))).isFalse();
    }

    @Test
    void isApplicableAt_returnsFalse_whenDateIsAfterRange() {
        assertThat(price.isApplicableAt(Instant.parse("2020-06-14T16:30:01Z"))).isFalse();
    }
}
