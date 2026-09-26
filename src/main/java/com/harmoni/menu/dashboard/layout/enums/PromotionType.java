package com.harmoni.menu.dashboard.layout.enums;

import lombok.Getter;

/**
 * The mechanism a promotion applies, mirroring the menu service
 * {@code PromotionType} wire values.
 */
@Getter
public enum PromotionType {
    /** A percentage off the line total. */
    PERCENTAGE("Percentage", "percentage"),
    /** A fixed currency amount off the line total. */
    FIXED_AMOUNT("Fixed amount", "fixedAmount"),
    /** A replacement price for a specific SKU. */
    SPECIAL_PRICE("Special price", "specialPrice"),
    /** Buy a quantity and get another quantity free. */
    BUY_X_GET_Y("Buy X get Y", "buyXGetY"),
    /** A fixed set of items sold together. */
    BUNDLE("Bundle", "bundle");

    private final String label;
    private final String messageKey;

    PromotionType(String label, String messageKey) {
        this.label = label;
        this.messageKey = messageKey;
    }

}
