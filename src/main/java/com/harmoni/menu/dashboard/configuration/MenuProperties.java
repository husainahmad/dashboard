package com.harmoni.menu.dashboard.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.Serializable;

@Configuration
@Data
@ConfigurationProperties("menu")
public class MenuProperties implements Serializable {
    public static final String CATEGORY = "%s/%d";
    public static final String CATEGORY_BRAND = "%s/%d/%d";
    private transient UrlProperties url;
}
