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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
@Slf4j
public class AsyncRestClientMenuService implements Serializable {

    private final MenuProperties menuProperties;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

    public static interface AsyncRestCallback<T> {
        void operationFinished(T result);
    }

    public void getAllCategoryAsync(AsyncRestCallback<List<CategoryDto>> callback, Integer brandId) {
        RequestHeadersSpec<?> spec = WebClient.create()
                  .get().uri("%s/%d".formatted(menuProperties.getUrl().getCategories().getBrand(), brandId));

        spec.retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessBadRequestException::new))
                .onStatus(HttpStatus.INTERNAL_SERVER_ERROR::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessServerRequestException::new))
                .toEntity(RestAPIResponse.class).subscribe(result -> {
            final List<CategoryDto> categoryDtos = objectMapper.convertValue(Objects.requireNonNull(result.getBody()).getData(),
                    new TypeReference<>() {
                    });

            callback.operationFinished(categoryDtos);
        });
    }

    public void getAllProductAsync(AsyncRestCallback<List<ProductDto>> callback, Integer categoryId) {
        RequestHeadersSpec<?> spec = WebClient.create()
        .get().uri("%s/%d".formatted(menuProperties.getUrl().getProducts().getCategory(), categoryId));
        spec.retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessBadRequestException::new))
                .onStatus(HttpStatus.INTERNAL_SERVER_ERROR::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessServerRequestException::new))
                .toEntity(RestAPIResponse.class).subscribe(result -> {
            final List<ProductDto> productDtos = objectMapper.convertValue(Objects.requireNonNull(result.getBody()).getData(),
                    new TypeReference<>() {
                    });

            callback.operationFinished(productDtos);
        });
    }

    public void getAllProductCategoryBrandAsync(AsyncRestCallback<List<ProductDto>> callback,
                                                Integer categoryId, Integer brandId) {
        RequestHeadersSpec<?> spec = WebClient.create()
                .get().uri("%s/%d/%d".formatted(menuProperties.getUrl().getProducts().getCategory(), categoryId, brandId));
        spec.retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessBadRequestException::new))
                .onStatus(HttpStatus.INTERNAL_SERVER_ERROR::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessServerRequestException::new))
                .toEntity(RestAPIResponse.class).subscribe(result -> {
                    final List<ProductDto> productDtos = objectMapper.convertValue(Objects.requireNonNull(result.getBody()).getData(),
                            new TypeReference<>() {
                            });

                    callback.operationFinished(productDtos);
                });
    }

    public void getAllSkuAsync(AsyncRestCallback<List<SkuDto>> callback) {
        RequestHeadersSpec<?> spec = WebClient.create().get().uri(menuProperties.getUrl().getSku());

        spec.retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessBadRequestException::new))
                .onStatus(HttpStatus.INTERNAL_SERVER_ERROR::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessServerRequestException::new))
                .toEntity(RestAPIResponse.class).subscribe(result -> {
            final List<SkuDto> skuDtos = objectMapper.convertValue(Objects.requireNonNull(result.getBody()).getData(),
                    new TypeReference<>() {
                    });

            callback.operationFinished(skuDtos);
        });
    }

    public void getAllSkuByProductAsync(AsyncRestCallback<List<SkuDto>> callback, Integer productId) {
        RequestHeadersSpec<?> spec = WebClient.create()
                                    .get().uri("%s/%d/sku".formatted(menuProperties.getUrl().getProducts().getSku(), productId));

        spec.retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessBadRequestException::new))
                .onStatus(HttpStatus.INTERNAL_SERVER_ERROR::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessServerRequestException::new))
                .toEntity(RestAPIResponse.class).subscribe(result -> {
                    final List<SkuDto> skuDtos = objectMapper.convertValue(Objects.requireNonNull(result.getBody()).getData(),
                            new TypeReference<>() {
                            });

                    callback.operationFinished(skuDtos);
                });
    }

    public void getDetailCategoryAsync(AsyncRestCallback<CategoryDto> callback, Long id) {
        RequestHeadersSpec<?> spec = WebClient.create().get()
                .uri("%s/%d".formatted(menuProperties.getUrl().getCategory(), id));

            spec.retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessBadRequestException::new))
                .onStatus(HttpStatus.INTERNAL_SERVER_ERROR::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessServerRequestException::new))
                .toEntity(RestAPIResponse.class).subscribe(result -> {
                    final CategoryDto categoryDto = objectMapper.convertValue(
                            Objects.requireNonNull(result.getBody()).getData(),
                            new TypeReference<>() {
                            });

                    callback.operationFinished(categoryDto);
                });
    }

    public void getDetailSkuTierPriceAsync(AsyncRestCallback<List<SkuTierPriceDto>> callback,
                                           List<Integer> skuIds, Integer tierId) {
        RequestHeadersSpec<?> spec = WebClient.create(menuProperties.getUrl().getSkutierprice()).get()
                .uri(uriBuilder -> uriBuilder
                        .queryParam("skuIds", skuIds.stream().map(String::valueOf)
                                .collect(Collectors.joining(",")))
                        .queryParam("tierId", tierId)
                        .build())
                ;

        spec.retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessBadRequestException::new))
                .onStatus(HttpStatus.INTERNAL_SERVER_ERROR::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessServerRequestException::new))
                .toEntity(RestAPIResponse.class).subscribe(result -> {
                    final List<SkuTierPriceDto> skuTierPriceDto = objectMapper.convertValue(
                            Objects.requireNonNull(result.getBody()).getData(),
                            new TypeReference<>() {
                            });

                    callback.operationFinished(skuTierPriceDto);
                });
    }
}
