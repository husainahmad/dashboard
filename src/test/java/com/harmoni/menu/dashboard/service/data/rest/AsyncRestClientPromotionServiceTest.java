package com.harmoni.menu.dashboard.service.data.rest;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.harmoni.menu.dashboard.configuration.MenuProperties;
import com.harmoni.menu.dashboard.configuration.PromotionProperties;
import com.harmoni.menu.dashboard.configuration.UrlProperties;
import com.harmoni.menu.dashboard.dto.PromotionDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the promotion client against the contract exposed by the menu service:
 * the paged query string it builds, and the JSON the {@link PromotionDto} graph
 * has to accept coming back.
 */
class AsyncRestClientPromotionServiceTest {

    private static final String BASE_URL = "http://localhost:8080/api/v1/promotion";

    private AsyncRestClientPromotionService client;

    @BeforeEach
    void setUp() {
        PromotionProperties promotions = new PromotionProperties();
        promotions.setPromotion(BASE_URL);
        promotions.setQuery(BASE_URL + "?page=%d&size=%d&status=%s&promotionType=%s&search=%s");
        promotions.setRedeemable(BASE_URL + "/redeemable");
        promotions.setByCode(BASE_URL + "/code/%s");
        promotions.setStatus(BASE_URL + "/%d/status");
        promotions.setOrderItemDiscounts(
                "http://localhost:8080/api/v1/order-item-discount/order-item/%d");

        UrlProperties url = new UrlProperties();
        url.setPromotions(promotions);
        MenuProperties properties = new MenuProperties();
        properties.setUrl(url);
        client = new AsyncRestClientPromotionService(properties, null);
    }

    @Test
    void buildPromotionUri_keepsPagingAndEveryFilter() {
        String uri = client.buildPromotionUri(2, 15, "ACTIVE", "PERCENTAGE", "happy");

        assertTrue(uri.startsWith(BASE_URL), uri);
        assertTrue(uri.contains("page=2"), uri);
        assertTrue(uri.contains("size=15"), uri);
        assertTrue(uri.contains("status=ACTIVE"), uri);
        assertTrue(uri.contains("promotionType=PERCENTAGE"), uri);
        assertTrue(uri.contains("search=happy"), uri);
    }

    @Test
    void buildPromotionUri_sendsUnsetFiltersBlankSoTheyStayUnfiltered() {
        String uri = client.buildPromotionUri(1, 15, null, null, null);

        assertTrue(uri.contains("status=&"), uri);
        assertTrue(uri.contains("promotionType=&"), uri);
        assertTrue(uri.endsWith("search="), uri);
    }

    @Test
    void buildPromotionUri_treatsBlankSearchAsUnfiltered() {
        assertEquals(client.buildPromotionUri(1, 15, null, null, null),
                client.buildPromotionUri(1, 15, null, null, ""));
    }

    @Test
    void promotionListRow_deserializesListPageEnvelope() throws Exception {
        String json = """
                {
                  "data": [
                    {
                      "id": 1,
                      "code": "HAPPY10",
                      "name": "Happy Hour",
                      "promotionType": "PERCENTAGE",
                      "status": "ACTIVE",
                      "priority": 5
                    }
                  ],
                  "page": 1,
                  "size": 15,
                  "total": 1,
                  "navigate": {}
                }
                """;

        @SuppressWarnings("unchecked")
        java.util.Map<String, Object> page = objectMapper().readValue(json, java.util.Map.class);

        assertEquals(1, page.get("total"));
        assertEquals("1", String.valueOf(page.get("page")));

        var rows = (java.util.List<java.util.Map<String, Object>>) page.get("data");
        PromotionDto promotion = objectMapper().convertValue(rows.getFirst(), PromotionDto.class);

        assertEquals(1L, promotion.getId());
        assertEquals("HAPPY10", promotion.getCode());
        assertEquals("Happy Hour", promotion.getName());
        assertEquals(com.harmoni.menu.dashboard.layout.enums.PromotionType.PERCENTAGE,
                promotion.getPromotionType());
        assertEquals(com.harmoni.menu.dashboard.layout.enums.PromotionStatus.ACTIVE,
                promotion.getStatus());
        assertEquals(5, promotion.getPriority());
    }

