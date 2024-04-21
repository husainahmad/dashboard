package com.harmoni.menu.dashboard.rest.data;

import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.exception.BusinessBadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.Serializable;

@Service
public class RestClientOrganizationService implements Serializable {

    private final static Logger log = LoggerFactory.getLogger(RestClientOrganizationService.class);

    @Value("${menu.url.chain}")
    private String urlChain;

    @Value("${menu.url.brand}")
    private String urlBrand;

    @Value("${menu.url.tier}")
    private String urlTier;

    @Value("${menu.url.store}")
    private String urlStore;

    public Mono<RestAPIResponse> createChain(ChainDto chainDto) {

        WebClient webClient = WebClient.builder().build();

        return
                webClient.post()
                .uri(urlChain)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(chainDto), ChainDto.class)
                .retrieve()
                .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .handle(((restAPIResponse, throwableSynchronousSink) -> {
                                    log.error("BAD_REQUEST Server Response {}", restAPIResponse.getHttpStatus());
                                    throwableSynchronousSink.error(new BusinessBadRequestException(restAPIResponse));
                                }))
                )
                .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .handle(((restAPIResponse, throwableSynchronousSink) -> {
                                    log.error("INTERNAL_SERVER_ERROR Server Response {}", restAPIResponse.getHttpStatus());
                                    throwableSynchronousSink.error(new BusinessBadRequestException(restAPIResponse));
                                }))
                )
                .bodyToMono(RestAPIResponse.class);

    }

    public Mono<RestAPIResponse> updateChain(ChainDto chainDto) {

        WebClient webClient = WebClient.builder().build();

        return
                webClient.put()
                .uri("%s/%d".formatted(urlChain, chainDto.getId()))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(chainDto), ChainDto.class)
                .retrieve()
                .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .handle(((restAPIResponse, throwableSynchronousSink) -> {
                                    log.error("BAD_REQUEST Server Response {}", restAPIResponse.getHttpStatus());
                                    throwableSynchronousSink.error(new BusinessBadRequestException(restAPIResponse));
                                }))
                )
                .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .handle(((restAPIResponse, throwableSynchronousSink) -> {
                                    log.error("INTERNAL_SERVER_ERROR Server Response {}", restAPIResponse.getHttpStatus());
                                    throwableSynchronousSink.error(new BusinessBadRequestException(restAPIResponse));
                                }))
                )
                .bodyToMono(RestAPIResponse.class);

    }

    public Mono<RestAPIResponse> createBrand(BrandDto brandDto) {

        WebClient webClient = WebClient.builder().build();

        return
                webClient.post()
                .uri(urlBrand)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(brandDto), BrandDto.class)
                .retrieve()
                .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .handle(((restAPIResponse, throwableSynchronousSink) -> {
                                    log.error("BAD_REQUEST Server Response {}", restAPIResponse.getHttpStatus());
                                    throwableSynchronousSink.error(new BusinessBadRequestException(restAPIResponse));
                                }))
                )
                .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .handle(((restAPIResponse, throwableSynchronousSink) -> {
                                    log.error("INTERNAL_SERVER_ERROR Server Response {}", restAPIResponse.getHttpStatus());
                                    throwableSynchronousSink.error(new BusinessBadRequestException(restAPIResponse));
                                }))
                )
                .bodyToMono(RestAPIResponse.class);

    }

    public Mono<RestAPIResponse> updateBrand(BrandDto brandDto) {

        WebClient webClient = WebClient.builder().build();

        return
                webClient.put()
                .uri("%s/%d".formatted(urlChain, brandDto.getId()))
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(brandDto), BrandDto.class)
                .retrieve()
                .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .handle(((restAPIResponse, throwableSynchronousSink) -> {
                                    log.error("BAD_REQUEST Server Response {}", restAPIResponse.getHttpStatus());
                                    throwableSynchronousSink.error(new BusinessBadRequestException(restAPIResponse));
                                }))
                )
                .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .handle(((restAPIResponse, throwableSynchronousSink) -> {
                                    log.error("INTERNAL_SERVER_ERROR Server Response {}", restAPIResponse.getHttpStatus());
                                    throwableSynchronousSink.error(new BusinessBadRequestException(restAPIResponse));
                                }))
                )
                .bodyToMono(RestAPIResponse.class);

    }

    public Mono<RestAPIResponse> createTier(TierDto tierDto) {

        WebClient webClient = WebClient.builder().build();

        return
                webClient.post()
                .uri(urlTier)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(tierDto), TierDto.class)
                .retrieve()
                .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .handle(((restAPIResponse, throwableSynchronousSink) -> {
                                    log.error("BAD_REQUEST Server Response {}", restAPIResponse.getHttpStatus());
                                    throwableSynchronousSink.error(new BusinessBadRequestException(restAPIResponse));
                                }))
                )
                .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .handle(((restAPIResponse, throwableSynchronousSink) -> {
                                    log.error("INTERNAL_SERVER_ERROR Server Response {}", restAPIResponse.getHttpStatus());
                                    throwableSynchronousSink.error(new BusinessBadRequestException(restAPIResponse));
                                }))
                )
                .bodyToMono(RestAPIResponse.class);

    }

    public Mono<RestAPIResponse> createStore(StoreDto storeDto) {
        WebClient webClient = WebClient.builder().build();

        return
                webClient.post()
                .uri(urlStore)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(Mono.just(storeDto), StoreDto.class)
                .retrieve()
                .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .handle(((restAPIResponse, throwableSynchronousSink) -> {
                            log.error("BAD_REQUEST Server Response {}", restAPIResponse.getHttpStatus());
                            throwableSynchronousSink.error(new BusinessBadRequestException(restAPIResponse));
                        }))
                )
                .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .handle(((restAPIResponse, throwableSynchronousSink) -> {
                                    log.error("INTERNAL_SERVER_ERROR Server Response {}", restAPIResponse.getHttpStatus());
                                    throwableSynchronousSink.error(new BusinessBadRequestException(restAPIResponse));
                                }))
                )
                .bodyToMono(RestAPIResponse.class);
    }
}
