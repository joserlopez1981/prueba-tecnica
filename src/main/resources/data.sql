-- =============================================================================
-- PRICE SERVICE — TEST DATA
-- All dates stored in UTC. Madrid offset: Summer CEST=UTC+2, Winter CET=UTC+1
-- =============================================================================

-- =============================================================================
-- BRAND 1 (ZARA) — PRODUCT 35455  [original spec data]
-- =============================================================================

-- PL-1  Base tariff: full period, lowest priority
-- Madrid: 2020-06-14 00:00 → 2020-12-31 23:59  |  UTC: 2020-06-13 22:00 → 2020-12-31 22:59
INSERT INTO PRICES (BRAND_ID, START_DATE, END_DATE, PRICE_LIST, PRODUCT_ID, PRIORITY, PRICE, CURRENCY)
VALUES (1, '2020-06-13 22:00:00', '2020-12-31 22:59:59', 1, 35455, 0, 35.50, 'EUR');

-- PL-2  Afternoon promo June 14 (priority 1 beats PL-1)
-- Madrid: 2020-06-14 15:00 → 18:30  |  UTC: 2020-06-14 13:00 → 16:30
INSERT INTO PRICES (BRAND_ID, START_DATE, END_DATE, PRICE_LIST, PRODUCT_ID, PRIORITY, PRICE, CURRENCY)
VALUES (1, '2020-06-14 13:00:00', '2020-06-14 16:30:00', 2, 35455, 1, 25.45, 'EUR');

-- PL-3  Night tariff June 14→15 (priority 1 beats PL-1)
-- Madrid: 2020-06-15 00:00 → 11:00  |  UTC: 2020-06-14 22:00 → 2020-06-15 09:00
INSERT INTO PRICES (BRAND_ID, START_DATE, END_DATE, PRICE_LIST, PRODUCT_ID, PRIORITY, PRICE, CURRENCY)
VALUES (1, '2020-06-14 22:00:00', '2020-06-15 09:00:00', 3, 35455, 1, 30.50, 'EUR');

-- PL-4  Summer tariff from June 15 afternoon (priority 1 beats PL-1)
-- Madrid: 2020-06-15 16:00 → 2020-12-31 23:59  |  UTC: 2020-06-15 14:00 → 2020-12-31 22:59
INSERT INTO PRICES (BRAND_ID, START_DATE, END_DATE, PRICE_LIST, PRODUCT_ID, PRIORITY, PRICE, CURRENCY)
VALUES (1, '2020-06-15 14:00:00', '2020-12-31 22:59:59', 4, 35455, 1, 38.95, 'EUR');

-- =============================================================================
-- BRAND 1 (ZARA) — PRODUCT 35455  [extended test scenarios]
-- =============================================================================

-- PL-5  Flash sale June 14 noon (priority 2 — beats everything)
-- Madrid: 2020-06-14 12:00 → 12:30  |  UTC: 2020-06-14 10:00 → 10:30
INSERT INTO PRICES (BRAND_ID, START_DATE, END_DATE, PRICE_LIST, PRODUCT_ID, PRIORITY, PRICE, CURRENCY)
VALUES (1, '2020-06-14 10:00:00', '2020-06-14 10:30:00', 5, 35455, 2, 19.99, 'EUR');

-- PL-6  Weekend special July 4–5 (priority 1 beats PL-4 during that weekend)
-- Madrid: 2020-07-04 00:00 → 2020-07-05 23:59  |  UTC: 2020-07-03 22:00 → 2020-07-05 21:59
INSERT INTO PRICES (BRAND_ID, START_DATE, END_DATE, PRICE_LIST, PRODUCT_ID, PRIORITY, PRICE, CURRENCY)
VALUES (1, '2020-07-03 22:00:00', '2020-07-05 21:59:59', 6, 35455, 1, 29.95, 'EUR');

