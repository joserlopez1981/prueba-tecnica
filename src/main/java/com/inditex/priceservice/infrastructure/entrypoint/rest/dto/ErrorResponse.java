package com.inditex.priceservice.infrastructure.entrypoint.rest.dto;

import java.time.Instant;

public record ErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message
) {
}
