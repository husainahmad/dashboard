package com.harmoni.menu.dashboard.dto.growth;

import java.time.Instant;

public record CampaignDto(Long id, String code, String name, String type, String status,
                          Instant startAt, Instant endAt) {
}
