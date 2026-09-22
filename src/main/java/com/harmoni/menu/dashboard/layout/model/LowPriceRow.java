package com.harmoni.menu.dashboard.layout.model;

/**
 * A SKU's cheapest tier price, shown on the dashboard's "Lowest priced SKUs"
 * watchlist.
 */
public record LowPriceRow(String product, String sku, Double price, String tier) {
}