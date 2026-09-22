package com.harmoni.menu.dashboard.layout.model;

import java.util.List;

/**
 * Aggregated result of scanning the brand's price matrix: every SKU missing a
 * price in at least one tier, every priced SKU's cheapest tier, and the total
 * SKU count scanned.
 */
public record MatrixStats(List<UnpricedSku> unpriced, List<LowPriceRow> lowest, int skuCount) {
}