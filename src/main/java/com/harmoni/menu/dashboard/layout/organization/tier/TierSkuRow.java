package com.harmoni.menu.dashboard.layout.organization.tier;

import lombok.Getter;
import lombok.Setter;

/**
 * One editable row of the Price panel: a SKU and its price under the selected
 * tier.
 */
@Getter
public class TierSkuRow {

    private final Integer skuId;
    private final String name;

    @Setter
    private Double tierPrice;

    private final Double originalTierPrice;

    public TierSkuRow(Integer skuId, String name, Double tierPrice) {
        this.skuId = skuId;
        this.name = name;
        this.tierPrice = tierPrice;
        this.originalTierPrice = tierPrice;
    }

    /** Whether the tier price was changed since the row was loaded. */
    public boolean isChanged() {
        return !java.util.Objects.equals(tierPrice, originalTierPrice);
    }
}
