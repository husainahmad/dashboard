package com.harmoni.menu.dashboard.rest.data;

import com.harmoni.menu.dashboard.configuration.MenuProperties;
import com.harmoni.menu.dashboard.dto.*;
import com.harmoni.menu.dashboard.exception.BusinessBadRequestException;
import com.harmoni.menu.dashboard.layout.menu.product.dto.ProductFormDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.Serializable;

@RequiredArgsConstructor
@Service
@Slf4j
public class RestClientMenuService implements Serializable {

    private final transient MenuProperties urlMenuProperties;
    private final transient WebClient webClient = WebClient.builder().build();

    public Mono<RestAPIResponse> createCategory(CategoryDto categoryDto) {
        return create(urlMenuProperties.getUrl().getCategory(), Mono.just(categoryDto), CategoryDto.class);
    }

    public Mono<RestAPIResponse> getAllCategoryByBrand(Integer brandId) {
        return get("%s/%d".formatted(urlMenuProperties.getUrl().getCategories().getBrand(), brandId));
    }

    public Mono<RestAPIResponse> getAllTierByBrand(Integer brandId, String type) {
        return get("%s/brand/%d/type/%s".formatted(urlMenuProperties.getUrl().getTier(), brandId, type));
    }

    public Mono<RestAPIResponse> getAllBrand() {
        return get(urlMenuProperties.getUrl().getBrand());
    }

    public Mono<RestAPIResponse> saveProductBulk(ProductFormDto productFormDto) {
        return put(urlMenuProperties.getUrl().getProducts().getBulk().formatted(productFormDto.getId()),
                Mono.just(productFormDto),  ProductFormDto.class);
    }

    public Mono<RestAPIResponse> deleteSku(SkuDto skuDto) {
        return delete("%s/%d".formatted(urlMenuProperties.getUrl().getSku(), skuDto.getId()));
    }

    public Mono<RestAPIResponse> deleteCategory(CategoryDto categoryDto) {
        return delete("%s/%d".formatted(urlMenuProperties.getUrl().getCategory(), categoryDto.getId()));
    }

    private Mono<RestAPIResponse> create(String url, Publisher<?> publisher, Class<?> className) {
        return
                webClient.post()
                        .uri(url)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(Mono.just(publisher), className)
                        .retrieve()
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.NO_CONTENT),this::handleNoContent)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),this::handleBadRequest)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),this::handleInternalServerError)
                        .bodyToMono(RestAPIResponse.class);

    }

    private Mono<RestAPIResponse> get(String url) {
        WebClient.ResponseSpec retrieve = webClient.get()
                .uri(url)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve();
        retrieve.onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.NO_CONTENT),this::handleNoContent);
        retrieve.onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),this::handleBadRequest);
        retrieve.onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),this::handleInternalServerError);
        return retrieve.bodyToMono(RestAPIResponse.class);
    }

    private Mono<RestAPIResponse> put(String url, Publisher<?> publisher, Class<?> className) {
        return
                webClient.put()
                        .uri(url)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(Mono.just(publisher), className)
                        .retrieve()
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.NO_CONTENT),this::handleNoContent)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),this::handleBadRequest)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),this::handleInternalServerError)
                        .bodyToMono(RestAPIResponse.class);
    }

    private Mono<RestAPIResponse> delete(String url) {
        return
                webClient.delete()
                        .uri(url)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .retrieve()
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.NO_CONTENT),this::handleNoContent)
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

    private Mono<? extends Throwable> handleNoContent(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(RestAPIResponse.class)
                .handle(((restAPIResponse, throwableSynchronousSink) -> {
                    log.error("NO_CONTENT Server Response {}", restAPIResponse.getHttpStatus());
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
