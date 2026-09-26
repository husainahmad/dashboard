package com.harmoni.menu.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * The replacement price granted to one SKU when a {@code SPECIAL_PRICE}
 * promotion applies.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionSpecialPriceDto {

    /** The database id, or {@code null} for a row that has not been saved yet. */

    private Long id;

    /** The SKU this price replaces the normal price of. */

    private Long skuId;

    /** The promotional price, which must not exceed the normal SKU price. */

    private BigDecimal specialPrice;

}
