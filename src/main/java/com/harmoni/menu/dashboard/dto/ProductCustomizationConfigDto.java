package com.harmoni.menu.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-product override configuration for a single attached customization.
 *
 * <p>Update payload for
 * {@code PUT /api/v1/product/{productId}/customization/{linkId}}; any
 * {@code null} override restores the master default from the customization
 * definition.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCustomizationConfigDto {

    /** Overrides the master {@code required} flag, or {@code null} to inherit. */

    private Boolean requiredOverride;

    /** Overrides the master minimum selection, or {@code null} to inherit. */

    private Integer minSelectionOverride;

    /** Overrides the master maximum selection, or {@code null} to inherit. */

    private Integer maxSelectionOverride;

    /** Sort position among the product's linked customizations. */

    private Integer sortOrder;

}