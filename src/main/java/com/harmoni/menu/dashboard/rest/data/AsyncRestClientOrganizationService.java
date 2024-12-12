package com.harmoni.menu.dashboard.rest.data;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harmoni.menu.dashboard.configuration.MenuProperties;
import com.harmoni.menu.dashboard.dto.*;
import com.harmoni.menu.dashboard.exception.BusinessBadRequestException;
import com.harmoni.menu.dashboard.exception.BusinessServerRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Service
@Slf4j
public class AsyncRestClientOrganizationService implements Serializable {

    private final transient MenuProperties menuProperties;
    private final transient WebClient webClient = WebClient.builder().build();

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

    public interface AsyncRestCallback<T> {
        void operationFinished(T result);
    }

    private <T> void makeAsyncRequest(String uri, TypeReference<T> typeReference, AsyncRestCallback<T> callback) {
        WebClient.ResponseSpec responseSpec = webClient.get()
                .uri(uri)
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

    public void getDetailChainAsync(AsyncRestCallback<ChainDto> callback, Long id) {
        String uri = MenuProperties.CATEGORY.formatted(menuProperties.getUrl().getChain(), id);
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

    public void getDetailTierAsync(AsyncRestCallback<TierDto> callback, Long id) {
        String uri = MenuProperties.CATEGORY.formatted(menuProperties.getUrl().getTier(), id);
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback);
    }

    public void getAllTierAsync(AsyncRestCallback<List<TierDto>> callback) {
        String uri = menuProperties.getUrl().getTier();
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback);
    }

    public void getAllTierByBrandAsync(AsyncRestCallback<List<TierDto>> callback, Integer id, TierTypeDto tierTypeDto) {
        String uri = String.format("%s/brand/%d/type/%s", menuProperties.getUrl().getTier(), id, tierTypeDto);
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback);
    }

    public void getAllStoreAsync(AsyncRestCallback<List<StoreDto>> callback) {
        String uri = menuProperties.getUrl().getStore();
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback);
    }

    public void getAllCategoryAsync(AsyncRestCallback<List<CategoryDto>> callback) {
        String uri = menuProperties.getUrl().getCategory();
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback);
    }

    public void getAllServicesAsync(AsyncRestCallback<List<ServiceDto>> callback) {
        String uri = menuProperties.getUrl().getService();
        makeAsyncRequest(uri, new TypeReference<>() {
        }, callback);
    }

}