    @Test
    void promotionDetail_deserializesTheWholeAggregate() throws Exception {
        String json = """
                {
                  "id": 7,
                  "code": "LUNCH",
                  "name": "Lunch Special",
                  "description": "Weekday only",
                  "promotionType": "SPECIAL_PRICE",
                  "status": "ACTIVE",
                  "priority": 0,
                  "startDate": "2026-01-01",
                  "endDate": "2026-12-31",
                  "schedules": [
                    {
                      "id": 1,
                      "dayOfWeek": "MONDAY",
                      "startTime": "11:00:00",
                      "endTime": "14:00:00",
                      "enabled": true
                    },
                    {
                      "id": 2,
                      "dayOfWeek": "SUNDAY",
                      "startTime": "15:00",
                      "endTime": "17:00",
                      "enabled": false
                    }
                  ],
                  "targets": [
                    { "id": 3, "targetType": "CATEGORY", "categoryId": 13 },
                    { "id": 4, "targetType": "SKU", "skuId": 42 },
                    { "id": 5, "targetType": "PRODUCT", "productId": 8 }
                  ],
                  "rules": [
                    { "id": 6, "ruleType": "PERCENTAGE", "discountValue": 10.00 },
                    { "id": 7, "ruleType": "MAX_DISCOUNT_AMOUNT", "maxDiscountAmount": 50000.00 },
                    { "id": 8, "ruleType": "MIN_QUANTITY", "minQuantity": 2.00 },
                    { "id": 9, "ruleType": "MIN_AMOUNT", "minAmount": 75000.00 }
                  ],
                  "specialPrices": [
                    { "id": 10, "skuId": 42, "specialPrice": 25000.00 }
                  ]
                }
                """;

        PromotionDto promotion = objectMapper().readValue(json, PromotionDto.class);

        assertEquals(7L, promotion.getId());
        assertEquals("LUNCH", promotion.getCode());
        assertEquals(java.time.LocalDate.of(2026, 1, 1), promotion.getStartDate());
        assertEquals(java.time.LocalDate.of(2026, 12, 31), promotion.getEndDate());

        assertEquals(2, promotion.getSchedules().size());
        assertEquals(java.time.DayOfWeek.MONDAY, promotion.getSchedules().getFirst().getDayOfWeek());
        assertEquals(java.time.LocalTime.of(11, 0), promotion.getSchedules().getFirst().getStartTime());
        assertEquals(java.time.LocalTime.of(14, 0), promotion.getSchedules().getFirst().getEndTime());
        assertTrue(promotion.getSchedules().getFirst().getEnabled());
        assertEquals(false, promotion.getSchedules().get(1).getEnabled());

        assertEquals(3, promotion.getTargets().size());
        assertEquals(13L, promotion.getTargets().get(0).getCategoryId());
        assertEquals(42L, promotion.getTargets().get(1).getSkuId());
        assertEquals(8L, promotion.getTargets().get(2).getProductId());

        assertEquals(4, promotion.getRules().size());
        assertEquals(new java.math.BigDecimal("10.00"),
                promotion.getRules().get(0).getDiscountValue());
        assertEquals(new java.math.BigDecimal("50000.00"),
                promotion.getRules().get(1).getMaxDiscountAmount());
        assertEquals(new java.math.BigDecimal("2.00"),
                promotion.getRules().get(2).getMinQuantity());
        assertEquals(new java.math.BigDecimal("75000.00"),
                promotion.getRules().get(3).getMinAmount());

        assertEquals(1, promotion.getSpecialPrices().size());
        assertEquals(42L, promotion.getSpecialPrices().getFirst().getSkuId());
        assertEquals(new java.math.BigDecimal("25000.00"),
                promotion.getSpecialPrices().getFirst().getSpecialPrice());
    }

