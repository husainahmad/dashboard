package com.harmoni.menu.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Replaces the whole set of customizations attached to a product.
 *
 * <p>Payload for {@code PUT /api/v1/product/{productId}/customization}; the
 * given customization ids fully replace the previous attachment list.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductCustomizationReplaceDto {

    /** The customization ids to attach to the product, replacing the existing set. */

    private List<Integer> customizationIds;

}