package com.harmoni.menu.dashboard.service.data.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.harmoni.menu.dashboard.configuration.SettingProperties;
import com.harmoni.menu.dashboard.dto.*;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Reactive client for the settings backend endpoints (services and tables).
 *
 * <p>Each getter performs an asynchronous {@link org.springframework.web.reactive.function.client.WebClient}
 * request and returns the result through an {@link AsyncRestClientBase.AsyncRestCallback}.
 * A missing or expired token is refreshed transparently via
 * {@link TokenRefreshService}; business failures are reported through the
 * service's mapped exceptions.
 */
@Service
public class AsyncRestClientSettingService extends AsyncRestClientBase {

    private final transient SettingProperties settingProperties;

    public AsyncRestClientSettingService(SettingProperties settingProperties,
                                         TokenRefreshService tokenRefreshService) {
        super(tokenRefreshService);
        this.settingProperties = settingProperties;
    }

    public void getAllService(AsyncRestCallback<List<ServiceDto>> callback) {
        getAllService(callback, null);
    }

    public void getAllService(AsyncRestCallback<List<ServiceDto>> callback,
                              AsyncRestCallback<Throwable> errorCallback) {
        makeAsyncRequest(settingProperties.getUrl().getService(), new TypeReference<List<ServiceDto>>() {}, callback, errorCallback);
    }

    public void getAllTables(AsyncRestCallback<List<TableDto>> callback) {
        getAllTables(callback, null);
    }

    public void getAllTables(AsyncRestCallback<List<TableDto>> callback,
                             AsyncRestCallback<Throwable> errorCallback) {
        makeAsyncRequest(settingProperties.getUrl().getTable(), new TypeReference<List<TableDto>>() {}, callback, errorCallback);
    }

    public void getAllTablesByStore(AsyncRestCallback<List<TableDto>> callback, Integer storeId) {
        getAllTablesByStore(callback, null, storeId);
    }

    public void getAllTablesByStore(AsyncRestCallback<List<TableDto>> callback,
                                    AsyncRestCallback<Throwable> errorCallback, Integer storeId) {
        String uri = String.format(settingProperties.getUrl().getTableByStore(), storeId);
        makeAsyncRequest(uri, new TypeReference<List<TableDto>>() {}, callback, errorCallback);
    }
}