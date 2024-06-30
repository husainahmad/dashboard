package com.harmoni.menu.dashboard.rest.data;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harmoni.menu.dashboard.configuration.MenuProperties;
import com.harmoni.menu.dashboard.dto.*;
import com.harmoni.menu.dashboard.exception.BusinessBadRequestException;
import com.harmoni.menu.dashboard.exception.BusinessServerRequestException;
import com.vaadin.flow.component.UI;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient.RequestHeadersSpec;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;

@RequiredArgsConstructor
@Service
@Slf4j
public class AsyncRestClientOrganizationService implements Serializable {

    private final MenuProperties menuProperties;

    private UI ui;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

    public static interface AsyncRestCallback<T> {
        void operationFinished(T result);
    }

    public void getAllChainAsync(AsyncRestCallback<List<ChainDto>> callback) {
        RequestHeadersSpec<?> spec = WebClient.create().get().uri(menuProperties.getUrl().getChain());

        spec.retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessBadRequestException::new))
                .onStatus(HttpStatus.INTERNAL_SERVER_ERROR::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessServerRequestException::new))
                .toEntity(RestAPIResponse.class).subscribe(result -> {
            final List<ChainDto> chainDtos = objectMapper.convertValue(
                    Objects.requireNonNull(result.getBody()).getData(),
                    new TypeReference<>() {
                    });

            callback.operationFinished(chainDtos);
        });
    }

    public void getDetailChainAsync(AsyncRestCallback<ChainDto> callback, Long id) {
        RequestHeadersSpec<?> spec = WebClient.create().get()
                .uri("%s/%d".formatted(menuProperties.getUrl().getChain(), id));

        spec.retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessBadRequestException::new))
                .onStatus(HttpStatus.INTERNAL_SERVER_ERROR::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessServerRequestException::new))
                .toEntity(RestAPIResponse.class).subscribe(result -> {
            final ChainDto chainDto = objectMapper.convertValue(
                    Objects.requireNonNull(result.getBody()).getData(),
                    new TypeReference<>() {
                    });

            callback.operationFinished(chainDto);
        });
    }

    public void getAllBrandAsync(AsyncRestCallback<List<BrandDto>> callback) {
        RequestHeadersSpec<?> spec = WebClient.create().get().uri(menuProperties.getUrl().getBrand());

        spec.retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessBadRequestException::new))
                .onStatus(HttpStatus.INTERNAL_SERVER_ERROR::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessServerRequestException::new))
                .toEntity(RestAPIResponse.class).subscribe(result -> {
            final List<BrandDto> brandDtos = objectMapper.convertValue(
                    Objects.requireNonNull(result.getBody()).getData(),
                    new TypeReference<>() {
                    });

            callback.operationFinished(brandDtos);
        });
    }

    public void getDetailBrandAsync(AsyncRestCallback<BrandDto> callback, Long id) {
        RequestHeadersSpec<?> spec = WebClient.create().get()
                .uri("%s/%d".formatted(menuProperties.getUrl().getBrand(), id));

        spec.retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessBadRequestException::new))
                .onStatus(HttpStatus.INTERNAL_SERVER_ERROR::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessServerRequestException::new))
                .toEntity(RestAPIResponse.class).subscribe(result -> {
            final BrandDto brandDto = objectMapper.convertValue(
                    Objects.requireNonNull(result.getBody()).getData(),
                    new TypeReference<>() {
                    });

            callback.operationFinished(brandDto);
        });
    }

    public void getDetailTierAsync(AsyncRestCallback<TierDto> callback, Long id) {
        RequestHeadersSpec<?> spec = WebClient.create().get()
                .uri("%s/%d".formatted(menuProperties.getUrl().getTier(), id));

        spec.retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessBadRequestException::new))
                .onStatus(HttpStatus.INTERNAL_SERVER_ERROR::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessServerRequestException::new))
                .toEntity(RestAPIResponse.class).subscribe(result -> {
            final TierDto tierDto = objectMapper.convertValue(
                    Objects.requireNonNull(result.getBody()).getData(),
                    new TypeReference<>() {
                    });

            callback.operationFinished(tierDto);
        });
    }

    public void getAllTierAsync(AsyncRestCallback<List<TierDto>> callback) {
        RequestHeadersSpec<?> spec = WebClient.create()
                .get().uri(menuProperties.getUrl().getTier());

        spec.retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessBadRequestException::new))
                .onStatus(HttpStatus.INTERNAL_SERVER_ERROR::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessServerRequestException::new))
                .toEntity(RestAPIResponse.class).subscribe(result -> {
            final List<TierDto> brandDtos = objectMapper.convertValue(
                    Objects.requireNonNull(result.getBody()).getData(),
                    new TypeReference<>() {
                    });

            callback.operationFinished(brandDtos);
        });
    }

    public void getAllTierByBrandAsync(AsyncRestCallback<List<TierDto>> callback, Integer id) {
        RequestHeadersSpec<?> spec = WebClient.create().get()
                .uri("%s/brand/%d".formatted(menuProperties.getUrl().getTier(), id));

        spec.retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessBadRequestException::new))
                .onStatus(HttpStatus.INTERNAL_SERVER_ERROR::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessServerRequestException::new))
                .toEntity(RestAPIResponse.class).subscribe(result -> {
            final List<TierDto> brandDtos = objectMapper.convertValue(
                    Objects.requireNonNull(result.getBody()).getData(),
                    new TypeReference<>() {
                    });

            callback.operationFinished(brandDtos);
        });
    }

    public void getAllStoreAsync(AsyncRestCallback<List<StoreDto>> callback) {
        RequestHeadersSpec<?> spec = WebClient.create().get().uri(menuProperties.getUrl().getStore());

        spec.retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessBadRequestException::new))
                .onStatus(HttpStatus.INTERNAL_SERVER_ERROR::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessServerRequestException::new))
                .toEntity(RestAPIResponse.class).subscribe(result -> {
            final List<StoreDto> storeDtos = objectMapper.convertValue(
                    Objects.requireNonNull(result.getBody()).getData(),
                    new TypeReference<>() {
                    });

            callback.operationFinished(storeDtos);
        });
    }

    public void getAllCategoryAsync(AsyncRestCallback<List<CategoryDto>> callback) {
        RequestHeadersSpec<?> spec = WebClient.create().get().uri(menuProperties.getUrl().getCategory());

        spec.retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessBadRequestException::new))
                .onStatus(HttpStatus.INTERNAL_SERVER_ERROR::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessServerRequestException::new))
                .toEntity(RestAPIResponse.class).subscribe(result -> {
            final List<CategoryDto> categoryDtos = objectMapper.convertValue(
                    Objects.requireNonNull(result.getBody()).getData(),
                    new TypeReference<>() {
                    });

            callback.operationFinished(categoryDtos);
        });
    }


}
