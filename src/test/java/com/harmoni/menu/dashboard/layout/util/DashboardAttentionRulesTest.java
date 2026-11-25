package com.harmoni.menu.dashboard.layout.util;

import com.harmoni.menu.dashboard.dto.PromotionDto;
import com.harmoni.menu.dashboard.dto.order.RecentOrderDto;
import com.harmoni.menu.dashboard.dto.report.SettlementReportDto;
import com.harmoni.menu.dashboard.layout.enums.PromotionStatus;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DashboardAttentionRulesTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 23);

    @Test
    void evaluate_reportsRefundsAndHighDiscounts() {
        SettlementReportDto settlement = new SettlementReportDto();
        settlement.setTotalSales(new BigDecimal("1000000"));
        settlement.setTotalDiscounts(new BigDecimal("200000"));
        settlement.setTotalRefunds(new BigDecimal("50000"));

        List<DashboardAttentionRules.Signal> signals =
                DashboardAttentionRules.evaluate(settlement, List.of(), List.of(), TODAY);

        assertEquals(2, signals.size());
        assertTrue(signals.stream().anyMatch(signal ->
                signal.type() == DashboardAttentionRules.Type.REFUNDS
                        && signal.amount().compareTo(new BigDecimal("50000")) == 0));
        assertTrue(signals.stream().anyMatch(signal ->
                signal.type() == DashboardAttentionRules.Type.HIGH_DISCOUNT && signal.count() == 20));
    }

    @Test
    void evaluate_countsPendingAndConfirmedOrders() {
        List<DashboardAttentionRules.Signal> signals = DashboardAttentionRules.evaluate(
                null, List.of(order("PENDING"), order("CONFIRMED"), order("PAID"), order("VOID")),
                List.of(), TODAY);

        assertEquals(1, signals.size());
        assertEquals(DashboardAttentionRules.Type.UNPAID_ORDERS, signals.getFirst().type());
        assertEquals(2, signals.getFirst().count());
    }

    @Test
    void evaluate_reportsPromotionsExpiringWithinSevenDays() {
        List<DashboardAttentionRules.Signal> signals = DashboardAttentionRules.evaluate(
                null, List.of(), List.of(
                        promotion(TODAY.plusDays(1)),
                        promotion(TODAY.plusDays(7)),
                        promotion(TODAY.plusDays(8))), TODAY);

        assertEquals(1, signals.size());
        assertEquals(DashboardAttentionRules.Type.EXPIRING_PROMOTIONS, signals.getFirst().type());
        assertEquals(2, signals.getFirst().count());
    }

    @Test
    void evaluate_returnsNoSignalsWhenEverythingIsNormal() {
        SettlementReportDto settlement = new SettlementReportDto();
        settlement.setTotalSales(new BigDecimal("1000000"));
        settlement.setTotalDiscounts(new BigDecimal("100000"));
        settlement.setTotalRefunds(BigDecimal.ZERO);

        List<DashboardAttentionRules.Signal> signals =
                DashboardAttentionRules.evaluate(settlement, List.of(), List.of(), TODAY);

        assertTrue(signals.isEmpty());
    }

    private static RecentOrderDto order(String status) {
        RecentOrderDto order = new RecentOrderDto();
        order.setStatus(status);
        return order;
    }

    private static PromotionDto promotion(LocalDate endDate) {
        return PromotionDto.builder()
                .name("Promo")
                .status(PromotionStatus.ACTIVE)
                .endDate(endDate)
                .build();
    }
}
