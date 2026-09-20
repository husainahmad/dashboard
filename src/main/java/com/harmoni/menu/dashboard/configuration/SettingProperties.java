package com.harmoni.menu.dashboard.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.Serializable;

/**
 * Root configuration for the settings backend.
 *
 * <p>
 * Binds the {@code setting.*} keys, with the service and table URLs exposed
 * through the nested {@link SettingUrlProperties} under {@code setting.url.*}.
 * </p>
 */

@Configuration
@Data
@ConfigurationProperties("setting")
public class SettingProperties implements Serializable {
    private transient SettingUrlProperties url;
}
