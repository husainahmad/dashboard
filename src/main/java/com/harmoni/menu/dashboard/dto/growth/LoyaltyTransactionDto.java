package com.harmoni.menu.dashboard.dto.growth;

import java.time.Instant;

public record LoyaltyTransactionDto(Long id, String type, int points, Integer orderId, Instant createdAt) {
}
