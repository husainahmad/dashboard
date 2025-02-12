package com.harmoni.menu.dashboard.service.data.rest;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harmoni.menu.dashboard.configuration.MenuProperties;
import com.harmoni.menu.dashboard.dto.*;
import com.harmoni.menu.dashboard.exception.BusinessBadRequestException;
import com.harmoni.menu.dashboard.exception.BusinessServerRequestException;
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
        WebClient.ResponseSpec responseSpec = webClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, getTokenString())
                .retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessBadRequestException::new))
                .onStatus(HttpStatus.INTERNAL_SERVER_ERROR::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessServerRequestException::new));

        responseSpec.toEntity(RestAPIResponse.class).subscribe(result -> {
            T data = objectMapper.convertValue(
                    Objects.requireNonNull(result.getBody()).getData(),
                    typeReference
            );
            callback.operationFinished(data);
        });
    }

    public void getAllChainByBrandIdAsync(AsyncRestCallback<List<ChainDto>> callback, Integer brandId) {
        String uri = String.format("%s/brand/%d", menuProperties.getUrl().getChain(), brandId);
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback);
    }

    public void getAllBrandAsync(AsyncRestCallback<List<BrandDto>> callback) {
        String uri = menuProperties.getUrl().getBrand();
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback);
    }

    public void getDetailBrandAsync(AsyncRestCallback<BrandDto> callback, Long id) {
        String uri = MenuProperties.CATEGORY.formatted(menuProperties.getUrl().getBrand(), id);
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback);
    }

    public void getAllTierByBrandAsync(AsyncRestCallback<List<TierDto>> callback, Integer id, TierTypeDto tierTypeDto) {
        String uri = String.format("%s/brand/%d/type/%s", menuProperties.getUrl().getTier(), id, tierTypeDto);
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback);
    }

    public void getTierMenuByBrandAsync(AsyncRestCallback<List<TierMenuDto>> callback, Integer id) {
        String uri = menuProperties.getUrl().getTiers().getMenu()
                .concat("?brandId=")
                .concat(id.toString());
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback);
    }

    public void getTierServiceByBrandAsync(AsyncRestCallback<List<TierServiceDto>> callback, Integer id) {
        String uri = menuProperties.getUrl().getTiers().getService()
                .concat("?brandId=")
                .concat(id.toString());
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback);
    }

    public void getAllStoreAsync(AsyncRestCallback<Map<String, Object>> callback, Integer chainId, int page, int size, String search) {
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
        }, callback);
    }

    public void getAllUserByChainAsync(AsyncRestCallback<Map<String, Object>> callback, Integer chainId, int page, int size, String search) {
        String uri = String.format("%s/%d?page=%d&size=%d&search=%s",
                menuProperties.getUrl().getUsers().getChain(),
                chainId, page, size, search);
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback);
    }

    public void getAllServicesAsync(AsyncRestCallback<List<ServiceDto>> callback) {
        String uri = menuProperties.getUrl().getService();
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback);
    }

    private static String getTokenString() {
        String token = VaadinSessionUtil.getAttribute(VaadinSessionUtil.JWT_TOKEN, String.class);
        if (ObjectUtils.isNotEmpty(token)) {
            return BEARER.concat(token);
        }
        return token;
    }

}
