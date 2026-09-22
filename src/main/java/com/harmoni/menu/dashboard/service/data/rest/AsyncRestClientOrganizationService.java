package com.harmoni.menu.dashboard.service.data.rest;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harmoni.menu.dashboard.configuration.MenuProperties;
import com.harmoni.menu.dashboard.dto.*;
import com.harmoni.menu.dashboard.exception.BusinessBadRequestException;
import com.harmoni.menu.dashboard.exception.BusinessServerRequestException;
import com.harmoni.menu.dashboard.exception.TokenRefreshRequiredException;
import com.harmoni.menu.dashboard.util.VaadinSessionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Reactive client for the organization backend endpoints (chains, stores and
 * tables).
 *
 * <p>Each getter performs an asynchronous {@link WebClient} request and returns
 * the result through an {@link AsyncRestCallback}. A missing or expired token
 * is refreshed transparently via {@link TokenRefreshService}; business
 * failures are reported through {@link BusinessBadRequestException} or
 * {@link BusinessServerRequestException}.
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class AsyncRestClientOrganizationService implements Serializable {

    private final transient MenuProperties menuProperties;
    private final transient WebClient webClient = WebClient.builder().build();
    private static final String BEARER = "Bearer ";

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

    public interface AsyncRestCallback<T> {
        void operationFinished(T result);
    }

    private <T> void makeAsyncRequest(String uri, TypeReference<T> typeReference, AsyncRestCallback<T> callback) {
        makeAsyncRequest(uri, typeReference, callback, null);
    }

    private <T> void makeAsyncRequest(String uri, TypeReference<T> typeReference, AsyncRestCallback<T> callback,
                                       AsyncRestCallback<Throwable> errorCallback) {
        TokenRefreshService.TokenRequest<RestAPIResponse> request = accessToken -> webClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, resolveToken(accessToken))
                .retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessBadRequestException::new))
                .onStatus(HttpStatus.INTERNAL_SERVER_ERROR::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessServerRequestException::new))
                .onStatus(HttpStatus.UNAUTHORIZED::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(response -> new TokenRefreshRequiredException(response.toString())))
                .bodyToMono(RestAPIResponse.class);

        TokenRefreshService.getInstance().withTokenRefresh(request)
                .subscribe(result -> {
                    T data = objectMapper.convertValue(
                            Objects.requireNonNull(result).getData(),
                            typeReference
                    );
                    callback.operationFinished(data);
                }, error -> {
                    log.error("Async request failed uri={}", uri, error);
                    if (errorCallback != null) {
                        errorCallback.operationFinished(error);
                    }
                });
    }

    public void getAllChainByBrandIdAsync(AsyncRestCallback<List<ChainDto>> callback, Integer brandId) {
        getAllChainByBrandIdAsync(callback, null, brandId);
    }

    public void getAllChainByBrandIdAsync(AsyncRestCallback<List<ChainDto>> callback,
                                          AsyncRestCallback<Throwable> errorCallback, Integer brandId) {
        String uri = String.format("%s/brand/%d", menuProperties.getUrl().getChain(), brandId);
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
        String uri = MenuProperties.CATEGORY.formatted(menuProperties.getUrl().getBrand(), id);
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback);
    }

    public void getAllTierByBrandAsync(AsyncRestCallback<List<TierDto>> callback, Integer id, TierTypeDto tierTypeDto) {
        getAllTierByBrandAsync(callback, null, id, tierTypeDto);
    }

    public void getAllTierByBrandAsync(AsyncRestCallback<List<TierDto>> callback,
                                       AsyncRestCallback<Throwable> errorCallback, Integer id, TierTypeDto tierTypeDto) {
        String uri = String.format("%s/brand/%d/type/%s", menuProperties.getUrl().getTier(), id, tierTypeDto);
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback, errorCallback);
    }

    public void getTierMenuByBrandAsync(AsyncRestCallback<List<TierMenuDto>> callback, Integer id) {
        getTierMenuByBrandAsync(callback, null, id);
    }

    public void getTierMenuByBrandAsync(AsyncRestCallback<List<TierMenuDto>> callback,
                                        AsyncRestCallback<Throwable> errorCallback, Integer id) {
        String uri = menuProperties.getUrl().getTiers().getMenu()
                .concat("?brandId=")
                .concat(id.toString());
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback, errorCallback);
    }

    public void getTierServiceByBrandAsync(AsyncRestCallback<List<TierServiceDto>> callback, Integer id) {
        getTierServiceByBrandAsync(callback, null, id);
    }

    public void getTierServiceByBrandAsync(AsyncRestCallback<List<TierServiceDto>> callback,
                                           AsyncRestCallback<Throwable> errorCallback, Integer id) {
        String uri = menuProperties.getUrl().getTiers().getService()
                .concat("?brandId=")
                .concat(id.toString());
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback, errorCallback);
    }

    public void getAllStoreAsync(AsyncRestCallback<Map<String, Object>> callback, Integer chainId, int page, int size, String search) {
        getAllStoreAsync(callback, null, chainId, page, size, search);
    }

    public void getAllStoreAsync(AsyncRestCallback<Map<String, Object>> callback,
                                 AsyncRestCallback<Throwable> errorCallback, Integer chainId, int page, int size, String search) {
        String uri = menuProperties.getUrl().getStore();
        uri = uri
                .concat("?chainId=").concat(String.valueOf(chainId))
                .concat("&page=")
                .concat(String.valueOf(page))
                .concat("&size=")
                .concat(String.valueOf(size))
                .concat("&search=")
                .concat(search);

        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback, errorCallback);
    }

    public void getAllUserByChainAsync(AsyncRestCallback<Map<String, Object>> callback, Integer chainId, int page, int size, String search) {
        getAllUserByChainAsync(callback, null, chainId, page, size, search);
    }

    public void getAllUserByChainAsync(AsyncRestCallback<Map<String, Object>> callback,
                                       AsyncRestCallback<Throwable> errorCallback, Integer chainId, int page, int size, String search) {
        String uri = String.format("%s/%d?page=%d&size=%d&search=%s",
                menuProperties.getUrl().getUsers().getChain(),
                chainId, page, size, search);
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

    private static String resolveToken(String accessToken) {
        if (ObjectUtils.isNotEmpty(accessToken)) {
            return BEARER.concat(accessToken);
        }
        return getTokenString();
    }

    private static String getTokenString() {
        String token = VaadinSessionUtil.getAttribute(VaadinSessionUtil.JWT_TOKEN, String.class);
        if (ObjectUtils.isNotEmpty(token)) {
            return BEARER.concat(token);
        }
        return token;
    }

}
