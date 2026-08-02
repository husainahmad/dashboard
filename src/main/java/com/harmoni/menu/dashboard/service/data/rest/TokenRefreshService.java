package com.harmoni.menu.dashboard.service.data.rest;

import com.harmoni.menu.dashboard.configuration.AuthProperties;
import com.harmoni.menu.dashboard.dto.JwtDto;
import com.harmoni.menu.dashboard.exception.TokenRefreshRequiredException;
import com.harmoni.menu.dashboard.exception.UnAuthorizedServerRequestException;
import com.harmoni.menu.dashboard.util.VaadinSessionUtil;
import com.vaadin.flow.server.VaadinSession;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Component
@Slf4j
public class TokenRefreshService {

    private static TokenRefreshService instance;

    private final AuthProperties authProperties;
    private final WebClient.Builder webClientBuilder;

    private WebClient webClient;

    @PostConstruct
    void init() {
        instance = this;
        webClient = webClientBuilder.build();
    }

    public static TokenRefreshService getInstance() {
        return instance;
    }

    public Mono<JwtDto> refresh(String refreshToken) {
        String url = authProperties.getUrl().getRefreshToken();
        log.debug("Refreshing access token url={}", url);
        return webClient.post()
                .uri(url)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(refreshToken))
                .retrieve()
                .bodyToMono(JwtDto.class);
    }

    @FunctionalInterface
    public interface TokenRequest<T> {
        Mono<T> request(String accessToken);
    }

    public <T> Mono<T> withTokenRefresh(TokenRequest<T> request) {
        VaadinSession session = VaadinSession.getCurrent();
        String refreshToken = VaadinSessionUtil.getAttribute(VaadinSessionUtil.REFRESH_TOKEN, String.class);

        return request.request(null)
                .onErrorResume(TokenRefreshRequiredException.class,
                        error -> refreshAndRetry(request, session, refreshToken, error))
                .onErrorResume(TokenRefreshRequiredException.class, this::handleUnauthorizedAfterRetry);
    }

    private <T> Mono<T> refreshAndRetry(TokenRequest<T> request, VaadinSession session, String refreshToken,
                                        Throwable originalError) {
        if (ObjectUtils.isEmpty(refreshToken)) {
            return Mono.error(originalError);
        }
        return refresh(refreshToken)
                .flatMap(jwtDto -> {
                    if (ObjectUtils.isEmpty(jwtDto) || ObjectUtils.isEmpty(jwtDto.getAccessToken())) {
                        return Mono.error(originalError);
                    }
                    if (session != null) {
                        session.access(() -> {
                            session.setAttribute(VaadinSessionUtil.JWT_TOKEN, jwtDto.getAccessToken());
                            session.setAttribute(VaadinSessionUtil.REFRESH_TOKEN, jwtDto.getRefreshToken());
                        });
                    }
                    return request.request(jwtDto.getAccessToken());
                })
                .onErrorResume(Throwable.class, refreshError -> {
                    log.warn("Token refresh failed", refreshError);
                    return Mono.error(originalError);
                });
    }

    private <T> Mono<T> handleUnauthorizedAfterRetry(Throwable error) {
        log.warn("Unauthorized after token refresh, logging out", error);
        UnAuthorizedServerRequestException.broadcast();
        return Mono.error(error);
    }
}
