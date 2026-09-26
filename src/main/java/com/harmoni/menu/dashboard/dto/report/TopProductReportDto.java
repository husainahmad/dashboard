package com.harmoni.menu.dashboard.dto.report;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * A single product's contribution to the top-products ranking.
 *
 * <p>Mirrors the order service's {@code TopProductReport} model.</p>
 */
@Data
public class TopProductReportDto implements Serializable {

    private Integer productId;

    private String productName;

    private BigDecimal totalSales;

    private Integer quantitySold;
}
