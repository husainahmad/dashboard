package com.harmoni.menu.dashboard.configuration;

import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@RequiredArgsConstructor
@Configuration
@ComponentScan("com.harmoni.menu.dashboard")
public class ComponentConfig {

    private final MenuProperties menuProperties;

    @Bean
    public AsyncRestClientMenuService asyncRestClientMenuService() {
        return new AsyncRestClientMenuService(menuProperties);
    }

    @Bean
    public AsyncRestClientOrganizationService asyncRestClientOrganizationService() {
        return new AsyncRestClientOrganizationService(menuProperties);
    }

    @Bean
    public RestClientMenuService restClientMenuService() {
        return new RestClientMenuService(menuProperties);
    }

}
