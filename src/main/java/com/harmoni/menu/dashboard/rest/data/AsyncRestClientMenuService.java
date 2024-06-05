package com.harmoni.menu.dashboard.rest.data;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harmoni.menu.dashboard.dto.*;
import com.harmoni.menu.dashboard.exception.BusinessBadRequestException;
import com.harmoni.menu.dashboard.exception.BusinessServerRequestException;
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

@Service
@Slf4j
public class AsyncRestClientMenuService implements Serializable {

    @Value("${menu.url.category}")
    private String urlCategory;

    @Value("${menu.url.category.brand}")
    private String urlCategoryBrand;

    @Value("${menu.url.product}")
    private String urlProduct;

    @Value("${menu.url.product.category}")
    private String urlProductCategory;

    @Value("${menu.url.sku}")
    private String urlSKU;

    @Value("${menu.url.product.sku}")
    private String urlProductSKU;

    @Value("${menu.url.skutierprice}")
    private String urlSKUTierPrice;

    @Value("${menu.url}")
    private String urlMenu;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

    public static interface AsyncRestCallback<T> {
        void operationFinished(T result);
    }

    public void getAllCategoryAsync(AsyncRestCallback<List<CategoryDto>> callback, Integer brandId) {
        RequestHeadersSpec<?> spec = WebClient.create()
                  .get().uri("%s/%d".formatted(urlCategoryBrand, brandId));

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
        .get().uri("%s/%d".formatted(urlProductCategory, categoryId));
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
                .get().uri("%s/%d/%d".formatted(urlProductCategory, categoryId, brandId));
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
        RequestHeadersSpec<?> spec = WebClient.create().get().uri(urlSKU);

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
                                    .get().uri("%s/%d/sku".formatted(urlProductSKU, productId));

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
                .uri("%s/%d".formatted(urlCategory, id));

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
        RequestHeadersSpec<?> spec = WebClient.create(urlSKUTierPrice).get()
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
