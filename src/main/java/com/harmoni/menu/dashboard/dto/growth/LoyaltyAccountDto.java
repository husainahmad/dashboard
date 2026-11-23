package com.harmoni.menu.dashboard.dto.growth;

public record LoyaltyAccountDto(Long id, Long customerId, int pointsBalance,
                                int lifetimePoints, String tier) {
}
