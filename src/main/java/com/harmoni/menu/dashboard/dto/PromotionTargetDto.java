package com.harmoni.menu.dashboard.dto;

import com.harmoni.menu.dashboard.layout.enums.PromotionScopeType;
import com.harmoni.menu.dashboard.layout.enums.PromotionTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * One product, SKU or category a promotion applies to. Exactly one of the three
 * reference ids is meaningful, selected by {@link #targetType}.
 * <p>
 * The scope determines the organizational level the promotion applies to:
 * <ul>
 *   <li>{@code All Stores} - Promotion applies to every store</li>
 *   <li>{@code Brand} - Promotion applies to a specific brand</li>
 *   <li>{@code Chain} - Promotion applies to a specific chain of stores</li>
 *   <li>{@code Store} - Promotion applies to specific stores (selected as a list)</li>
 * </ul>
 * <p>
 * Only one of the reference ids ({@code productId}, {@code skuId}, {@code categoryId})
 * should be set depending on the {@link #targetType}. The scope-related fields
 * ({@code scope}, {@code brandId}, {@code chainId}, {@code storeIds}) should
 * be set according to the chosen scope.
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

    /** The scope the promotion applies to. */

    private PromotionScopeType scope;

    /** Set when {@link #scope} is {@code BRAND}. */

    private Long brandId;

    /** Set when {@link #scope} is {@code CHAIN}. */

    private Long chainId;

    /** Set when {@link #scope} is {@code STORE}. Stores selected as a list. */

    private List<Long> storeIds;

}
