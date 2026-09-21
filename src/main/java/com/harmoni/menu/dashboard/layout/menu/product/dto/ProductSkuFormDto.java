package com.harmoni.menu.dashboard.layout.menu.product.dto;

import lombok.Builder;
import lombok.Data;

/**
 * A single SKU row submitted from the product edit form.
 *
 * <p>Carries the SKU identity and name together with the tier price assigned
 * to it, matching one editable row of the SKU &amp; pricing section.
 */
@Builder
@Data
public class ProductSkuFormDto {
    /** The SKU id, or {@code null} for a newly added row. */
    private Integer id;
    /** The SKU name. */
    private String name;
    /** The tier price linked to this SKU. */
    private ProductSkuTierPriceFormDto tierPrice;
}
