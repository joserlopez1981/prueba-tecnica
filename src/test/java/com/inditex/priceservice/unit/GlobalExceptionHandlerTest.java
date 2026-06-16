package com.inditex.priceservice.unit;

import com.inditex.priceservice.domain.exception.PriceNotFoundException;
import com.inditex.priceservice.infrastructure.entrypoint.rest.controller.GlobalExceptionHandler;
import com.inditex.priceservice.infrastructure.entrypoint.rest.dto.ErrorResponse;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    @DisplayName("PriceNotFoundException → 404 Not Found")
    void handlePriceNotFoundException_returns404() {
        var ex = new PriceNotFoundException(35455L, 1L);

        ResponseEntity<ErrorResponse> response = handler.handlePriceNotFoundException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(404);
        assertThat(response.getBody().error()).isEqualTo("Not Found");
        assertThat(response.getBody().message()).contains("35455").contains("1");
    }

    @Test
    @DisplayName("ConstraintViolationException con violaciones → 400 Bad Request")
    void handleConstraintViolationException_withViolations_returns400() {
        ConstraintViolation<?> violation = mock(ConstraintViolation.class);
        var path = mock(jakarta.validation.Path.class);
        when(path.toString()).thenReturn("getApplicablePrice.productId");
        when(violation.getPropertyPath()).thenReturn(path);
        when(violation.getMessage()).thenReturn("must be greater than 0");

        var ex = new ConstraintViolationException("Constraint violation", Set.of(violation));

        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().error()).isEqualTo("Bad Request");
        assertThat(response.getBody().message()).contains("must be greater than 0");
    }

    @Test
    @DisplayName("ConstraintViolationException sin violaciones → fallback al mensaje de la excepción")
    void handleConstraintViolationException_withoutViolations_returns400WithFallbackMessage() {
        var ex = new ConstraintViolationException("fallback message", Set.of());

        ResponseEntity<ErrorResponse> response = handler.handleConstraintViolationException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().message()).isEqualTo("fallback message");
    }

    @Test
    @DisplayName("MethodArgumentTypeMismatchException → 400 Bad Request")
    void handleTypeMismatch_returns400() {
        var ex = mock(MethodArgumentTypeMismatchException.class);
        when(ex.getName()).thenReturn("applicationDate");
        when(ex.getValue()).thenReturn("not-a-date");
        when(ex.getRequiredType()).thenAnswer(inv -> java.time.Instant.class);

        ResponseEntity<ErrorResponse> response = handler.handleTypeMismatch(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).contains("applicationDate").contains("not-a-date");
    }

    @Test
    @DisplayName("MissingServletRequestParameterException → 400 Bad Request")
    void handleMissingParameter_returns400() {
        var ex = new MissingServletRequestParameterException("brandId", "Long");

        ResponseEntity<ErrorResponse> response = handler.handleMissingParameter(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(400);
        assertThat(response.getBody().message()).contains("brandId").contains("Long");
    }

    @Test
    @DisplayName("Exception genérica → 500 Internal Server Error sin detalles internos")
    void handleGenericException_returns500WithoutInternalDetails() {
        var ex = new RuntimeException("internal details that should not be exposed");

        ResponseEntity<ErrorResponse> response = handler.handleGenericException(ex);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().status()).isEqualTo(500);
        assertThat(response.getBody().error()).isEqualTo("Internal Server Error");
        // mensaje genérico, no expone detalles internos
        assertThat(response.getBody().message()).doesNotContain("internal details");
    }
}
