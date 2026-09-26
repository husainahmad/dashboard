package com.harmoni.menu.dashboard.dto.report;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Daily settlement totals reported by the order service.
 *
 * <p>
 * Mirrors the order service's {@code SettlementReport} model. Note that the
 * order service currently does not scope this report by store, so its totals
 * cover every store rather than just the signed-in one.
 * </p>
 */
@Data
public class SettlementReportDto implements Serializable {

    private String reportDate;

    private int totalOrders;

    private BigDecimal totalSales;

    private BigDecimal totalDiscounts;

    private BigDecimal totalTax;

    private BigDecimal totalNetSales;

    /** Payment method totals, e.g. {@code {"CASH": 500, "CARD": 350}}. */
    private Map<String, BigDecimal> paymentBreakdown;

    private BigDecimal totalRefunds;
}
