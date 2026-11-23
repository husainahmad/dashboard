package com.harmoni.menu.dashboard.dto.growth;

import java.math.BigDecimal;
import java.time.Instant;

public record InsightDto(Long id, Long customerId, String type, String title, String body,
                         BigDecimal confidence, Instant generatedAt) {
}
