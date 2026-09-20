package com.harmoni.menu.dashboard.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.Serializable;

/**
 * Root configuration for the menu backend.
 *
 * <p>
 * Binds the {@code menu.*} keys, with the per-resource URLs exposed through the
 * nested {@link UrlProperties} under {@code menu.url.*} and the category URL
 * templates provided as constants.
 * </p>
 */

@Configuration
@Data
@ConfigurationProperties("menu")
public class MenuProperties implements Serializable {
    /**
     * Formats a category listing URL from the brand-category base URL and the
     * brand id.
     */
    public static final String CATEGORY = "%s/%d";

    /**
     * Formats a category listing URL from the product-category base URL, the
     * category id and the brand id.
     */
    public static final String CATEGORY_BRAND = "%s/%d/%d";
    private transient UrlProperties url;
}
