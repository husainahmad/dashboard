package com.harmoni.menu.dashboard.layout.enums;

import lombok.Getter;

/**
 * A constraint attached to a promotion, mirroring the menu service
 * {@code PromotionRuleType} wire values. A promotion may carry several rules and
 * must satisfy all of them.
 */
@Getter
public enum PromotionRuleType {
    /** Requires a percentage based discount rule. */
    PERCENTAGE("Percentage", "percentage"),
    /** Requires a fixed amount based discount rule. */
    FIXED_AMOUNT("Fixed amount", "fixedAmount"),
    /** Caps the granted discount. */
    MAX_DISCOUNT_AMOUNT("Maximum discount", "maxDiscountAmount"),
    /** Requires a minimum basket quantity. */
    MIN_QUANTITY("Minimum quantity", "minQuantity"),
    /** Requires a minimum basket amount. */
    MIN_AMOUNT("Minimum amount", "minAmount");

    private final String label;
    private final String messageKey;

    PromotionRuleType(String label, String messageKey) {
        this.label = label;
        this.messageKey = messageKey;
    }

}
