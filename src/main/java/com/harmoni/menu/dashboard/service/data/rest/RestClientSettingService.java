package com.harmoni.menu.dashboard.service.data.rest;

import com.harmoni.menu.dashboard.configuration.SettingProperties;
import com.harmoni.menu.dashboard.dto.FloorDto;
import com.harmoni.menu.dashboard.dto.TableDto;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
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

    /**
     * Persists the floor plan of one floor in a single batch.
     *
     * @param tables the tables carrying position, size, shape and property edits
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> saveTableLayout(List<TableDto> tables) {
        return put(settingProperties.getUrl().getTable() + "/layout", Mono.just(tables), List.class);
    }

    /**
     * Creates a new floor.
     *
     * @param floorDto the floor to create
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> createFloor(FloorDto floorDto) {
        return post(settingProperties.getUrl().getFloor(), Mono.just(floorDto), FloorDto.class);
    }

    /**
     * Updates an existing floor.
     *
     * @param floorDto the floor to update
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> updateFloor(FloorDto floorDto) {
        return put(URL_FORMAT.formatted(settingProperties.getUrl().getFloor(), floorDto.getId()),
                Mono.just(floorDto), FloorDto.class);
    }

    /**
     * Deletes an existing floor. The server refuses while it still has tables.
     *
     * @param floorDto the floor to delete
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> deleteFloor(FloorDto floorDto) {
        return delete(URL_FORMAT.formatted(settingProperties.getUrl().getFloor(), floorDto.getId()));
    }
}
