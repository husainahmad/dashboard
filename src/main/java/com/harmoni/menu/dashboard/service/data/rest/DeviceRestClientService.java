package com.harmoni.menu.dashboard.service.data.rest;

import com.harmoni.menu.dashboard.configuration.MenuProperties;
import com.harmoni.menu.dashboard.dto.DeviceDto;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Blocking client for the store device endpoints.
 */
@Service
public class DeviceRestClientService extends RestClientService {

    private final transient MenuProperties menuProperties;

    public DeviceRestClientService(MenuProperties menuProperties,
                                   TokenRefreshService tokenRefreshService) {
        super(tokenRefreshService);
        this.menuProperties = menuProperties;
    }

    public Mono<RestAPIResponse> listDevices(Integer storeId) {
        return get(String.format(menuProperties.getUrl().getDevices().getList(), storeId));
    }

    public Mono<RestAPIResponse> createDevice(Integer storeId, DeviceDto dto) {
        return post(String.format(menuProperties.getUrl().getDevices().getList(), storeId),
                Mono.just(dto), DeviceDto.class);
    }

    public Mono<RestAPIResponse> updateDevice(Integer id, DeviceDto dto) {
        return put(String.format(menuProperties.getUrl().getDevices().getById(), id),
                Mono.just(dto), DeviceDto.class);
    }

    public Mono<RestAPIResponse> deleteDevice(Integer id) {
        return delete(String.format(menuProperties.getUrl().getDevices().getById(), id));
    }
}
