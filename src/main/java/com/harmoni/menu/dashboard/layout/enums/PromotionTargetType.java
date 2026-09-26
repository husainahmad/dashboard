package com.harmoni.menu.dashboard.layout.enums;

import lombok.Getter;

/**
 * What a promotion applies to, mirroring the menu service
 * {@code PromotionTargetType} wire values. Exactly one of the three reference ids
 * is set on a target, chosen by this type.
 */
@Getter
public enum PromotionTargetType {
    /** Applies to every SKU of one product. */
    PRODUCT("Product", "product"),
    /** Applies to a single SKU. */
    SKU("SKU", "sku"),
    /** Applies to every product in one category. */
    CATEGORY("Category", "category");

    private final String label;
    private final String messageKey;

    PromotionTargetType(String label, String messageKey) {
        this.label = label;
        this.messageKey = messageKey;
    }

}