    @Test
    void promotionAggregate_roundTripsThroughTheWireFormat() throws Exception {
        PromotionDto promotion = PromotionDto.builder()
                .code("WEEKEND")
                .name("Weekend")
                .promotionType(com.harmoni.menu.dashboard.layout.enums.PromotionType.FIXED_AMOUNT)
                .status(com.harmoni.menu.dashboard.layout.enums.PromotionStatus.DRAFT)
                .priority(2)
                .schedules(java.util.List.of(com.harmoni.menu.dashboard.dto.PromotionScheduleDto.builder()
                        .dayOfWeek(java.time.DayOfWeek.SATURDAY)
                        .startTime(java.time.LocalTime.of(20, 0))
                        .endTime(java.time.LocalTime.of(23, 0))
                        .enabled(true)
                        .build()))
                .targets(java.util.List.of(com.harmoni.menu.dashboard.dto.PromotionTargetDto.builder()
                        .targetType(com.harmoni.menu.dashboard.layout.enums.PromotionTargetType.CATEGORY)
                        .categoryId(4L)
                        .build()))
                .rules(java.util.List.of(com.harmoni.menu.dashboard.dto.PromotionRuleDto.builder()
                        .ruleType(com.harmoni.menu.dashboard.layout.enums.PromotionRuleType.FIXED_AMOUNT)
                        .discountValue(new java.math.BigDecimal("15000"))
                        .build()))
                .build();

        String json = objectMapper().writeValueAsString(promotion);
        PromotionDto parsed = objectMapper().readValue(json, PromotionDto.class);

        assertEquals(promotion.getCode(), parsed.getCode());
        assertEquals(promotion.getPromotionType(), parsed.getPromotionType());
        assertEquals(promotion.getStatus(), parsed.getStatus());
        assertEquals(1, parsed.getSchedules().size());
        assertNotNull(parsed.getSchedules().getFirst().getStartTime());
        assertEquals(4L, parsed.getTargets().getFirst().getCategoryId());
        assertEquals(new java.math.BigDecimal("15000"),
                parsed.getRules().getFirst().getDiscountValue());
    }

    @Test
    void promotionAggregate_sendsNullChildrenAsNullSoTheServerLeavesThemUntouched() throws Exception {
        PromotionDto promotion = PromotionDto.builder()
                .id(7L)
                .code("LUNCH")
                .name("Lunch")
                .promotionType(com.harmoni.menu.dashboard.layout.enums.PromotionType.PERCENTAGE)
                .status(com.harmoni.menu.dashboard.layout.enums.PromotionStatus.ACTIVE)
                .priority(0)
                .build();

        String json = objectMapper().writeValueAsString(promotion);

        // An explicit null is how the form says "I did not touch this collection";
        // the menu service treats it exactly like an absent property, whereas an
        // empty array is what clears the stored rows.
        assertTrue(json.contains("\"schedules\":null"), json);
        assertTrue(json.contains("\"targets\":null"), json);
        assertTrue(json.contains("\"rules\":null"), json);
        assertTrue(json.contains("\"specialPrices\":null"), json);
    }

    @Test
    void promotionAggregate_sendsEmptyChildrenAsEmptyArraySoTheServerClearsThem() throws Exception {
        PromotionDto promotion = PromotionDto.builder()
                .id(7L)
                .code("LUNCH")
                .name("Lunch")
                .promotionType(com.harmoni.menu.dashboard.layout.enums.PromotionType.PERCENTAGE)
                .status(com.harmoni.menu.dashboard.layout.enums.PromotionStatus.ACTIVE)
                .priority(0)
                .schedules(java.util.List.of())
                .build();

        String json = objectMapper().writeValueAsString(promotion);

        assertTrue(json.contains("\"schedules\":[]"), json);
        assertTrue(json.contains("\"targets\":null"), json);
    }

    /**
     * Mirrors the lenient mapper the reactive client relies on, so the test fails
     * if the wire format ever drifts from the DTOs.
     */
    private static ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
