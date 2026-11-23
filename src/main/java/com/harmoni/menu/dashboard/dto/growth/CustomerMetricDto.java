package com.harmoni.menu.dashboard.dto.growth;

import java.math.BigDecimal;
import java.time.Instant;

public record CustomerMetricDto(long customerId, BigDecimal totalSpend, int visitCount,
                                BigDecimal avgOrderValue, Instant lastVisitAt,
                                Integer favoriteCategoryId, String segment) {
}
