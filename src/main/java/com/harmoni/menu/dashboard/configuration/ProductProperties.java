package com.harmoni.menu.dashboard.configuration;

import lombok.Data;

import java.io.Serializable;

/**
 * Holds the REST URLs of the product endpoints.
 *
 * <p>
 * Binds the {@code menu.url.products.*} keys for the bulk, sku and category
 * paths, plus the nested {@link ImageProperties} upload URLs.
 * </p>
 */

@Data
public class ProductProperties implements Serializable {
    private String bulk;
    private String sku;
    private String category;
    private ImageProperties images;
}
