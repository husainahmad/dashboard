package com.harmoni.menu.dashboard.layout.util;

import com.harmoni.menu.dashboard.dto.PromotionDto;
import com.harmoni.menu.dashboard.dto.order.RecentOrderDto;
import com.harmoni.menu.dashboard.dto.report.SettlementReportDto;
import com.harmoni.menu.dashboard.layout.enums.PromotionStatus;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class DashboardAttentionRules {

    private static final BigDecimal DISCOUNT_THRESHOLD = new BigDecimal("0.20");

    private static final int EXPIRING_DAYS = 7;

    private DashboardAttentionRules() {
    }

    public enum Type {
        REFUNDS, HIGH_DISCOUNT, UNPAID_ORDERS, EXPIRING_PROMOTIONS
    }

    public record Signal(Type type, int count, BigDecimal amount) {
    }

    public static List<Signal> evaluate(SettlementReportDto settlement,
                                        List<RecentOrderDto> orders,
                                        List<PromotionDto> promotions,
                                        LocalDate today) {
        List<Signal> signals = new ArrayList<>();
        if (settlement != null) {
            BigDecimal refunds = settlement.getTotalRefunds();
            if (refunds != null && refunds.signum() > 0) {
                signals.add(new Signal(Type.REFUNDS, 0, refunds));
            }
            addDiscountSignal(signals, settlement);
        }
        int unpaid = countUnpaidOrders(orders);
        if (unpaid > 0) {
            signals.add(new Signal(Type.UNPAID_ORDERS, unpaid, BigDecimal.ZERO));
        }
        int expiring = countExpiringPromotions(promotions, today);
        if (expiring > 0) {
            signals.add(new Signal(Type.EXPIRING_PROMOTIONS, expiring, BigDecimal.ZERO));
        }
        return signals;
    }

    private static void addDiscountSignal(List<Signal> signals, SettlementReportDto settlement) {
        BigDecimal sales = settlement.getTotalSales();
        BigDecimal discounts = settlement.getTotalDiscounts();
        if (sales == null || sales.signum() <= 0 || discounts == null || discounts.signum() <= 0) {
            return;
        }
        BigDecimal ratio = discounts.divide(sales, 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(DISCOUNT_THRESHOLD) >= 0) {
            int percent = ratio.multiply(BigDecimal.valueOf(100)).setScale(0, RoundingMode.HALF_UP).intValue();
            signals.add(new Signal(Type.HIGH_DISCOUNT, percent, discounts));
        }
    }

    private static int countUnpaidOrders(List<RecentOrderDto> orders) {
        if (orders == null) {
            return 0;
        }
        return (int) orders.stream()
                .filter(order -> order.getStatus() != null)
                .filter(order -> {
                    String status = order.getStatus().toUpperCase(Locale.ROOT);
                    return "PENDING".equals(status) || "CONFIRMED".equals(status);
                })
                .count();
    }

    private static int countExpiringPromotions(List<PromotionDto> promotions, LocalDate today) {
        if (promotions == null || today == null) {
            return 0;
        }
        LocalDate limit = today.plusDays(EXPIRING_DAYS);
        return (int) promotions.stream()
                .filter(promotion -> promotion.getStatus() == PromotionStatus.ACTIVE)
                .filter(promotion -> promotion.getEndDate() != null)
                .filter(promotion -> !promotion.getEndDate().isBefore(today)
                        && !promotion.getEndDate().isAfter(limit))
                .count();
    }
}
