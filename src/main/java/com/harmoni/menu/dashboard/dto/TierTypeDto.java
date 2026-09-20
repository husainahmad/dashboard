package com.harmoni.menu.dashboard.dto;

/**
 * Classifies the kind of tier a {@link TierDto} represents.
 *
 * <p>Controls how a tier is applied: pricing SKUs and options, surfacing menu
 * categories, or enabling sub-services.
 */

public enum TierTypeDto {

    /** Price tier: drives {@link SkuTierPriceDto} and {@link CustomizationOptionTierPriceDto}. */

    PRICE,

    /** Menu tier: decides which categories are visible through the menu. */

    MENU,

    /** Service tier: decides which sub-services are active. */

    SERVICE
}
