package com.harmoni.menu.dashboard.layout.enums;

import lombok.Getter;

/**
 * The scope a promotion applies to, determining the organizational level.
 * <p>
 * A promotion can be configured to apply at different levels:
 * <ul>
 *   <li>{@code ALL_STORES} - Applies to all stores in the system</li>
 *   <li>{@code BRAND} - Applies to a specific brand (e.g., "Kopi Harmoni")</li>
 *   <li>{@code CHAIN} - Applies to a specific chain of stores (e.g., "Harmoni Coffee")</li>
 *   <li>{@code STORE} - Applies to specific stores selected by the operator</li>
 * </ul>
 * <p>
 * The scope is carried on the promotion and its targets, and decides which
 * organization fields the promotion form shows. See {@link #narrowsTo} for that rule.
 */
@Getter
public enum PromotionScopeType {
    /** Applies to all stores. */
    ALL_STORES("All Stores", "allStores"),
    /** Applies to a specific brand. */
    BRAND("Brand", "brand"),
    /** Applies to a specific chain. */
    CHAIN("Chain", "chain"),
    /** Applies to specific stores (multiple selection supported). */
    STORE("Store", "store");

    private final String label;
    private final String messageKey;

    PromotionScopeType(String label, String messageKey) {
        this.label = label;
        this.messageKey = messageKey;
    }

    /**
     * Whether this scope is narrowed by choosing the given organizational level.
     *
     * <p>Brand sits above chain and chain above store, so narrowing to a lower level
     * means naming every level above it as well: a store is only identified once the
     * chain it belongs to is known. {@code ALL_STORES} narrows by nothing, so it is
     * narrowed by no level at all.</p>
     *
     * <p>Keeping the rule here rather than in the form means a new scope cannot be added
     * without the form learning what it has to ask for.</p>
     *
     * @param level the organizational level to test
     * @return {@code true} when the operator has to choose this level for the scope
     */
    public boolean narrowsTo(PromotionScopeType level) {
        return switch (this) {
            case ALL_STORES -> false;
            case BRAND -> level == BRAND;
            case CHAIN -> level == BRAND || level == CHAIN;
            case STORE -> true;
        };
    }
}
