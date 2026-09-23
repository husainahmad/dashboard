package com.harmoni.menu.dashboard.service.data.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.harmoni.menu.dashboard.configuration.MenuProperties;
import com.harmoni.menu.dashboard.dto.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Reactive client for the organization backend endpoints (chains, stores and
 * tables).
 *
 * <p>Each getter performs an asynchronous {@link org.springframework.web.reactive.function.client.WebClient}
 * request and returns the result through an {@link AsyncRestClientBase.AsyncRestCallback}.
 * A missing or expired token is refreshed transparently via
 * {@link TokenRefreshService}; business failures are reported through the
 * service's mapped exceptions.
 */
@Service
public class AsyncRestClientOrganizationService extends AsyncRestClientBase {

    private final transient MenuProperties menuProperties;

    public AsyncRestClientOrganizationService(MenuProperties menuProperties,
                                              TokenRefreshService tokenRefreshService) {
        super(tokenRefreshService);
        this.menuProperties = menuProperties;
    }

    public void getAllChainByBrandIdAsync(AsyncRestCallback<List<ChainDto>> callback, Integer brandId) {
        getAllChainByBrandIdAsync(callback, null, brandId);
    }

    public void getAllChainByBrandIdAsync(AsyncRestCallback<List<ChainDto>> callback,
                                          AsyncRestCallback<Throwable> errorCallback, Integer brandId) {
        String uri = String.format(menuProperties.getUrl().getChainByBrand(), brandId);
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback, errorCallback);
    }

    public void getAllBrandAsync(AsyncRestCallback<List<BrandDto>> callback) {
        getAllBrandAsync(callback, null);
    }

    public void getAllBrandAsync(AsyncRestCallback<List<BrandDto>> callback,
                                 AsyncRestCallback<Throwable> errorCallback) {
        String uri = menuProperties.getUrl().getBrand();
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback, errorCallback);
    }

    public void getDetailBrandAsync(AsyncRestCallback<BrandDto> callback, Long id) {
        String uri = URL_FORMAT.formatted(menuProperties.getUrl().getBrand(), id);
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback);
    }

    public void getAllTierByBrandAsync(AsyncRestCallback<List<TierDto>> callback, Integer id, TierTypeDto tierTypeDto) {
        getAllTierByBrandAsync(callback, null, id, tierTypeDto);
    }

    public void getAllTierByBrandAsync(AsyncRestCallback<List<TierDto>> callback,
                                       AsyncRestCallback<Throwable> errorCallback, Integer id, TierTypeDto tierTypeDto) {
        String uri = String.format(menuProperties.getUrl().getTiers().getByBrandType(), id, tierTypeDto);
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback, errorCallback);
    }

    public void getTierMenuByBrandAsync(AsyncRestCallback<List<TierMenuDto>> callback, Integer id) {
        getTierMenuByBrandAsync(callback, null, id);
    }

    public void getTierMenuByBrandAsync(AsyncRestCallback<List<TierMenuDto>> callback,
                                        AsyncRestCallback<Throwable> errorCallback, Integer id) {
        String uri = String.format(menuProperties.getUrl().getTiers().getMenuByBrandId(), id);
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback, errorCallback);
    }

    public void getTierServiceByBrandAsync(AsyncRestCallback<List<TierServiceDto>> callback, Integer id) {
        getTierServiceByBrandAsync(callback, null, id);
    }

    public void getTierServiceByBrandAsync(AsyncRestCallback<List<TierServiceDto>> callback,
                                           AsyncRestCallback<Throwable> errorCallback, Integer id) {
        String uri = String.format(menuProperties.getUrl().getTiers().getServiceByBrandId(), id);
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback, errorCallback);
    }

    public void getAllStoreAsync(AsyncRestCallback<Map<String, Object>> callback, Integer chainId, int page, int size, String search) {
        getAllStoreAsync(callback, null, chainId, page, size, search);
    }

    public void getAllStoreAsync(AsyncRestCallback<Map<String, Object>> callback,
                                 AsyncRestCallback<Throwable> errorCallback, Integer chainId, int page, int size, String search) {
        String uri = String.format(menuProperties.getUrl().getStoreQuery(), chainId, page, size, search);

        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback, errorCallback);
    }

    public void getAllUserByChainAsync(AsyncRestCallback<Map<String, Object>> callback, Integer chainId, int page, int size, String search) {
        getAllUserByChainAsync(callback, null, chainId, page, size, search);
    }

    public void getAllUserByChainAsync(AsyncRestCallback<Map<String, Object>> callback,
                                       AsyncRestCallback<Throwable> errorCallback, Integer chainId, int page, int size, String search) {
        String uri = String.format(menuProperties.getUrl().getUsers().getChainPage(), chainId, page, size, search);
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback, errorCallback);
    }

    public void getAllServicesAsync(AsyncRestCallback<List<ServiceDto>> callback) {
        getAllServicesAsync(callback, null);
    }

    public void getAllServicesAsync(AsyncRestCallback<List<ServiceDto>> callback,
                                    AsyncRestCallback<Throwable> errorCallback) {
        String uri = menuProperties.getUrl().getService();
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback, errorCallback);
    }
}