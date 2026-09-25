package com.harmoni.menu.dashboard.service.data.rest;

import com.harmoni.menu.dashboard.configuration.SettingProperties;
import com.harmoni.menu.dashboard.dto.TableDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Blocking REST client for the settings endpoints: tables. Builds URLs from
 * {@link SettingProperties}, extends {@link RestClientService} and returns
 * {@link Mono} responses.
 */
@Service
@Slf4j
public class RestClientSettingService extends RestClientService {

    private final SettingProperties settingProperties;

    /**
     * Constructs the service with the required configuration and token refresh support.
     *
     * @param settingProperties   configuration for setting endpoints
     * @param tokenRefreshService service to refresh expired tokens
     */
    public RestClientSettingService(SettingProperties settingProperties,
                                    TokenRefreshService tokenRefreshService) {
        super(tokenRefreshService);
        this.settingProperties = settingProperties;
    }

    /**
     * Creates a new table.
     *
     * @param tableDto the table to create
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> createTable(TableDto tableDto) {
        return post(settingProperties.getUrl().getTable(), Mono.just(tableDto), TableDto.class);
    }

    /**
     * Updates an existing table.
     *
     * @param tableDto the table to update
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> updateTable(TableDto tableDto) {
        return put(URL_FORMAT.formatted(settingProperties.getUrl().getTable(), tableDto.getId()),
                Mono.just(tableDto), TableDto.class);
    }

    /**
     * Deletes an existing table.
     *
     * @param tableDto the table to delete
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> deleteTable(TableDto tableDto) {
        return delete(URL_FORMAT.formatted(settingProperties.getUrl().getTable(), tableDto.getId()));
    }
}
