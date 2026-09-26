package com.harmoni.menu.dashboard.dto.report;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * Per-product sales totals, including the category the product belongs to.
 *
 * <p>Mirrors the order service's {@code ProductSalesReport} model.</p>
 */
@Data
public class ProductSalesReportDto implements Serializable {

    private Integer categoryId;

    private String categoryName;

    private Integer productId;

    private String productName;

    private Integer quantity;

    private BigDecimal grossSales;

    private BigDecimal discount;

    private BigDecimal netSales;
}
