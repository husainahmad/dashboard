package com.harmoni.menu.dashboard.dto.growth;

import java.time.Instant;

public record VoucherDto(Long id, String code, Long campaignId, Long customerId,
                         String status, Instant expiresAt, Instant redeemedAt) {
}
