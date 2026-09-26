package com.harmoni.menu.dashboard.dto.report;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * One product's sales within a single day.
 *
 * <p>Mirrors the order service's {@code DailyProductReport} model, nested inside
 * a {@link DailyReportDto}.</p>
 */
@Data
public class DailyProductReportDto implements Serializable {

    private Integer productId;

    private String productName;

    private Integer quantity;

    private BigDecimal netSales;

    private BigDecimal discount;
}
