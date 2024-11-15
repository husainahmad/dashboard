package com.harmoni.menu.dashboard.configuration;

import lombok.Data;

import java.io.Serializable;

@Data
public class UrlProperties implements Serializable {
    private String store;
    private String category;
    private String sku;
    private String skutierprice;
    private String brand;
    private String chain;
    private String tier;
    private String product;
    private transient TierProperties tiers;
    private transient CategoryProperties categories;
    private transient ProductProperties products;
}
