package com.harmoni.menu.dashboard.dto;

import com.harmoni.menu.dashboard.layout.enums.PromotionTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * One product, SKU or category a promotion applies to. Exactly one of the three
 * reference ids is meaningful, selected by {@link #targetType}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionTargetDto {

    /** The database id, or {@code null} for a row that has not been saved yet. */

    private Long id;

    /** What this target points at. */

    private PromotionTargetType targetType;

    /** Set when {@link #targetType} is {@code PRODUCT}. */

    private Long productId;

    /** Set when {@link #targetType} is {@code SKU}. */

    private Long skuId;

    /** Set when {@link #targetType} is {@code CATEGORY}. */

    private Long categoryId;

}
