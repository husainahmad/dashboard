package com.harmoni.menu.dashboard.configuration;

import lombok.Data;

import java.io.Serializable;

/**
 * Holds the REST URLs of the tier management endpoints.
 *
 * <p>
 * Binds the {@code menu.url.tiers.*} keys for the brand, service and menu
 * lookups, plus the nested {@link TierMenuProperties} and
 * {@link TierServiceProperties} update URLs; consumed as the {@code tiers}
 * subtree of {@link UrlProperties}.
 * </p>
 */

@Data
public class TierProperties implements Serializable {
    private String brand;
    private String service;
    private String serviceByBrandId;
    private String byBrandType;
    private String menu;
    private String menuByBrandId;
    private TierMenuProperties menus;
    private TierServiceProperties services;

}
