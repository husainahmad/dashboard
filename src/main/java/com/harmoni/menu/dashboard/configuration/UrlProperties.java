package com.harmoni.menu.dashboard.configuration;

import lombok.Data;

import java.io.Serializable;

/**
 * Holds the REST URLs of the menu resource endpoints.
 *
 * <p>
 * Binds the {@code menu.url.*} keys for the flat resources (store, category,
 * branding, tier, product, service, user, ...) and groups the more complex
 * paths into the nested {@link TierProperties}, {@link CategoryProperties},
 * {@link ProductProperties} and {@link UserProperties}.
 * </p>
 */

@Data
public class UrlProperties implements Serializable {
    private String store;
    private String category;
    private String customization;
    private String sku;
    private String skutierprice;
    private String brand;
    private String chain;
    private String tier;
    private String product;
    private String service;
    private String user;
    private transient TierProperties tiers;
    private transient CategoryProperties categories;
    private transient ProductProperties products;
    private transient UserProperties users;
}
