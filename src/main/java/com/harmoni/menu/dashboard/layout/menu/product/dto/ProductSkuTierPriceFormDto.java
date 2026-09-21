package com.harmoni.menu.dashboard.layout.menu.product.dto;

import lombok.Builder;
import lombok.Data;

/**
 * The tier price attached to a SKU row in the product edit form.
 *
 * <p>Links a price tier to the price entered for it; the {@code id} identifies
 * the tier, not the product SKU.
 */
@Builder
@Data
public class ProductSkuTierPriceFormDto {
    /** The price tier id. */
    private Integer id;
    /** The price for the tier. */
    private Double price;
}
