package com.harmoni.menu.dashboard.layout.report;

import com.harmoni.menu.dashboard.dto.report.DailyProductReportDto;
import com.harmoni.menu.dashboard.dto.report.DailyReportDto;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers the two pieces of report logic that are easy to get subtly wrong: the
 * widening of a selected day range into the timestamps the order service
 * expects, and the flattening of the nested daily report into grid rows.
 */
class DateRangeAndDailyRowsTest {

    @Test
    void range_isValidWhenStartIsOnOrBeforeEnd() {
        assertTrue(new DateRange(LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 23)).isValid());
        assertTrue(new DateRange(LocalDate.of(2026, 8, 23), LocalDate.of(2026, 8, 23)).isValid());
    }

    @Test
    void range_isInvalidWhenEitherDayIsMissing() {
        assertFalse(new DateRange(null, LocalDate.of(2026, 8, 23)).isValid());
        assertFalse(new DateRange(LocalDate.of(2026, 8, 17), null).isValid());
    }

    @Test
    void range_isInvalidWhenStartFallsAfterEnd() {
        assertFalse(new DateRange(LocalDate.of(2026, 8, 24), LocalDate.of(2026, 8, 23)).isValid());
    }

    @Test
    void range_widensToTheStartOfTheFirstDay() {
        DateRange range = new DateRange(LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 23));

        assertEquals(LocalDateTime.of(2026, 8, 17, 0, 0, 0), range.startDateTime());
    }

    @Test
    void range_widensToTheLastSecondOfTheFinalDay() {
        DateRange range = new DateRange(LocalDate.of(2026, 8, 17), LocalDate.of(2026, 8, 23));

        assertEquals(LocalDateTime.of(2026, 8, 23, 23, 59, 59), range.endDateTime());
    }

    @Test
    void range_endCarriesNoNanoseconds() {
        DateRange range = new DateRange(LocalDate.of(2026, 8, 23), LocalDate.of(2026, 8, 23));

        assertEquals(0, range.endDateTime().getNano());
    }

    @Test
    void dailyRows_flattensOneRowPerDayAndProduct() {
        List<DailyReportView.DailyRow> rows =
                DailyReportView.flatten(List.of(day("2026-08-22", product("Nasi Goreng", 2, "30000.00")),
                        day("2026-08-23", product("Es Teh", 5, "25000.00"),
                                product("Kopi", 3, "45000.00"))));

        assertEquals(3, rows.size());
    }

    @Test
    void dailyRows_sortsNewestDayFirst() {
        List<DailyReportView.DailyRow> rows =
                DailyReportView.flatten(List.of(day("2026-08-22", product("Nasi Goreng", 2, "30000.00")),
                        day("2026-08-23", product("Es Teh", 5, "25000.00"))));

        assertEquals("2026-08-23", rows.getFirst().date());
        assertEquals("2026-08-22", rows.get(1).date());
    }

    @Test
    void dailyRows_keepsProductsOfTheSameDayInOrder() {
        List<DailyReportView.DailyRow> rows =
                DailyReportView.flatten(List.of(day("2026-08-23", product("Es Teh", 5, "25000.00"),
                        product("Kopi", 3, "45000.00"))));

        assertEquals(List.of("Es Teh", "Kopi"), rows.stream().map(DailyReportView.DailyRow::productName).toList());
    }

    @Test
    void dailyRows_skipsDaysWithoutProducts() {
        List<DailyReportView.DailyRow> rows = DailyReportView.flatten(
                Arrays.asList(day("2026-08-22", product("Nasi Goreng", 2, "30000.00")),
                        new DailyReportDto(), dayWithNoProductList()));

        assertEquals(1, rows.size());
        assertEquals("2026-08-22", rows.getFirst().date());
    }

    @Test
    void dailyRows_toleratesNullAndNullEntries() {
        List<DailyReportDto> reports = new ArrayList<>();
        reports.add(null);
        reports.add(day("2026-08-23", null, product("Kopi", 3, "45000.00")));

        List<DailyReportView.DailyRow> rows = DailyReportView.flatten(reports);

        assertEquals(1, rows.size());
        assertEquals("Kopi", rows.getFirst().productName());
    }

    @Test
    void dailyRows_returnsEmptyListForNullReport() {
        assertTrue(DailyReportView.flatten(null).isEmpty());
    }

    private static DailyReportDto day(String date, DailyProductReportDto... products) {
        DailyReportDto report = new DailyReportDto();
        report.setDate(date);
        report.setProducts(new ArrayList<>(Arrays.asList(products)));
        return report;
    }

    private static DailyReportDto dayWithNoProductList() {
        DailyReportDto report = new DailyReportDto();
        report.setDate("2026-08-21");
        report.setProducts(null);
        return report;
    }

    private static DailyProductReportDto product(String name, int quantity, String netSales) {
        DailyProductReportDto product = new DailyProductReportDto();
        product.setProductId(1);
        product.setProductName(name);
        product.setQuantity(quantity);
        product.setNetSales(new BigDecimal(netSales));
        product.setDiscount(BigDecimal.ZERO);
        return product;
    }
}
