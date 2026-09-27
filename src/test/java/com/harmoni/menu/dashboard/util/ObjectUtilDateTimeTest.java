package com.harmoni.menu.dashboard.util;

import com.harmoni.menu.dashboard.dto.PromotionDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the shared mapper in {@link ObjectUtil} against losing its
 * {@code JavaTimeModule} registration.
 *
 * <p>The promotion list deserialises every row with {@code convertValueToObject}, so a
 * mapper without the module throws on the first {@code LocalDate} it meets and takes
 * the whole list down with it. The reactive client in {@code AsyncRestClientBase} keeps
 * a second mapper of its own, and the existing promotion service test could not catch
 * this because it built its own mapper instead of exercising the shared one, so the
 * behaviour is asserted here against {@link ObjectUtil} directly.</p>
 */
class ObjectUtilDateTimeTest {

    @Test
    void convertValueToObject_readsAnIsoDateIntoALocalDate() {
        Map<String, Object> payload = Map.of(
                "code", "LUNCH",
                "name", "Lunch special",
                "startDate", "2026-09-27",
                "endDate", "2026-10-27");

        PromotionDto promotion = ObjectUtil.convertValueToObject(payload, PromotionDto.class);

        assertEquals(LocalDate.of(2026, 9, 27), promotion.getStartDate(),
                "the menu service sends ISO dates, so the shared mapper must bind them to LocalDate rather "
                        + "than refusing the whole payload");
        assertEquals(LocalDate.of(2026, 10, 27), promotion.getEndDate());
        assertEquals("Lunch special", promotion.getName());
    }

    @Test
    void convertValueToObject_readsAnIsoTimeIntoALocalTime() {
        Map<String, Object> payload = Map.of("startTime", "15:00", "endTime", "17:00");

        PromotionDto promotion = ObjectUtil.convertValueToObject(payload, PromotionDto.class);

        assertEquals(LocalTime.of(15, 0), promotion.getStartTime(),
                "a weekly schedule carries LocalTime values, which the shared mapper must also be able to "
                        + "bind");
    }

    @Test
    void objectToJsonString_writesDatesAsIsoTextRatherThanArrays() throws Exception {
        PromotionDto promotion = new PromotionDto();
        promotion.setStartDate(LocalDate.of(2026, 9, 27));
        promotion.setEndTime(LocalTime.of(17, 0));
        promotion.setDiscountValue(new BigDecimal("25"));

        String json = ObjectUtil.objectToJsonString(promotion);

        assertTrue(json.contains("\"startDate\":\"2026-09-27\""), json);
        assertTrue(json.contains("\"endTime\":\"17:00:00\""), json);
        assertFalse(json.contains("[2026,9,27]"),
                "Jackson writes java.time values as numeric arrays unless WRITE_DATES_AS_TIMESTAMPS is "
                        + "disabled, and the menu service expects ISO text");
    }

    @Test
    void convertValueToObject_ignoresPropertiesTheDtosDoNotModel() {
        Map<String, Object> payload = Map.of(
                "code", "LUNCH",
                "aFieldTheBackendAddedLastWeek", "ignored");

        PromotionDto promotion = ObjectUtil.convertValueToObject(payload, PromotionDto.class);

        assertEquals("LUNCH", promotion.getCode(),
                "the shared mapper is deliberately lenient so a new backend field cannot break a screen");
    }

    @Test
    void convertValueToObject_leavesOmittedChildCollectionsNull() {
        PromotionDto promotion = ObjectUtil.convertValueToObject(
                Map.of("code", "LUNCH"), PromotionDto.class);

        assertNull(promotion.getSchedules(),
                "the promotion form reads a null child list as \"leave these rows alone\" and an empty list "
                        + "as \"clear them\", so an omitted collection must stay null rather than becoming "
                        + "empty");
        assertNull(promotion.getTargets());
    }
}
