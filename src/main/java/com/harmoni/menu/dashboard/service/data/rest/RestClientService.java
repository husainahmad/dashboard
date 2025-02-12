package com.harmoni.menu.dashboard.service.data.rest;

import com.harmoni.menu.dashboard.exception.BusinessBadRequestException;
import com.harmoni.menu.dashboard.exception.UnAuthorizedServerRequestException;
import com.harmoni.menu.dashboard.util.VaadinSessionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.reactivestreams.Publisher;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.io.File;
import java.io.Serializable;

@RequiredArgsConstructor
@Service
@Slf4j
public class RestClientService implements Serializable {

    private static final String LOG_BAD_REQUEST = "BAD_REQUEST Server Response {}";
    private static final String LOG_NO_CONTENT = "LOG_NO_CONTENT Server Response {}";
    private static final String LOG_UN_AUTHORIZED = "LOG_UN_AUTHORIZED Server Response {}";
    private static final String LOG_INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR Server Response {}";
    private static final String BEARER = "Bearer ";
    private static final WebClient webClient = WebClient.builder().build();

    public Mono<RestAPIResponse> post(String url, Publisher<?> publisher, Class<?> className) {
        return
                webClient.post()
                        .uri(url)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, getTokenString())
                        .body(publisher, className)
                        .retrieve()
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.NO_CONTENT),this::handleNoContent)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),this::handleBadRequest)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),this::handleInternalServerError)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.UNAUTHORIZED), this::handleUnAuthorized)
                        .bodyToMono(RestAPIResponse.class);
    }

    private static String getTokenString() {
        String token = VaadinSessionUtil.getAttribute(VaadinSessionUtil.JWT_TOKEN, String.class);
        if (ObjectUtils.isNotEmpty(token)) {
            return BEARER.concat(token);
        }
        return token;
    }

    public Mono<RestAPIResponse> get(String url) {
        WebClient.ResponseSpec retrieve = webClient.get()
                .uri(url)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .header(HttpHeaders.AUTHORIZATION, getTokenString())
                .retrieve();
        retrieve.onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.NO_CONTENT),this::handleNoContent);
        retrieve.onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),this::handleBadRequest);
        retrieve.onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),this::handleInternalServerError);
        retrieve.onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.UNAUTHORIZED), this::handleUnAuthorized);
        return retrieve.bodyToMono(RestAPIResponse.class);
    }

    public Mono<RestAPIResponse> put(String url, Publisher<?> publisher, Class<?> className) {
        return
                webClient.put()
                        .uri(url)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, getTokenString())
                        .body(publisher, className)
                        .retrieve()
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.NO_CONTENT),this::handleNoContent)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),this::handleBadRequest)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),this::handleInternalServerError)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.UNAUTHORIZED), this::handleUnAuthorized)
                        .bodyToMono(RestAPIResponse.class);
    }

    public Mono<RestAPIResponse> upload(String url, File file) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file)); // "file" should match the expected request parameter name
        return
                webClient.post()
                        .uri(url)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, getTokenString())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(BodyInserters.fromMultipartData(body))
                        .retrieve()
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.NO_CONTENT),this::handleNoContent)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),this::handleBadRequest)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),this::handleInternalServerError)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.UNAUTHORIZED), this::handleUnAuthorized)
                        .bodyToMono(RestAPIResponse.class);

    }

    public Mono<RestAPIResponse> uploadUpdate(String url, File file) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file)); // "file" should match the expected request parameter name
        return
                webClient.put()
                        .uri(url)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, getTokenString())
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(BodyInserters.fromMultipartData(body))
                        .retrieve()
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.NO_CONTENT),this::handleNoContent)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),this::handleBadRequest)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),this::handleInternalServerError)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.UNAUTHORIZED), this::handleUnAuthorized)
                        .bodyToMono(RestAPIResponse.class);

    }

    public Mono<RestAPIResponse> delete(String url) {
        return
                webClient.delete()
                        .uri(url)
                        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, getTokenString())
                        .retrieve()
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.NO_CONTENT),this::handleNoContent)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),this::handleBadRequest)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),this::handleInternalServerError)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.UNAUTHORIZED), this::handleUnAuthorized)
                        .bodyToMono(RestAPIResponse.class);
    }

    private static void logError(String s, RestAPIResponse restAPIResponse) {
        log.error(s, restAPIResponse.getHttpStatus());
    }

    private Mono<? extends Throwable> handleBadRequest(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(RestAPIResponse.class)
                .handle(((restAPIResponse, throwableSynchronousSink) -> {
                    logError(LOG_BAD_REQUEST, restAPIResponse);
                    throwableSynchronousSink.error(new BusinessBadRequestException(restAPIResponse));
                }));
    }

    private Mono<? extends Throwable> handleNoContent(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(RestAPIResponse.class)
                .handle(((restAPIResponse, throwableSynchronousSink) -> {
                    logError(LOG_NO_CONTENT, restAPIResponse);
                    throwableSynchronousSink.error(new BusinessBadRequestException(restAPIResponse));
                }));
    }

    private Mono<? extends Throwable> handleUnAuthorized(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(RestAPIResponse.class)
                .handle(((restAPIResponse, throwableSynchronousSink) -> {
                    logError(LOG_UN_AUTHORIZED, restAPIResponse);
                    throwableSynchronousSink.error(new UnAuthorizedServerRequestException(restAPIResponse));
                }));
    }

    private Mono<? extends Throwable> handleInternalServerError(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(RestAPIResponse.class)
                .handle(((restAPIResponse, throwableSynchronousSink) -> {
                    logError(LOG_INTERNAL_SERVER_ERROR, restAPIResponse);
                    throwableSynchronousSink.error(new BusinessBadRequestException(restAPIResponse));
                }));
    }

}
