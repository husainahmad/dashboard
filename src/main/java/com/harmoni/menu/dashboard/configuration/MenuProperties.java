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
 * nested {@link UrlProperties} under {@code menu.url.*}.
 * </p>
 */

@Configuration
@Data
@ConfigurationProperties("menu")
public class MenuProperties implements Serializable {
    private transient UrlProperties url;
}
