package com.harmoni.menu.dashboard.dto;

import com.harmoni.menu.dashboard.layout.enums.PromotionRuleType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * A constraint a basket must satisfy for a promotion to apply. A promotion may
 * carry several rules and all of them must hold.
 * <p>
 * Only the field named by {@link #ruleType} is meaningful; the others stay
 * {@code null}. {@code PERCENTAGE} and {@code FIXED_AMOUNT} use
 * {@link #discountValue}, {@code MAX_DISCOUNT_AMOUNT} uses
 * {@link #maxDiscountAmount}, {@code MIN_QUANTITY} uses {@link #minQuantity} and
 * {@code MIN_AMOUNT} uses {@link #minAmount}.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionRuleDto {

    /** Which constraint this row expresses. */

    private PromotionRuleType ruleType;

    /** Threshold for a {@code PERCENTAGE} or {@code FIXED_AMOUNT} rule. */

    private BigDecimal discountValue;

    /** Ceiling for a {@code MAX_DISCOUNT_AMOUNT} rule. */

    private BigDecimal maxDiscountAmount;

    /** Threshold for a {@code MIN_QUANTITY} rule. */

    private BigDecimal minQuantity;

    /** Threshold for a {@code MIN_AMOUNT} rule. */

    private BigDecimal minAmount;

}
