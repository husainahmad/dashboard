package com.harmoni.menu.dashboard.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.Serializable;

@Configuration
@Data
@ConfigurationProperties("setting")
public class SettingProperties implements Serializable {
    private transient SettingUrlProperties url;
}
