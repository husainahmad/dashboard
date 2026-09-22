package com.harmoni.menu.dashboard.layout.model;

import java.util.List;

/**
 * A SKU reported as missing a price in at least one of the brand's price tiers.
 */
public record UnpricedSku(String product, String sku, List<String> missingTiers) {
}