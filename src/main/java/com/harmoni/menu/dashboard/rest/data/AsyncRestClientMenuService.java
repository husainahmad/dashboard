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

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

    public interface AsyncRestCallback<T> {
        void operationFinished(T result);
    }

    private <T> void makeAsyncRequest(String uri, TypeReference<T> typeReference,
                                      AsyncRestClientMenuService.AsyncRestCallback<T> callback) {
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

    public void getAllCategoryAsync(AsyncRestCallback<List<CategoryDto>> callback, Integer brandId) {
        String url = MenuProperties.CATEGORY.formatted(menuProperties.getUrl().getCategories().getBrand(), brandId);
        makeAsyncRequest(url, new TypeReference<>() {
        }, callback);
    }

    public void getAllProductAsync(AsyncRestCallback<List<ProductDto>> callback, Integer categoryId) {
        String url = MenuProperties.CATEGORY.formatted(menuProperties.getUrl().getProducts().getCategory(), categoryId);
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

    public void getAllSkuAsync(AsyncRestCallback<List<SkuDto>> callback) {
        String url = menuProperties.getUrl().getSku();
        makeAsyncRequest(url, new TypeReference<>() {
        }, callback);
    }

    public void getDetailCategoryAsync(AsyncRestCallback<CategoryDto> callback, Long id) {
        String url = MenuProperties.CATEGORY.formatted(menuProperties.getUrl().getCategory(), id);
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
}
