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

    /** Formats a resource base path plus an id, e.g. {@code "/brands/5"}. */
    protected static final String URL_FORMAT = "%s/%d";

    private final transient TokenRefreshService tokenRefreshService;

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
        return tokenRefreshService.withTokenRefresh(accessToken ->
                toResponse(webClient.post()
                        .uri(url)
                        .headers(headers -> applyDefaultHeaders(headers, resolveToken(accessToken, null)))
                        .body(publisher, bodyClass)
                        .retrieve(), responseClass));
    }

    /**
     * GETs a resource using the default session token.
     *
     * @param url the target endpoint
     * @return a {@link Mono} with the server response
     */
    private static String getTokenString() {
        return getTokenString(VaadinSessionUtil.getAttribute(VaadinSessionUtil.JWT_TOKEN, String.class));
    }

    /**
     * Returns the token string with the "Bearer " prefix if the token is not empty.
     *
     * @param token the token to format
     * @return the formatted token string or null if the token is empty
     */
    private static String getTokenString(String token) {
        return ObjectUtils.isNotEmpty(token) ? BEARER.concat(token) : token;
    }

    /**
     * Retrieves the session token from the Vaadin session.
     *
     * @return the session token or null if not present
     */
    private static String sessionToken() {
        return VaadinSessionUtil.getAttribute(VaadinSessionUtil.JWT_TOKEN, String.class);
    }

    /**
     * Resolves the token to use for the request, preferring the access token if available,
     * then the explicit token, and finally falling back to the session token.
     *
     * @param accessToken   the access token from the token refresh service
     * @param explicitToken  an explicitly provided token
     * @return the resolved token to use for the request
     */
    private static String resolveToken(String accessToken, String explicitToken) {
        if (ObjectUtils.isNotEmpty(accessToken)) {
            return accessToken;
        }
        return ObjectUtils.isNotEmpty(explicitToken) ? explicitToken : sessionToken();
    }

    /**
     * Applies default headers to the request, including Content-Type and Authorization.
     *
     * @param httpHeaders the headers to modify
     * @param token       the token to use for the Authorization header
     */
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
        return tokenRefreshService.withTokenRefresh(accessToken ->
                toResponse(webClient.get()
                        .uri(url)
                        .headers(headers -> applyDefaultHeaders(headers, resolveToken(accessToken, token)))
                        .retrieve(), RestAPIResponse.class));
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
        return tokenRefreshService.withTokenRefresh(accessToken ->
                toResponse(webClient.put()
                        .uri(url)
                        .headers(headers -> applyDefaultHeaders(headers, resolveToken(accessToken, null)))
                        .body(publisher, className)
                        .retrieve(), RestAPIResponse.class));
    }

    /**
     * Uploads a file with a multipart POST.
     *
     * @param url  the upload endpoint
     * @param file the file to attach
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> upload(String url, File file) {
        return tokenRefreshService.withTokenRefresh(accessToken ->
                toResponse(webClient.post()
                        .uri(url)
                        .headers(headers -> applyDefaultHeaders(headers, resolveToken(accessToken, null)))
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(BodyInserters.fromMultipartData(multipartBody(file)))
                        .retrieve(), RestAPIResponse.class));

    }

    /**
     * Uploads a replacement file with a multipart PUT.
     *
     * @param url  the update endpoint
     * @param file the file to attach
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> uploadUpdate(String url, File file) {
        return tokenRefreshService.withTokenRefresh(accessToken ->
                toResponse(webClient.put()
                        .uri(url)
                        .headers(headers -> applyDefaultHeaders(headers, resolveToken(accessToken, null)))
                        .contentType(MediaType.MULTIPART_FORM_DATA)
                        .body(BodyInserters.fromMultipartData(multipartBody(file)))
                        .retrieve(), RestAPIResponse.class));

    }

    /**
     * DELETEs the resource at the URL.
     *
     * @param url the target endpoint
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> delete(String url) {
        return tokenRefreshService.withTokenRefresh(accessToken ->
                toResponse(webClient.delete()
                        .uri(url)
                        .headers(headers -> applyDefaultHeaders(headers, resolveToken(accessToken, null)))
                        .retrieve(), RestAPIResponse.class));
    }

    /**
     * PATCHes a resource that carries its parameters in the query string, such as
     * a lifecycle status transition. The body is empty, so no publisher is sent.
     *
     * @param url the target endpoint including its query string
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> patch(String url) {
        log.debug("PATCH {} auth={}", url, ObjectUtils.isNotEmpty(getTokenString()));
        return tokenRefreshService.withTokenRefresh(accessToken ->
                toResponse(webClient.patch()
                        .uri(url)
                        .headers(headers -> applyDefaultHeaders(headers, resolveToken(accessToken, null)))
                        .retrieve(), RestAPIResponse.class));
    }

    /**
     * Applies the shared error-status handlers to a retrieved response and
     * decodes the body into the given type.
     *
     * @param responseSpec the response to post-process
     * @param responseClass the expected response type
     * @param <T>           the response type
     * @return a {@link Mono} mapping every error status to its exception
     */
    private <T> Mono<T> toResponse(WebClient.ResponseSpec responseSpec, Class<T> responseClass) {
        return responseSpec
                .onStatus(HttpStatus.NO_CONTENT::equals, this::handleNoContent)
                .onStatus(HttpStatus.BAD_REQUEST::equals, this::handleBadRequest)
                .onStatus(HttpStatus.INTERNAL_SERVER_ERROR::equals, this::handleInternalServerError)
                .onStatus(HttpStatus.UNAUTHORIZED::equals, this::handleUnAuthorized)
                .bodyToMono(responseClass);
    }

    /**
     * Constructs a multipart body for file upload.
     *
     * @param file the file to attach
     * @return a {@link MultiValueMap} representing the multipart body
     */
    private static MultiValueMap<String, Object> multipartBody(File file) {
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(file)); // "file" should match the expected request parameter name
        return body;
    }

    /**
     * Logs an error message with the HTTP status from the response.
     *
     * @param s                the log message
     * @param restAPIResponse  the response containing the HTTP status
     */
    private static void logError(String s, RestAPIResponse restAPIResponse) {
        log.error(s, restAPIResponse.getHttpStatus());
    }

    /**
     * Handles a 400 Bad Request response by logging the error and throwing a
     * {@link BusinessBadRequestException}.
     *
     * @param clientResponse the client response to process
     * @return a {@link Mono} that emits the exception
     */
    private Mono<? extends Throwable> handleBadRequest(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(RestAPIResponse.class)
                .handle(((restAPIResponse, throwableSynchronousSink) -> {
                    logError(LOG_BAD_REQUEST, restAPIResponse);
                    throwableSynchronousSink.error(new BusinessBadRequestException(restAPIResponse));
                }));
    }

    /**
     * Handles a 204 No Content response by logging the error and throwing a
     * {@link BusinessBadRequestException}.
     *
     * @param clientResponse the client response to process
     * @return a {@link Mono} that emits the exception
     */
    private Mono<? extends Throwable> handleNoContent(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(RestAPIResponse.class)
                .handle(((restAPIResponse, throwableSynchronousSink) -> {
                    logError(LOG_NO_CONTENT, restAPIResponse);
                    throwableSynchronousSink.error(new BusinessBadRequestException(restAPIResponse));
                }));
    }

    /**
     * Handles a 401 Unauthorized response by logging the error and throwing a
     * {@link TokenRefreshRequiredException}.
     *
     * @param clientResponse the client response to process
     * @return a {@link Mono} that emits the exception
     */
    private Mono<? extends Throwable> handleUnAuthorized(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(RestAPIResponse.class)
                .handle(((restAPIResponse, throwableSynchronousSink) -> {
                    logError(LOG_UN_AUTHORIZED, restAPIResponse);
                    throwableSynchronousSink.error(new TokenRefreshRequiredException(restAPIResponse.toString()));
                }));
    }

    /**
     * Handles a 500 Internal Server Error response by logging the error and throwing a
     * {@link BusinessBadRequestException}.
     *
     * @param clientResponse the client response to process
     * @return a {@link Mono} that emits the exception
     */
    private Mono<? extends Throwable> handleInternalServerError(ClientResponse clientResponse) {
        return clientResponse.bodyToMono(RestAPIResponse.class)
                .handle(((restAPIResponse, throwableSynchronousSink) -> {
                    logError(LOG_INTERNAL_SERVER_ERROR, restAPIResponse);
                    throwableSynchronousSink.error(new BusinessBadRequestException(restAPIResponse));
                }));
    }

}
