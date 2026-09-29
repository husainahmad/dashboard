package com.harmoni.menu.dashboard.service.data.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.harmoni.menu.dashboard.configuration.MenuProperties;
import com.harmoni.menu.dashboard.dto.*;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Reactive client for the menu-related backend endpoints (brands, categories,
 * products, SKUs, customizations and prices).
 *
 * <p>Each getter performs an asynchronous {@link org.springframework.web.reactive.function.client.WebClient}
 * request and returns the result through an {@link AsyncRestClientBase.AsyncRestCallback}.
 * A missing or expired token is refreshed transparently via
 * {@link TokenRefreshService}; business failures are reported through the
 * service's mapped exceptions.
 */
@Service
public class AsyncRestClientMenuService extends AsyncRestClientBase {

    private final transient MenuProperties menuProperties;

    /**
     * Constructs the service with the required configuration and token refresh support.
     *
     * @param menuProperties      configuration for menu endpoints
     * @param tokenRefreshService service to refresh expired tokens
     */
    public AsyncRestClientMenuService(MenuProperties menuProperties,
                                      TokenRefreshService tokenRefreshService) {
        super(tokenRefreshService);
        this.menuProperties = menuProperties;
    }

    /**
     * Asynchronously retrieves all brands.
     *
     * @param callback      success callback with the list of brands
     * @param errorCallback optional error callback for handling failures
     */
    /**
     * Asynchronously retrieves a single product by id.
     *
     * <p>Used to resolve the display name of a target that a saved promotion already
     * points at: the form restores targets as bare ids and needs a name for the preview,
     * and the by-brand product listing can only be queried one category at a time, which
     * a restored target does not carry.</p>
     *
     * @param callback      success callback with the product
     * @param errorCallback optional error callback for handling failures
     * @param productId     the product id
     */
    public void getProductAsync(AsyncRestCallback<ProductDto> callback,
                                AsyncRestCallback<Throwable> errorCallback, Integer productId) {
        String url = URL_FORMAT.formatted(menuProperties.getUrl().getProduct(), productId);
        makeAsyncRequest(url, new TypeReference<>() {
        }, callback, errorCallback);
    }

    public void getAllCategoryAsync(AsyncRestCallback<List<CategoryDto>> callback,
                                    AsyncRestCallback<Throwable> errorCallback, Integer brandId) {
        String url = URL_FORMAT.formatted(menuProperties.getUrl().getCategories().getBrand(), brandId);
        makeAsyncRequest(url, new TypeReference<>() {
        }, callback, errorCallback);
    }

    /**
     * Asynchronously retrieves all brands.
     *
     * @param callback      success callback with the list of brands
     * @param errorCallback optional error callback for handling failures
     */
    public void getAllProductCategoryBrandAsync(AsyncRestCallback<Map<String, Object>> callback,
                                                AsyncRestCallback<Throwable> errorCallback,
                                                Integer categoryId, Integer brandId, int page, int size, String search) {
        String url = String.format(menuProperties.getUrl().getProducts().getCategoryQuery(),
                categoryId, brandId, page, size, search);
        makeAsyncRequest(url, new TypeReference<>() {
        }, callback, errorCallback);
    }

    /**
     * Asynchronously retrieves all customizations.
     *
     * @param callback      success callback with the list of customizations
     * @param errorCallback optional error callback for handling failures
     */
    public void getAllCustomizationAsync(AsyncRestCallback<Map<String, Object>> callback,
                                         AsyncRestCallback<Throwable> errorCallback,
                                         Integer brandId, int page, int size, String search) {
        String url = String.format(menuProperties.getUrl().getCustomizationQuery(), brandId, page, size, search);

        makeAsyncRequest(url, new TypeReference<>() {}, callback, errorCallback);
    }

    /**
     * Asynchronously retrieves SKU tier prices for the specified SKUs and tier.
     *
     * @param callback success callback with the list of SKU tier prices
     * @param skuIds   list of SKU IDs to retrieve prices for
     * @param tierId   the tier ID to filter prices by
     */
    public void getDetailSkuTierPriceAsync(AsyncRestCallback<List<SkuTierPriceDto>> callback,
                                           List<Integer> skuIds, Integer tierId) {
        URI uri = UriComponentsBuilder.fromUriString(menuProperties.getUrl().getSkutierprice())
                .queryParam("skuIds", skuIds.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .queryParam("tierId", tierId).build().toUri();

        makeAsyncRequest(uri.toString(), new TypeReference<>() {
        }, callback);
    }
}