package com.harmoni.menu.dashboard.rest.data;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harmoni.menu.dashboard.dto.*;
import com.harmoni.menu.dashboard.exception.BusinessBadRequestException;
import com.harmoni.menu.dashboard.layout.menu.product.ProductFormDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.Serializable;

@Service
public class RestClientMenuService implements Serializable {

    private final static Logger log = LoggerFactory.getLogger(RestClientMenuService.class);

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

    @Value("${menu.url.category}")
    private String urlCategory;

    @Value("${menu.url.category.brand}")
    private String urlCategoryBrand;

    @Value("${menu.url.tier}")
    private String urlTier;

    @Value("${menu.url.brand}")
    private String urlBrand;

    @Value("${menu.url.product.bulk}")
    private String urlProductBulk;
    public Mono<RestAPIResponse> createCategory(CategoryDto categoryDto) {

        WebClient webClient = WebClient.builder().build();

        return
                webClient.post()
                .uri(urlCategory)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(categoryDto), CategoryDto.class)
                .retrieve()
                .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),this::handleBadRequest)
                .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),this::handleInternalServerError)
                .bodyToMono(RestAPIResponse.class);

    }

    public Mono<RestAPIResponse> getAllCategoryByBrand(Integer brandId) {
        WebClient webClient = WebClient.builder().build();
        return
                webClient.get()
                        .uri("%s/%d".formatted(urlCategoryBrand, brandId))
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .retrieve()
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),this::handleBadRequest)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),this::handleInternalServerError)
                        .bodyToMono(RestAPIResponse.class);
    }

    public Mono<RestAPIResponse> getAllTierByBrand(Integer brandId) {
        WebClient webClient = WebClient.builder().build();
        return
                webClient.get()
                        .uri("%s/brand/%d".formatted(urlTier, brandId))
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .retrieve()
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),this::handleBadRequest)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),this::handleInternalServerError)
                        .bodyToMono(RestAPIResponse.class);
    }

    public Mono<RestAPIResponse> getAllBrand() {
        WebClient webClient = WebClient.builder().build();
        WebClient.ResponseSpec retrieve = webClient.get()
                .uri(urlBrand)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve();
        retrieve.onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),this::handleBadRequest);
        retrieve.onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),this::handleInternalServerError);
        return retrieve.bodyToMono(RestAPIResponse.class);
    }

    public Mono<RestAPIResponse> saveProductBulk(ProductFormDto productFormDto) {

        WebClient webClient = WebClient.builder().build();

        return
            webClient.put()
                .uri(urlProductBulk.formatted(productFormDto.getId()))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(productFormDto), ProductFormDto.class)
                .retrieve()
                .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),this::handleBadRequest)
                .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),this::handleInternalServerError)
                .bodyToMono(RestAPIResponse.class);

    }

    private Mono<? extends Throwable> handleBadRequest(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(RestAPIResponse.class)
                .handle(((restAPIResponse, throwableSynchronousSink) -> {
                    log.error("BAD_REQUEST Server Response {}", restAPIResponse.getHttpStatus());
                    throwableSynchronousSink.error(new BusinessBadRequestException(restAPIResponse));
                }));
    }

    private Mono<? extends Throwable> handleInternalServerError(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(RestAPIResponse.class)
                .handle(((restAPIResponse, throwableSynchronousSink) -> {
                    log.error("INTERNAL_SERVER_ERROR Server Response {}", restAPIResponse.getHttpStatus());
                    throwableSynchronousSink.error(new BusinessBadRequestException(restAPIResponse));
                }));
    }

}
