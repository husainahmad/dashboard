package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.dto.SkuDto;
import com.harmoni.menu.dashboard.layout.enums.ProductItemType;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * A node of the product tree grid.
 *
 * <p>Each node represents either a product or one of its SKUs. Product nodes
 * carry {@link #productId}, {@link #categoryId}, the name and the full SKU
 * list; SKU nodes carry {@link #skuId} and the per-tier prices in
 * {@link #tierPrices}. The tree data is used directly to seed the
 * {@link ProductForm} editor.
 */
@Data
@Builder
public class ProductTreeItem {

    /** Stable client id used by the tree grid to expand and render this node. */
    private String id;
    /** Display name shown in the tree. */
    private String name;
    /** Whether this node is a product or a SKU row. */
    private ProductItemType productItemType;
    /** The product id, {@code null} for SKU rows. */
    private Integer productId;
    /** The SKU id, {@code null} for product nodes. */
    private Integer skuId;
    /** The id of the category the product belongs to. */
    private Integer categoryId;
    /** The category display name. */
    private String categoryName;
    /** Selling price carried by the node. */
    private Double price;
    /** Display name of the price tier. */
    private String tierName;
    /** The product's SKUs, used to seed the edit form. */
    private List<SkuDto> skus;
    /** Per-tier selling prices keyed by tier id, populated on SKU rows. */
    private Map<Integer, Double> tierPrices;
}
