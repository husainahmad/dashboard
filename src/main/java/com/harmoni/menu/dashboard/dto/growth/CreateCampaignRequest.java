package com.harmoni.menu.dashboard.dto.growth;

import java.time.Instant;
import java.util.List;

public record CreateCampaignRequest(String code, String name, String type,
                                    Instant startAt, Instant endAt, List<CampaignRuleDto> rules) {

    public record CampaignRuleDto(String ruleType, String ruleValue) {
    }
}
