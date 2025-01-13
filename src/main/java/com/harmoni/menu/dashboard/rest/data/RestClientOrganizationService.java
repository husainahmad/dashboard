package com.harmoni.menu.dashboard.rest.data;

import com.harmoni.menu.dashboard.configuration.MenuProperties;
import com.harmoni.menu.dashboard.dto.*;
import com.harmoni.menu.dashboard.exception.BusinessBadRequestException;
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
import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class RestClientOrganizationService implements Serializable {

    private final MenuProperties menuProperties;
    private static final String LOG_BAD_REQUEST = "BAD_REQUEST Server Response {}";
    private static final String LOG_INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR Server Response {}";
    private static final String URL_FORMAT = "%s/%d";

    public Mono<RestAPIResponse> createChain(ChainDto chainDto) {
        return create(menuProperties.getUrl().getChain(), Mono.just(chainDto), ChainDto.class);
    }

    public Mono<RestAPIResponse> updateChain(ChainDto chainDto) {
        return update(URL_FORMAT.formatted(menuProperties.getUrl().getChain(), chainDto.getId()),
                Mono.just(chainDto), ChainDto.class);
    }

    public Mono<RestAPIResponse> createBrand(BrandDto brandDto) {
        return create(menuProperties.getUrl().getBrand(), Mono.just(brandDto), BrandDto.class);
    }

    public Mono<RestAPIResponse> updateBrand(BrandDto brandDto) {
        return update(URL_FORMAT.formatted(menuProperties.getUrl().getChain(), brandDto.getId()),
                Mono.just(brandDto), BrandDto.class);
    }

    public Mono<RestAPIResponse> deleteBrand(BrandDto brandDto) {
        return delete(URL_FORMAT.formatted(menuProperties.getUrl().getBrand(), brandDto.getId()));
    }

    public Mono<RestAPIResponse> createTier(TierDto tierDto) {
        return create(menuProperties.getUrl().getTier(), Mono.just(tierDto), TierDto.class);
    }

    public Mono<RestAPIResponse> updateTier(TierDto tierDto) {
        return update(URL_FORMAT.formatted(menuProperties.getUrl().getTier(), tierDto.getId()),
                Mono.just(tierDto), TierDto.class);
    }

    public Mono<RestAPIResponse> updateTierService(TierDto tierDto, List<TierSubServiceDto> tierServiceDtos) {
        return update(String.format(menuProperties.getUrl().getTiers().getServices().getUpdate(), tierDto.getId()),
                Mono.just(tierServiceDtos), List.class);
    }

    public Mono<RestAPIResponse> updateTierMenu(TierDto tierDto, List<TierMenuDto> tierMenuDtos) {
        return update(String.format(menuProperties.getUrl().getTiers().getMenus().getUpdate(), tierDto.getId()),
                Mono.just(tierMenuDtos), List.class);
    }

    public Mono<RestAPIResponse> deleteTier(TierDto tierDto) {
        return delete(URL_FORMAT.formatted(menuProperties.getUrl().getTier(), tierDto.getId()));
    }

    public Mono<RestAPIResponse> deleteChain(ChainDto chainDto) {
        return delete(URL_FORMAT.formatted(menuProperties.getUrl().getChain(), chainDto.getId()));
    }

    public Mono<RestAPIResponse> createStore(StoreDto storeDto) {
        return create(menuProperties.getUrl().getStore(), Mono.just(storeDto), StoreDto.class);
    }

    public Mono<RestAPIResponse> updateStore(StoreDto storeDto) {
        return update(URL_FORMAT.formatted(menuProperties.getUrl().getStore(), storeDto.getId()),
                Mono.just(storeDto), StoreDto.class);
    }

    public Mono<RestAPIResponse> deleteStore(StoreDto storeDto) {
        return delete(URL_FORMAT.formatted(menuProperties.getUrl().getStore(), storeDto.getId()));
    }

    public Mono<RestAPIResponse> create(String url, Publisher<?> publisher, Class<?> className) {
        WebClient webClient = WebClient.builder().build();

        return
                webClient.post()
                        .uri(url)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(publisher, className)
                        .retrieve()
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),
                                RestClientOrganizationService::applyOnBadRequest
                        )
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),
                                RestClientOrganizationService::applyOnError
                        )
                        .bodyToMono(RestAPIResponse.class);
    }

    public Mono<RestAPIResponse> update(String url, Publisher<?> publisher, Class<?> className) {

        WebClient webClient = WebClient.builder().build();

        return
                webClient.put()
                        .uri(url)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .body(publisher, className)
                        .retrieve()
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),
                                RestClientOrganizationService::applyOnBadRequest
                        )
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),
                                RestClientOrganizationService::applyOnError
                        )
                        .bodyToMono(RestAPIResponse.class);

    }

    private static Mono<RestAPIResponse> delete(String url) {
        WebClient webClient = WebClient.builder().build();

        return
                webClient.delete()
                        .uri(url)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .retrieve()
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),
                                RestClientOrganizationService::applyOnBadRequest
                        )
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),
                                RestClientOrganizationService::applyOnError
                        )
                        .bodyToMono(RestAPIResponse.class);
    }

    private static void logError(String s, RestAPIResponse restAPIResponse) {
        log.error(s, restAPIResponse.getHttpStatus());
    }

    private static Mono<? extends Throwable> applyOnError(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(RestAPIResponse.class)
                .handle(((restAPIResponse, throwableSynchronousSink) -> {
                    logError(LOG_INTERNAL_SERVER_ERROR, restAPIResponse);
                    throwableSynchronousSink.error(new BusinessBadRequestException(restAPIResponse));
                }));
    }

    private static Mono<? extends Throwable> applyOnBadRequest(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(RestAPIResponse.class)
                .handle(((restAPIResponse, throwableSynchronousSink) -> {
                    logError(LOG_BAD_REQUEST, restAPIResponse);
                    throwableSynchronousSink.error(new BusinessBadRequestException(restAPIResponse));
                }));
    }
}
