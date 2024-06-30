package com.harmoni.menu.dashboard.configuration;

import lombok.Data;

@Data
public class UrlProperties {
    private String store;
    private String category;
    private String sku;
    private String skutierprice;
    private String brand;
    private String chain;
    private String tier;
    private String product;
    private TierProperties tiers;
    private CategoryProperties categories;
    private ProductProperties products;
}
