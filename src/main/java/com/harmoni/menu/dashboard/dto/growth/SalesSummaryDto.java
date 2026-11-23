package com.harmoni.menu.dashboard.dto.growth;

import java.math.BigDecimal;

public record SalesSummaryDto(long orderCount, BigDecimal totalRevenue,
                              BigDecimal avgOrderValue) {
}
