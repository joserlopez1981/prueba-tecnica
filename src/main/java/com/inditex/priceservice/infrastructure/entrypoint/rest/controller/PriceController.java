package com.inditex.priceservice.infrastructure.entrypoint.rest.controller;

import com.inditex.priceservice.domain.port.in.GetApplicablePriceUseCase;
import com.inditex.priceservice.infrastructure.entrypoint.rest.dto.ErrorResponse;
import com.inditex.priceservice.infrastructure.entrypoint.rest.dto.PriceResponse;
import com.inditex.priceservice.infrastructure.entrypoint.rest.mapper.PriceRestMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@Slf4j
@Validated
@RestController
@RequestMapping("/api/v1/prices")
@RequiredArgsConstructor
@Tag(name = "Prices", description = "API for querying applicable prices by product, brand and date")
public class PriceController {

    private final GetApplicablePriceUseCase getApplicablePriceUseCase;
    private final PriceRestMapper priceRestMapper;

    @GetMapping
    @Operation(
            summary = "Get applicable price",
            description = "Returns the applicable price for a product and brand at a given date. " +
                    "When multiple prices overlap, the one with higher priority is returned."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Applicable price found",
                    content = @Content(schema = @Schema(implementation = PriceResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input parameters",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @ApiResponse(responseCode = "404", description = "No applicable price found",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<PriceResponse> getApplicablePrice(
            @Parameter(description = "Date and time of application in ISO-8601 UTC format (e.g. 2020-06-14T08:00:00Z)", required = true)
            @RequestParam @NotNull Instant applicationDate,

            @Parameter(description = "Product identifier", required = true, example = "35455")
            @RequestParam @NotNull @Positive Long productId,

            @Parameter(description = "Brand identifier (1 = ZARA)", required = true, example = "1")
            @RequestParam @NotNull @Positive Long brandId
    ) {
        log.info("Query price: productId={}, brandId={}, applicationDate={}", productId, brandId, applicationDate);
        var price = getApplicablePriceUseCase.execute(applicationDate, productId, brandId);
        return ResponseEntity.ok(priceRestMapper.toResponse(price));
    }
}
