package com.inditex.priceservice.domain.exception;

public class PriceNotFoundException extends RuntimeException {

    public PriceNotFoundException(
            Long productId,
            Long brandId
    ) {

        super(
                String.format(
                        "No applicable price found for product %s and brand %s",
                        productId,
                        brandId
                )
        );
    }
}