package com.harmoni.menu.dashboard.service.data.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.harmoni.menu.dashboard.configuration.MenuProperties;
import com.harmoni.menu.dashboard.dto.*;
import org.springframework.stereotype.Service;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
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

    /**
     * Constructs the service with the required configuration and token refresh support.
     *
     * @param menuProperties      configuration for menu endpoints
     * @param tokenRefreshService service to refresh expired tokens
     */
    public AsyncRestClientOrganizationService(MenuProperties menuProperties,
                                              TokenRefreshService tokenRefreshService) {
        super(tokenRefreshService);
        this.menuProperties = menuProperties;
    }

    /**
     * Asynchronously retrieves all chains for a given brand.
     *
     * @param callback      success callback with the list of chains
     * @param errorCallback optional error callback for handling failures
     * @param brandId       the ID of the brand to filter chains
     */
    public void getAllChainByBrandIdAsync(AsyncRestCallback<List<ChainDto>> callback, Integer brandId) {
        getAllChainByBrandIdAsync(callback, null, brandId);
    }

    /**
     * Asynchronously retrieves all chains for a given brand with error handling.
     *
     * @param callback      success callback with the list of chains
     * @param errorCallback error callback for handling failures
     * @param brandId       the ID of the brand to filter chains
     */
    public void getAllChainByBrandIdAsync(AsyncRestCallback<List<ChainDto>> callback,
                                          AsyncRestCallback<Throwable> errorCallback, Integer brandId) {
        String uri = String.format(menuProperties.getUrl().getChainByBrand(), brandId);
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback, errorCallback);
    }

    /**
     * Asynchronously retrieves the nested brand → chain → store tree.
     *
     * @param callback      success callback with the tree
     * @param errorCallback optional error callback for handling failures
     */
    public void getOrganizationTreeAsync(AsyncRestCallback<OrganizationTreeDto> callback,
                                         AsyncRestCallback<Throwable> errorCallback) {
        makeAsyncRequest(menuProperties.getUrl().getOrganizationTree(), new TypeReference<>() {
        }, callback, errorCallback);
    }

    /**
     * Asynchronously retrieves all brands.
     *
     * @param callback      success callback with the list of brands
     * @param errorCallback optional error callback for handling failures
     */
    public void getAllBrandAsync(AsyncRestCallback<List<BrandDto>> callback) {
        getAllBrandAsync(callback, null);
    }

    /**
     * Asynchronously retrieves all brands with error handling.
     *
     * @param callback      success callback with the list of brands
     * @param errorCallback error callback for handling failures
     */
    public void getAllBrandAsync(AsyncRestCallback<List<BrandDto>> callback,
                                 AsyncRestCallback<Throwable> errorCallback) {
        String uri = menuProperties.getUrl().getBrand();
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback, errorCallback);
    }

    /**
     * Asynchronously retrieves every tier of a brand, regardless of type.
     *
     * @param callback      success callback with the list of tiers
     * @param errorCallback optional error callback for handling failures
     * @param brandId       the ID of the brand
     */
    public void getAllTierByBrandAsync(AsyncRestCallback<List<TierDto>> callback,
                                       AsyncRestCallback<Throwable> errorCallback, Integer brandId) {
        String uri = String.format("%s/brand/%d", menuProperties.getUrl().getTier(), brandId);
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback, errorCallback);
    }

    /**
     * Asynchronously retrieves all tiers for a given brand and tier type.
     *
     * @param callback      success callback with the list of tiers
     * @param errorCallback optional error callback for handling failures
     * @param id            the ID of the brand to filter tiers
     * @param tierTypeDto   the type of tier to filter
     */
    public void getAllTierByBrandAsync(AsyncRestCallback<List<TierDto>> callback, Integer id, TierTypeDto tierTypeDto) {
        getAllTierByBrandAsync(callback, null, id, tierTypeDto);
    }

    /**
     * Asynchronously retrieves all tiers for a given brand and tier type with error handling.
     *
     * @param callback      success callback with the list of tiers
     * @param errorCallback error callback for handling failures
     * @param id            the ID of the brand to filter tiers
     * @param tierTypeDto   the type of tier to filter
     */
    public void getAllTierByBrandAsync(AsyncRestCallback<List<TierDto>> callback,
                                       AsyncRestCallback<Throwable> errorCallback, Integer id, TierTypeDto tierTypeDto) {
        String uri = String.format(menuProperties.getUrl().getTiers().getByBrandType(), id, tierTypeDto);
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback, errorCallback);
    }

    /**
     * Asynchronously retrieves the tier menu for a given brand.
     *
     * @param callback      success callback with the list of tier menus
     * @param errorCallback optional error callback for handling failures
     * @param id            the ID of the brand to filter tier menus
     */
    public void getTierMenuByBrandAsync(AsyncRestCallback<List<TierMenuDto>> callback,
                                        AsyncRestCallback<Throwable> errorCallback, Integer id) {
        String uri = String.format(menuProperties.getUrl().getTiers().getMenuByBrandId(), id);
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback, errorCallback);
    }

    /**
     * Asynchronously retrieves the tier service for a given brand.
     *
     * @param callback      success callback with the list of tier services
     * @param errorCallback optional error callback for handling failures
     * @param id            the ID of the brand to filter tier services
     */
    public void getTierServiceByBrandAsync(AsyncRestCallback<List<TierServiceDto>> callback,
                                           AsyncRestCallback<Throwable> errorCallback, Integer id) {
        String uri = String.format(menuProperties.getUrl().getTiers().getServiceByBrandId(), id);
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback, errorCallback);
    }

    /**
     * Asynchronously retrieves all stores for a given chain.
     *
     * @param callback      success callback with the list of stores
     * @param errorCallback optional error callback for handling failures
     * @param chainId       the ID of the chain to filter stores
     * @param page          the page number for pagination
     * @param size          the number of items per page
     * @param search        the search term to filter stores
     */
    public void getAllStoreAsync(AsyncRestCallback<Map<String, Object>> callback,
                                 AsyncRestCallback<Throwable> errorCallback, Integer chainId, int page, int size, String search) {
        String uri = String.format(menuProperties.getUrl().getStoreQuery(), chainId, page, size, search);

        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback, errorCallback);
    }

    /**
     * Asynchronously retrieves all users for a given chain.
     *
     * @param callback      success callback with the list of users
     * @param errorCallback optional error callback for handling failures
     * @param chainId       the ID of the chain to filter users
     * @param page          the page number for pagination
     * @param size          the number of items per page
     * @param search        the search term to filter users
     */
    public void getAllUserByChainAsync(AsyncRestCallback<Map<String, Object>> callback,
                                       AsyncRestCallback<Throwable> errorCallback, Integer chainId, int page, int size, String search) {
        String uri = String.format(menuProperties.getUrl().getUsers().getChainPage(), chainId, page, size, search);
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback, errorCallback);
    }

    /**
     * Asynchronously retrieves a page of users across the stores in scope.
     *
     * @param callback      success callback with the page payload
     * @param errorCallback optional error callback for handling failures
     * @param storeIds      comma-separated store ids in the caller's scope
     * @param page          1-based page number
     * @param size          page size
     * @param search        the search term to filter users
     * @param roleId        optional role filter
     * @param scopeType     optional scope filter
     */
    public void getUsersPageAsync(AsyncRestCallback<Map<String, Object>> callback,
                                  AsyncRestCallback<Throwable> errorCallback, String storeIds,
                                  int page, int size, String search, String roleId, String scopeType) {
        String encodedSearch = URLEncoder.encode(search == null ? "" : search, StandardCharsets.UTF_8);
        String uri = String.format(menuProperties.getUrl().getUsers().getPage(),
                storeIds, page, size, encodedSearch,
                roleId == null ? "" : roleId, scopeType == null ? "" : scopeType);
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback, errorCallback);
    }

    /**
     * Asynchronously retrieves all services.
     *
     * @param callback      success callback with the list of services
     * @param errorCallback optional error callback for handling failures
     */
    public void getAllServicesAsync(AsyncRestCallback<List<ServiceDto>> callback,
                                    AsyncRestCallback<Throwable> errorCallback) {
        String uri = menuProperties.getUrl().getService();
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback, errorCallback);
    }

    /**
     * Asynchronously retrieves one page of services with their sub-services.
     *
     * @param callback      success callback with the paged response
     * @param errorCallback optional error callback for handling failures
     * @param page          the page number, 1-based
     * @param size          the number of services per page
     */
    public void getAllServicesPageAsync(AsyncRestCallback<Map<String, Object>> callback,
                                        AsyncRestCallback<Throwable> errorCallback,
                                        int page, int size) {
        String uri = String.format(menuProperties.getUrl().getServicePage(), page, size);
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback, errorCallback);
    }
}