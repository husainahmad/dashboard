package com.harmoni.menu.dashboard.layout.menu.product;

import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

/**
 * Editable draft of one SKU row on the product form. The grid renders from this
 * model directly (no side-channel name/description maps) and the tier prices are
 * kept in {@code tierPrices} keyed by tier id.
 */
@Data
@Builder
public class SkuTreeItem {
    /** Persisted SKU id, or {@code null} for rows that are not saved yet. */
    private Integer skuId;
    /** SKU display name, edited in place via the grid field. */
    private String skuName;
    /** SKU description, edited in place via the grid field. */
    private String skuDesc;
    /** Price per tier, keyed by tier id. */
    @Builder.Default
    private Map<Integer, Double> tierPrices = new HashMap<>();
}