package com.harmoni.menu.dashboard.service.data.rest;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.harmoni.menu.dashboard.exception.BusinessBadRequestException;
import com.harmoni.menu.dashboard.exception.BusinessForbiddenException;
import com.harmoni.menu.dashboard.exception.BusinessServerRequestException;
import com.harmoni.menu.dashboard.exception.TokenRefreshRequiredException;
import com.harmoni.menu.dashboard.util.VaadinSessionUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import io.netty.channel.ChannelOption;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.io.Serializable;
import java.time.Duration;
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

    /**
     * Shared client. The timeouts are what keep a dropdown from spinning
     * forever: without a connect/response deadline a request to a stalled
     * upstream never completes and never errors, so the combo box has nothing
     * to render and no failure to report.
     */
    protected final transient WebClient webClient = buildWebClient();

    private static WebClient buildWebClient() {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10_000)
                .responseTimeout(Duration.ofSeconds(30));
        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }

    private final transient TokenRefreshService tokenRefreshService;

    protected AsyncRestClientBase(TokenRefreshService tokenRefreshService) {
        this.tokenRefreshService = tokenRefreshService;
    }

    private final transient ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true)
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

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
        TokenRefreshService.TokenRequest<RestAPIResponse> request = accessToken ->
                toBody(webClient.get()
                        .uri(uri)
                        .header(HttpHeaders.AUTHORIZATION, resolveToken(accessToken))
                        .retrieve(), RestAPIResponse.class, false);

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

    /**
     * Performs a GET against a service that answers with a plain body, without
     * entering the token-refresh / global-logout flow.
     *
     * <p>A 401 is reported through {@code errorCallback} like any other failure,
     * so a read-only lookup cannot tear down the whole session. That matters for
     * the standalone customer service, which is reached through the same gateway
     * but is not a session-critical endpoint: a rejected token there should show
     * the page's retry feedback, not navigate the operator to the login screen.
     * The session token is still attached when present.</p>
     *
     * @param uri           endpoint to call
     * @param responseClass expected type of the raw response body
     * @param callback      success callback
     * @param errorCallback optional error callback
     * @param <T>           payload type
     */
    protected <T> void makeAsyncRawRequestWithoutLogout(String uri, Class<T> responseClass,
                                                        AsyncRestCallback<T> callback,
                                                        AsyncRestCallback<Throwable> errorCallback) {
        TokenRefreshService.TokenRequest<T> request = accessToken ->
                toBody(webClient.get()
                        .uri(uri)
                        .header(HttpHeaders.AUTHORIZATION, resolveToken(accessToken))
                        .retrieve(), responseClass, true);

        tokenRefreshService.withTokenRefreshWithoutLogout(request)
                .subscribe(result -> {
                    if (result != null) {
                        callback.operationFinished(result);
                    }
                }, error -> {
                    log.error("Async request failed uri={}", uri, error);
                    if (errorCallback != null) {
                        errorCallback.operationFinished(error);
                    }
                });
    }

    /**
     * Applies the shared error-status mapping to a retrieved response and decodes
     * the body into {@code responseClass}.
     *
     * @param responseSpec          the response to post-process
     * @param responseClass         the expected body type
     * @param notFoundAsBadRequest  whether a 404 should map to a bad-request error
     * @param <T>                   the body type
     * @return a {@link Mono} with the decoded body
     */
    private static <T> Mono<T> toBody(WebClient.ResponseSpec responseSpec, Class<T> responseClass,
                                      boolean notFoundAsBadRequest) {
        WebClient.ResponseSpec spec = notFoundAsBadRequest
                ? responseSpec.onStatus(HttpStatus.NOT_FOUND::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessBadRequestException::new))
                : responseSpec;
        return spec
                .onStatus(HttpStatus.BAD_REQUEST::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessBadRequestException::new))
                .onStatus(HttpStatus.FORBIDDEN::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessForbiddenException::new))
                .onStatus(HttpStatus.INTERNAL_SERVER_ERROR::equals,
                        clientResponse -> clientResponse.bodyToMono(RestAPIResponse.class)
                                .map(BusinessServerRequestException::new))
                .onStatus(HttpStatus.UNAUTHORIZED::equals,
                        clientResponse -> clientResponse.createException()
                                .map(error -> new TokenRefreshRequiredException(error.getResponseBodyAsString())))
                .bodyToMono(responseClass);
    }

    /**
     * Resolves the token to use for the request. If an explicit access token is
     * provided, it is used; otherwise, the token from the Vaadin session is used.
     *
     * @param accessToken the explicit access token, or null
     * @return the resolved Bearer token string
     */
    private static String resolveToken(String accessToken) {
        if (ObjectUtils.isNotEmpty(accessToken)) {
            return BEARER.concat(accessToken);
        }
        return getTokenString();
    }

    /**
     * Retrieves the JWT token from the Vaadin session and formats it as a Bearer token.
     *
     * @return the formatted Bearer token, or null if no token is present
     */
    private static String getTokenString() {
        String token = VaadinSessionUtil.getAttribute(VaadinSessionUtil.JWT_TOKEN, String.class);
        if (ObjectUtils.isNotEmpty(token)) {
            return BEARER.concat(token);
        }
        return token;
    }
}