package com.harmoni.menu.dashboard.rest.data;

import com.harmoni.menu.dashboard.configuration.MenuProperties;
import com.harmoni.menu.dashboard.dto.*;
import com.harmoni.menu.dashboard.exception.BusinessBadRequestException;
import com.harmoni.menu.dashboard.layout.menu.product.ProductFormDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    public Mono<RestAPIResponse> createCategory(CategoryDto categoryDto) {

        WebClient webClient = WebClient.builder().build();

        return
                webClient.post()
                .uri(urlMenuProperties.getUrl().getCategory())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(categoryDto), CategoryDto.class)
                .retrieve()
                    .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.NO_CONTENT),this::handleNoContent)
                    .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),this::handleBadRequest)
                    .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),this::handleInternalServerError)
                .bodyToMono(RestAPIResponse.class);

    }

    public Mono<RestAPIResponse> getAllCategoryByBrand(Integer brandId) {
        WebClient webClient = WebClient.builder().build();
        return
                webClient.get()
                        .uri("%s/%d".formatted(urlMenuProperties.getUrl().getCategory(), brandId))
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .retrieve()
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.NO_CONTENT),this::handleNoContent)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),this::handleBadRequest)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),this::handleInternalServerError)
                        .bodyToMono(RestAPIResponse.class);
    }

    public Mono<RestAPIResponse> getAllTierByBrand(Integer brandId) {
        WebClient webClient = WebClient.builder().build();
        return
                webClient.get()
                        .uri("%s/brand/%d".formatted(urlMenuProperties.getUrl().getTier(), brandId))
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .retrieve()
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.NO_CONTENT),this::handleNoContent)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),this::handleBadRequest)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),this::handleInternalServerError)
                        .bodyToMono(RestAPIResponse.class);
    }

    public Mono<RestAPIResponse> getAllBrand() {
        WebClient webClient = WebClient.builder().build();
        WebClient.ResponseSpec retrieve = webClient.get()
                .uri(urlMenuProperties.getUrl().getBrand())
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .retrieve();
        retrieve.onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.NO_CONTENT),this::handleNoContent);
        retrieve.onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),this::handleBadRequest);
        retrieve.onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),this::handleInternalServerError);
        return retrieve.bodyToMono(RestAPIResponse.class);
    }

    public Mono<RestAPIResponse> saveProductBulk(ProductFormDto productFormDto) {

        WebClient webClient = WebClient.builder().build();

        return
            webClient.put()
                .uri(urlMenuProperties.getUrl().getProducts().getBulk().formatted(productFormDto.getId()))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(productFormDto), ProductFormDto.class)
                .retrieve()
                    .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.NO_CONTENT),this::handleNoContent)
                    .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),this::handleBadRequest)
                    .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),this::handleInternalServerError)
                .bodyToMono(RestAPIResponse.class);

    }

    public Mono<RestAPIResponse> deleteSku(SkuDto skuDto) {

        WebClient webClient = WebClient.builder().build();

        return
            webClient.delete()
                .uri("%s/%d".formatted(urlMenuProperties.getUrl().getSku(), skuDto.getId()))
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
