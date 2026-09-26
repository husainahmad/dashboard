package com.harmoni.menu.dashboard.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.Serializable;

/**
 * Root configuration for the standalone customer backend.
 *
 * <p>
 * Binds the {@code customer.*} keys, with the per-resource URLs exposed through
 * the nested {@link CustomerUrlProperties} under {@code customer.url.*}.
 * </p>
 *
 * <p>
 * The customer service is a separate microservice exposing plain JSON (no
 * {@code RestAPIResponse} envelope), so it gets its own base URL instead of
 * reusing {@link MenuProperties}.
 * </p>
 */

@Configuration
@Data
@ConfigurationProperties("customer")
public class CustomerProperties implements Serializable {
    private transient CustomerUrlProperties url = new CustomerUrlProperties();
}
