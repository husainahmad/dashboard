package com.harmoni.menu.dashboard.layout.menu.product.binder;

import lombok.Builder;
import lombok.Data;


/**
 * Transient holder for the SKU tier-price editing grid.
 *
 * <p>Matches one row of the {@code Product Binder} UI: the SKU being priced,
 * the price tier and the price entered for that tier. Not persisted directly;
 * it is converted into {@code SkuTierPriceDto} on save.
 */
@Data
@Builder
public class ProductBinderBean {
    /** The product id this price belongs to. */
    private Integer id;
    /** The SKU being priced. */
    private Integer skuId;
    /** The SKU display name. */
    private String skuName;

    /** The price tier the price applies to. */
    private Integer tierId;
    /** The price for the tier. */
    private Double price;
}