-- PL-7  Black Friday (priority 2 — highest, beats PL-4)
-- Madrid: 2020-11-27 00:00 → 23:59  |  UTC: 2020-11-26 23:00 → 2020-11-27 22:59
INSERT INTO PRICES (BRAND_ID, START_DATE, END_DATE, PRICE_LIST, PRODUCT_ID, PRIORITY, PRICE, CURRENCY)
VALUES (1, '2020-11-26 23:00:00', '2020-11-27 22:59:59', 7, 35455, 2, 22.00, 'EUR');

-- =============================================================================
-- BRAND 1 (ZARA) — PRODUCT 35456  [different product, same brand]
-- =============================================================================

-- PL-1  Base tariff full period
-- UTC: 2020-06-13 22:00 → 2020-12-31 22:59
INSERT INTO PRICES (BRAND_ID, START_DATE, END_DATE, PRICE_LIST, PRODUCT_ID, PRIORITY, PRICE, CURRENCY)
VALUES (1, '2020-06-13 22:00:00', '2020-12-31 22:59:59', 1, 35456, 0, 59.99, 'EUR');

-- PL-2  Summer sale July–August (priority 1)
-- Madrid: 2020-07-01 00:00 → 2020-08-31 23:59  |  UTC: 2020-06-30 22:00 → 2020-08-31 21:59
INSERT INTO PRICES (BRAND_ID, START_DATE, END_DATE, PRICE_LIST, PRODUCT_ID, PRIORITY, PRICE, CURRENCY)
VALUES (1, '2020-06-30 22:00:00', '2020-08-31 21:59:59', 2, 35456, 1, 49.99, 'EUR');

-- PL-3  Clearance last week of December (priority 2)
-- Madrid: 2020-12-26 00:00 → 2020-12-31 23:59  |  UTC: 2020-12-25 23:00 → 2020-12-31 22:59
INSERT INTO PRICES (BRAND_ID, START_DATE, END_DATE, PRICE_LIST, PRODUCT_ID, PRIORITY, PRICE, CURRENCY)
VALUES (1, '2020-12-25 23:00:00', '2020-12-31 22:59:59', 3, 35456, 2, 39.99, 'EUR');

-- =============================================================================
-- BRAND 2 (Pull&Bear) — PRODUCT 35455  [same product, different brand]
-- =============================================================================

-- PL-1  Base tariff full period
-- UTC: 2020-06-13 22:00 → 2020-12-31 22:59
INSERT INTO PRICES (BRAND_ID, START_DATE, END_DATE, PRICE_LIST, PRODUCT_ID, PRIORITY, PRICE, CURRENCY)
VALUES (2, '2020-06-13 22:00:00', '2020-12-31 22:59:59', 1, 35455, 0, 42.00, 'EUR');

-- PL-2  Afternoon promo June 14 (priority 1)
-- Madrid: 2020-06-14 15:00 → 18:30  |  UTC: 2020-06-14 13:00 → 16:30
INSERT INTO PRICES (BRAND_ID, START_DATE, END_DATE, PRICE_LIST, PRODUCT_ID, PRIORITY, PRICE, CURRENCY)
VALUES (2, '2020-06-14 13:00:00', '2020-06-14 16:30:00', 2, 35455, 1, 38.50, 'EUR');

-- PL-3  Summer tariff from June 15 (priority 1)
-- UTC: 2020-06-15 14:00 → 2020-12-31 22:59
INSERT INTO PRICES (BRAND_ID, START_DATE, END_DATE, PRICE_LIST, PRODUCT_ID, PRIORITY, PRICE, CURRENCY)
VALUES (2, '2020-06-15 14:00:00', '2020-12-31 22:59:59', 3, 35455, 1, 45.00, 'EUR');

-- =============================================================================
-- BRAND 2 (Pull&Bear) — PRODUCT 99999  [exclusive product]
-- =============================================================================

-- PL-1  Only base price exists for this product
-- UTC: 2020-06-13 22:00 → 2020-09-30 21:59 (ends Sept 30 Madrid)
INSERT INTO PRICES (BRAND_ID, START_DATE, END_DATE, PRICE_LIST, PRODUCT_ID, PRIORITY, PRICE, CURRENCY)
VALUES (2, '2020-06-13 22:00:00', '2020-09-30 21:59:59', 1, 99999, 0, 15.00, 'EUR');
