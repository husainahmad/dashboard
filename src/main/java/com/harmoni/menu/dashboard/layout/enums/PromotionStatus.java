package com.harmoni.menu.dashboard.layout.enums;

import lombok.Getter;

/**
 * The lifecycle state of a promotion, mirroring the menu service
 * {@code PromotionStatus} wire values.
 */
@Getter
public enum PromotionStatus {
    /** Being configured; never redeemable. */
    DRAFT("Draft", "draft"),
    /** Active but outside its scheduled time windows. */
    SCHEDULED("Scheduled", "scheduled"),
    /** Live and redeemable within its date range. */
    ACTIVE("Active", "active"),
    /** Temporarily switched off by an operator. */
    PAUSED("Paused", "paused"),
    /** Past its end date. */
    EXPIRED("Expired", "expired"),
    /** Ended early and can no longer be reactivated. */
    CANCELLED("Cancelled", "cancelled");

    private final String label;
    private final String messageKey;

    PromotionStatus(String label, String messageKey) {
        this.label = label;
        this.messageKey = messageKey;
    }

}
