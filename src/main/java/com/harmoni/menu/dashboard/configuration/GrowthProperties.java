package com.harmoni.menu.dashboard.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.Serializable;

@Configuration
@Data
@ConfigurationProperties("growth")
public class GrowthProperties implements Serializable {

    /** Base URL of the API gateway, which routes growth endpoints to the customer and AI services. */
    private String url = "http://localhost:8080";
}
