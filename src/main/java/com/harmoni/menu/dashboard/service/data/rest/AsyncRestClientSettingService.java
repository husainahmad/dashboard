package com.harmoni.menu.dashboard.service.data.rest;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harmoni.menu.dashboard.configuration.SettingProperties;
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
import java.util.Objects;

/**
 * Reactive client for the settings backend endpoints (services).
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
public class AsyncRestClientSettingService implements Serializable {

    private final transient SettingProperties settingProperties;
    private final transient WebClient webClient = WebClient.builder().build();
    private static final String BEARER = "Bearer ";

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

    public interface AsyncRestCallback<T> {
        void operationFinished(T result);
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
        String uri = settingProperties.getUrl().getTable().concat("/store/%d".formatted(storeId));
        makeAsyncRequest(uri, new TypeReference<List<TableDto>>() {}, callback, errorCallback);
    }

    private <T> void makeAsyncRequest(String uri, TypeReference<T> typeReference,
                                      AsyncRestClientSettingService.AsyncRestCallback<T> callback) {
        makeAsyncRequest(uri, typeReference, callback, null);
    }

    private <T> void makeAsyncRequest(String uri, TypeReference<T> typeReference,
                                      AsyncRestClientSettingService.AsyncRestCallback<T> callback,
                                      AsyncRestClientSettingService.AsyncRestCallback<Throwable> errorCallback) {
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
