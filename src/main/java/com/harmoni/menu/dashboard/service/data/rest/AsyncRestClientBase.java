package com.harmoni.menu.dashboard.service.data.rest;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harmoni.menu.dashboard.exception.BusinessBadRequestException;
import com.harmoni.menu.dashboard.exception.BusinessServerRequestException;
import com.harmoni.menu.dashboard.exception.TokenRefreshRequiredException;
import com.harmoni.menu.dashboard.util.VaadinSessionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.Serializable;
import java.util.Objects;

/**
 * Shared plumbing for the reactive REST clients: token resolution, error status
 * mapping and safe callback invocation. Concrete clients only add their own
 * endpoint methods built on {@link #makeAsyncRequest}.
 */
@Slf4j
public abstract class AsyncRestClientBase implements Serializable {

    private static final String BEARER = "Bearer ";

    /** Formats a resource base path plus an id, e.g. {@code "/brands/5"}. */
    protected static final String URL_FORMAT = "%s/%d";

    protected final transient WebClient webClient = WebClient.builder().build();

    private final transient TokenRefreshService tokenRefreshService;

    protected AsyncRestClientBase(TokenRefreshService tokenRefreshService) {
        this.tokenRefreshService = tokenRefreshService;
    }

    private final transient ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

    /**
     * Callback fired when the async request lands, either with the deserialized
     * payload or with the error {@link Throwable}.
     *
     * @param <T> the payload type carried on success
     */
    public interface AsyncRestCallback<T> {
        void operationFinished(T result);
    }

    /**
     * Performs a GET for {@code uri} without an error callback.
     *
     * @param uri          endpoint to call
     * @param typeReference expected shape of the response data
     * @param callback     success callback
     * @param <T>          payload type
     */
    protected <T> void makeAsyncRequest(String uri, TypeReference<T> typeReference, AsyncRestCallback<T> callback) {
        makeAsyncRequest(uri, typeReference, callback, null);
    }

    /**
     * Performs a GET for {@code uri}, refreshing the token when needed and
     * mapping business errors into exceptions reported through
     * {@code errorCallback}.
     *
     * @param uri            endpoint to call
     * @param typeReference  expected shape of the response data
     * @param callback       success callback
     * @param errorCallback  optional error callback
     * @param <T>            payload type
     */
    protected <T> void makeAsyncRequest(String uri, TypeReference<T> typeReference, AsyncRestCallback<T> callback,
                                        AsyncRestCallback<Throwable> errorCallback) {
        TokenRefreshService.TokenRequest<RestAPIResponse> request = accessToken -> webClient.get()
                .uri(uri)
                .header(HttpHeaders.AUTHORIZATION, resolveToken(accessToken))
                .retrieve()
                .onStatus(HttpStatus.BAD_REQUEST::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessBadRequestException::new))
                .onStatus(HttpStatus.INTERNAL_SERVER_ERROR::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessServerRequestException::new))
                .onStatus(HttpStatus.UNAUTHORIZED::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(response -> new TokenRefreshRequiredException(response.toString())))
                .bodyToMono(RestAPIResponse.class);

        tokenRefreshService.withTokenRefresh(request)
                .subscribe(result -> {
                    T data = objectMapper.convertValue(
                            Objects.requireNonNull(result).getData(),
                            typeReference
                    );
                    callback.operationFinished(data);
                }, error -> {
                    log.error("Async request failed uri={}", uri, error);
                    if (errorCallback != null) {
                        errorCallback.operationFinished(error);
                    }
                });
    }

    private static String resolveToken(String accessToken) {
        if (ObjectUtils.isNotEmpty(accessToken)) {
            return BEARER.concat(accessToken);
        }
        return getTokenString();
    }

    private static String getTokenString() {
        String token = VaadinSessionUtil.getAttribute(VaadinSessionUtil.JWT_TOKEN, String.class);
        if (ObjectUtils.isNotEmpty(token)) {
            return BEARER.concat(token);
        }
        return token;
    }
}