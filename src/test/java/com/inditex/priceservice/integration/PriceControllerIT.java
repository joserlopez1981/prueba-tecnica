package com.inditex.priceservice.integration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for PriceController.
 * <p>
 * Test dates are converted from Europe/Madrid (UTC+2 in June) to UTC:
 * Madrid 10:00 = UTC 08:00  |  Madrid 15:00 = UTC 13:00
 * Madrid 16:00 = UTC 14:00  |  Madrid 18:30 = UTC 16:30
 * Madrid 21:00 = UTC 19:00  |  Madrid 00:00 = UTC 22:00 (previous day)
 */
@SpringBootTest
@AutoConfigureMockMvc
class PriceControllerIT {

    private static final String ENDPOINT = "/api/v1/prices";
    private static final String PRODUCT_ID = "35455";
    private static final String BRAND_ID = "1";

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("Test 1: 10:00h on June 14 → Price list 1 (35.50 EUR)")
    void test1_at10hOnJune14_shouldReturnPriceList1() throws Exception {
        // 10:00 Madrid time (UTC+2) = 08:00 UTC
        mockMvc.perform(get(ENDPOINT)
                        .param("applicationDate", "2020-06-14T08:00:00Z")
                        .param("productId", PRODUCT_ID)
                        .param("brandId", BRAND_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(35455))
                .andExpect(jsonPath("$.brandId").value(1))
                .andExpect(jsonPath("$.priceList").value(1))
                .andExpect(jsonPath("$.startDate").value("2020-06-13T22:00:00Z"))
                .andExpect(jsonPath("$.endDate").value("2020-12-31T22:59:59Z"))
                .andExpect(jsonPath("$.amount").value(35.50))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    @DisplayName("Test 2: 16:00h on June 14 → Price list 2 (25.45 EUR)")
    void test2_at16hOnJune14_shouldReturnPriceList2() throws Exception {
        // 16:00 Madrid time (UTC+2) = 14:00 UTC — within price list 2 window (13:00–16:30 UTC)
        mockMvc.perform(get(ENDPOINT)
                        .param("applicationDate", "2020-06-14T14:00:00Z")
                        .param("productId", PRODUCT_ID)
                        .param("brandId", BRAND_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(35455))
                .andExpect(jsonPath("$.brandId").value(1))
                .andExpect(jsonPath("$.priceList").value(2))
                .andExpect(jsonPath("$.startDate").value("2020-06-14T13:00:00Z"))
                .andExpect(jsonPath("$.endDate").value("2020-06-14T16:30:00Z"))
                .andExpect(jsonPath("$.amount").value(25.45))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    @DisplayName("Test 3: 21:00h on June 14 → Price list 1 (35.50 EUR)")
    void test3_at21hOnJune14_shouldReturnPriceList1() throws Exception {
        // 21:00 Madrid time (UTC+2) = 19:00 UTC — price list 2 ended at 16:30 UTC, price list 3 starts at 22:00 UTC
        mockMvc.perform(get(ENDPOINT)
                        .param("applicationDate", "2020-06-14T19:00:00Z")
                        .param("productId", PRODUCT_ID)
                        .param("brandId", BRAND_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(35455))
                .andExpect(jsonPath("$.brandId").value(1))
                .andExpect(jsonPath("$.priceList").value(1))
                .andExpect(jsonPath("$.startDate").value("2020-06-13T22:00:00Z"))
                .andExpect(jsonPath("$.endDate").value("2020-12-31T22:59:59Z"))
                .andExpect(jsonPath("$.amount").value(35.50))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    @DisplayName("Test 4: 10:00h on June 15 → Price list 3 (30.50 EUR)")
    void test4_at10hOnJune15_shouldReturnPriceList3() throws Exception {
        // 10:00 Madrid time (UTC+2) = 08:00 UTC — within price list 3 window (22:00 June 14 – 09:00 June 15 UTC)
        mockMvc.perform(get(ENDPOINT)
                        .param("applicationDate", "2020-06-15T08:00:00Z")
                        .param("productId", PRODUCT_ID)
                        .param("brandId", BRAND_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(35455))
                .andExpect(jsonPath("$.brandId").value(1))
                .andExpect(jsonPath("$.priceList").value(3))
                .andExpect(jsonPath("$.startDate").value("2020-06-14T22:00:00Z"))
                .andExpect(jsonPath("$.endDate").value("2020-06-15T09:00:00Z"))
                .andExpect(jsonPath("$.amount").value(30.50))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    @DisplayName("Test 5: 21:00h on June 16 → Price list 4 (38.95 EUR)")
    void test5_at21hOnJune16_shouldReturnPriceList4() throws Exception {
        // 21:00 Madrid time (UTC+2) = 19:00 UTC — price list 4 started at 14:00 UTC June 15
        mockMvc.perform(get(ENDPOINT)
                        .param("applicationDate", "2020-06-16T19:00:00Z")
                        .param("productId", PRODUCT_ID)
                        .param("brandId", BRAND_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.productId").value(35455))
                .andExpect(jsonPath("$.brandId").value(1))
                .andExpect(jsonPath("$.priceList").value(4))
                .andExpect(jsonPath("$.startDate").value("2020-06-15T14:00:00Z"))
                .andExpect(jsonPath("$.endDate").value("2020-12-31T22:59:59Z"))
                .andExpect(jsonPath("$.amount").value(38.95))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    @DisplayName("Boundary: exact start of PriceList 2 → Price list 2 (25.45 EUR)")
    void testBoundary_exactStartOfPriceList2_shouldReturnPriceList2() throws Exception {
        // PL-2 startDate = 2020-06-14T13:00:00Z (Madrid 15:00 - 2h)
        mockMvc.perform(get(ENDPOINT)
                        .param("applicationDate", "2020-06-14T13:00:00Z")
                        .param("productId", PRODUCT_ID)
                        .param("brandId", BRAND_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceList").value(2))
                .andExpect(jsonPath("$.amount").value(25.45))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    @DisplayName("Boundary: exact end of PriceList 2 → Price list 2 (25.45 EUR)")
    void testBoundary_exactEndOfPriceList2_shouldReturnPriceList2() throws Exception {
        // PL-2 endDate = 2020-06-14T16:30:00Z (Madrid 18:30 - 2h)
        mockMvc.perform(get(ENDPOINT)
                        .param("applicationDate", "2020-06-14T16:30:00Z")
                        .param("productId", PRODUCT_ID)
                        .param("brandId", BRAND_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.priceList").value(2))
                .andExpect(jsonPath("$.amount").value(25.45))
                .andExpect(jsonPath("$.currency").value("EUR"));
    }

    @Test
    @DisplayName("No applicable price returns 404")
    void whenNoPriceFound_shouldReturn404() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .param("applicationDate", "2000-01-01T00:00:00Z")
                        .param("productId", "99999")
                        .param("brandId", "99")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"));
    }

    @Test
    @DisplayName("Missing required parameter returns 400")
    void whenMissingParameter_shouldReturn400() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .param("applicationDate", "2020-06-14T08:00:00Z")
                        .param("productId", PRODUCT_ID)
                        // brandId missing
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("Invalid date format returns 400")
    void whenInvalidDateFormat_shouldReturn400() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .param("applicationDate", "not-a-date")
                        .param("productId", PRODUCT_ID)
                        .param("brandId", BRAND_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("Non-positive productId (0) triggers constraint violation → 400")
    void whenProductIdIsZero_shouldReturn400() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .param("applicationDate", "2020-06-14T08:00:00Z")
                        .param("productId", "0")
                        .param("brandId", BRAND_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }

    @Test
    @DisplayName("Non-positive brandId (negative) triggers constraint violation → 400")
    void whenBrandIdIsNegative_shouldReturn400() throws Exception {
        mockMvc.perform(get(ENDPOINT)
                        .param("applicationDate", "2020-06-14T08:00:00Z")
                        .param("productId", PRODUCT_ID)
                        .param("brandId", "-1")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Bad Request"));
    }
}
