package com.harmoni.menu.dashboard.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.Serializable;

/**
 * Root configuration for the auth backend.
 *
 * <p>
 * Binds the {@code auth.*} keys, with the login and token-refresh URLs exposed
 * through the nested {@link AuthUrlProperties} under {@code auth.url.*}.
 * </p>
 */

@Configuration
@Data
@ConfigurationProperties("auth")
public class AuthProperties implements Serializable {
    private transient AuthUrlProperties url;
}
