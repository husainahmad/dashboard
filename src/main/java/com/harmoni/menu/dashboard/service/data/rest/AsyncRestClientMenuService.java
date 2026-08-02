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
import org.springframework.web.util.UriComponentsBuilder;

import java.io.Serializable;
import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class AsyncRestClientMenuService implements Serializable {

    private final transient MenuProperties menuProperties;
    private final transient WebClient webClient = WebClient.builder().build();
    private static final String BEARER = "Bearer ";

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

    public interface AsyncRestCallback<T> {
        void operationFinished(T result);
    }

    private <T> void makeAsyncRequest(String uri, TypeReference<T> typeReference,
                                      AsyncRestClientMenuService.AsyncRestCallback<T> callback) {
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
                }, error -> log.error("Async request failed uri={}", uri, error));
    }

    public void getAllCategoryAsync(AsyncRestCallback<List<CategoryDto>> callback, Integer brandId) {
        String url = MenuProperties.CATEGORY.formatted(menuProperties.getUrl().getCategories().getBrand(), brandId);
        makeAsyncRequest(url, new TypeReference<>() {
        }, callback);
    }

    public void getAllProductCategoryBrandAsync(AsyncRestCallback<Map<String, Object>> callback,
                                                Integer categoryId, Integer brandId, int page, int size, String search) {
        String url = MenuProperties.CATEGORY_BRAND.formatted(menuProperties.getUrl().getProducts().getCategory(),
                categoryId, brandId).concat("?page=")
                .concat(String.valueOf(page))
                .concat("&size=")
                .concat(String.valueOf(size))
                .concat("&search=")
                .concat(search);
        makeAsyncRequest(url, new TypeReference<>() {
        }, callback);
    }

    public void getAllCustomizationAsync(AsyncRestCallback<Map<String, Object>> callback,
                                         Integer brandId, int page, int size, String search) {
        String url = menuProperties.getUrl().getCustomization()
                .concat("?brandId=")
                .concat(String.valueOf(brandId))
                .concat("&page=")
                .concat(String.valueOf(page))
                .concat("&size=")
                .concat(String.valueOf(size))
                .concat("&search=")
                .concat(search);

        makeAsyncRequest(url, new TypeReference<>() {}, callback);
    }

    public void getAllSkuAsync(AsyncRestCallback<List<SkuDto>> callback) {
        String url = menuProperties.getUrl().getSku();
        makeAsyncRequest(url, new TypeReference<>() {
        }, callback);
    }

    public void getDetailSkuTierPriceAsync(AsyncRestCallback<List<SkuTierPriceDto>> callback,
                                           List<Integer> skuIds, Integer tierId) {
        URI uri = UriComponentsBuilder.fromUriString(menuProperties.getUrl().getSkutierprice())
                .queryParam("skuIds", skuIds.stream().map(String::valueOf).collect(Collectors.joining(",")))
                .queryParam("tierId", tierId).build().toUri();

        makeAsyncRequest(uri.toString(), new TypeReference<>() {
        }, callback);
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
