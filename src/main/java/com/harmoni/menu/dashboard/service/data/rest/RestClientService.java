package com.harmoni.menu.dashboard.service.data.rest;

import com.harmoni.menu.dashboard.exception.BusinessBadRequestException;
import com.harmoni.menu.dashboard.exception.TokenRefreshRequiredException;
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

/**
 * Base blocking REST client for the POSHarmoni backend, built on Spring
 * {@link WebClient}. Provides post / get / put / multipart upload / delete
 * primitives that inject the JWT bearer token, transparently refresh it on a
 * 401 via {@link TokenRefreshService} and map error statuses to typed
 * exceptions. Every call returns a reactive {@link Mono} subscribed by the
 * caller.
 */
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

    /**
     * POSTs a payload and expects the standard {@link RestAPIResponse} body.
     *
     * @param url       the target endpoint
     * @param publisher the reactive body to publish
     * @param className the body element class
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> post(String url, Publisher<?> publisher, Class<?> className) {
        return post(url, publisher, className, RestAPIResponse.class);
    }

    /**
     * POSTs a payload and decodes the response into the given class.
     *
     * @param url           the target endpoint
     * @param publisher     the reactive body to publish
     * @param bodyClass     the body element class
     * @param responseClass the expected response type
     * @param <T>           the response type
     * @return a {@link Mono} with the decoded response
     */
    public <T> Mono<T> post(String url, Publisher<?> publisher, Class<?> bodyClass, Class<T> responseClass) {
        log.debug("POST {} auth={}", url, ObjectUtils.isNotEmpty(getTokenString()));
        return TokenRefreshService.getInstance().withTokenRefresh(accessToken ->
                webClient.post()
                        .uri(url)
                        .headers(headers -> applyDefaultHeaders(headers, resolveToken(accessToken, null)))
                        .body(publisher, bodyClass)
                        .retrieve()
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.NO_CONTENT),this::handleNoContent)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),this::handleBadRequest)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),this::handleInternalServerError)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.UNAUTHORIZED), this::handleUnAuthorized)
                        .bodyToMono(responseClass));
    }

    private static String getTokenString() {
        return getTokenString(VaadinSessionUtil.getAttribute(VaadinSessionUtil.JWT_TOKEN, String.class));
    }

    private static String getTokenString(String token) {
        return ObjectUtils.isNotEmpty(token) ? BEARER.concat(token) : token;
    }

    private static String sessionToken() {
        return VaadinSessionUtil.getAttribute(VaadinSessionUtil.JWT_TOKEN, String.class);
    }

    private static String resolveToken(String accessToken, String explicitToken) {
        if (ObjectUtils.isNotEmpty(accessToken)) {
            return accessToken;
        }
        return ObjectUtils.isNotEmpty(explicitToken) ? explicitToken : sessionToken();
    }

    private static void applyDefaultHeaders(HttpHeaders httpHeaders, String token) {
        httpHeaders.set(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
        if (ObjectUtils.isNotEmpty(token)) {
            httpHeaders.set(HttpHeaders.AUTHORIZATION, BEARER.concat(token));
        }
    }

    /**
     * GETs a resource using the default session token.
     *
     * @param url the target endpoint
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> get(String url) {
        return get(url, null);
    }

    /**
     * GETs a resource with an explicit bearer token, falling back to the
     * session token when {@code token} is {@code null}.
     *
     * @param url   the target endpoint
     * @param token the token to use, or {@code null} for the session token
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> get(String url, String token) {
        log.debug("GET {} auth={}", url, ObjectUtils.isNotEmpty(getTokenString(token)));
        return TokenRefreshService.getInstance().withTokenRefresh(accessToken ->
                webClient.get()
                        .uri(url)
                        .headers(headers -> applyDefaultHeaders(headers, resolveToken(accessToken, token)))
                        .retrieve()
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.NO_CONTENT),this::handleNoContent)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),this::handleBadRequest)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),this::handleInternalServerError)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.UNAUTHORIZED), this::handleUnAuthorized)
                        .bodyToMono(RestAPIResponse.class));
    }

    /**
     * PUTs a payload, replacing the resource at the URL.
     *
     * @param url       the target endpoint
     * @param publisher the reactive body to publish
     * @param className the body element class
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> put(String url, Publisher<?> publisher, Class<?> className) {
        return TokenRefreshService.getInstance().withTokenRefresh(accessToken ->
                webClient.put()
                        .uri(url)
                        .headers(headers -> applyDefaultHeaders(headers, resolveToken(accessToken, null)))
                        .body(publisher, className)
                        .retrieve()
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.NO_CONTENT),this::handleNoContent)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),this::handleBadRequest)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),this::handleInternalServerError)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.UNAUTHORIZED), this::handleUnAuthorized)
                        .bodyToMono(RestAPIResponse.class));
    }

    /**
     * Uploads a file with a multipart POST.
     *
     * @param url  the upload endpoint
     * @param file the file to attach
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> upload(String url, File file) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file)); // "file" should match the expected request parameter name
        return TokenRefreshService.getInstance().withTokenRefresh(accessToken ->
                webClient.post()
                        .uri(url)
                        .headers(headers -> applyDefaultHeaders(headers, resolveToken(accessToken, null)))
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(BodyInserters.fromMultipartData(body))
                        .retrieve()
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.NO_CONTENT),this::handleNoContent)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),this::handleBadRequest)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),this::handleInternalServerError)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.UNAUTHORIZED), this::handleUnAuthorized)
                        .bodyToMono(RestAPIResponse.class));

    }

    /**
     * Uploads a replacement file with a multipart PUT.
     *
     * @param url  the update endpoint
     * @param file the file to attach
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> uploadUpdate(String url, File file) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file)); // "file" should match the expected request parameter name
        return TokenRefreshService.getInstance().withTokenRefresh(accessToken ->
                webClient.put()
                        .uri(url)
                        .headers(headers -> applyDefaultHeaders(headers, resolveToken(accessToken, null)))
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(BodyInserters.fromMultipartData(body))
                        .retrieve()
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.NO_CONTENT),this::handleNoContent)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),this::handleBadRequest)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),this::handleInternalServerError)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.UNAUTHORIZED), this::handleUnAuthorized)
                        .bodyToMono(RestAPIResponse.class));

    }

    /**
     * DELETEs the resource at the URL.
     *
     * @param url the target endpoint
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> delete(String url) {
        return TokenRefreshService.getInstance().withTokenRefresh(accessToken ->
                webClient.delete()
                        .uri(url)
                        .headers(headers -> applyDefaultHeaders(headers, resolveToken(accessToken, null)))
                        .retrieve()
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.NO_CONTENT),this::handleNoContent)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.BAD_REQUEST),this::handleBadRequest)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.INTERNAL_SERVER_ERROR),this::handleInternalServerError)
                        .onStatus(httpStatusCode -> httpStatusCode.equals(HttpStatus.UNAUTHORIZED), this::handleUnAuthorized)
                        .bodyToMono(RestAPIResponse.class));
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
                    throwableSynchronousSink.error(new TokenRefreshRequiredException(restAPIResponse.toString()));
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
