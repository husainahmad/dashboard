package com.harmoni.menu.dashboard.layout.model;

/**
 * Per-SKU scan result: the tiers the SKU has no price in (absent when priced in
 * every tier) and the row for its cheapest tier price (absent when unpriced in
 * every tier).
 */
public record SkuScan(UnpricedSku unpriced, LowPriceRow lowest) {
}