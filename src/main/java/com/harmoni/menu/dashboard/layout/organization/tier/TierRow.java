package com.harmoni.menu.dashboard.layout.organization.tier;

import com.harmoni.menu.dashboard.dto.TierDto;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * One row of the tier list: the tier and the number of stores using it.
 */
@Getter
@AllArgsConstructor
public class TierRow {

    private final TierDto tier;
    private final int storeCount;
}
