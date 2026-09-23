package com.harmoni.menu.dashboard.service.data.rest;

import com.harmoni.menu.dashboard.configuration.AuthProperties;
import com.harmoni.menu.dashboard.configuration.MenuProperties;
import com.harmoni.menu.dashboard.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


/**
 * Blocking REST client for the authentication endpoints: logs in via
 * {@code /auth} and fetches the current user. Extends {@link RestClientService}
 * for the shared token and error handling; consumers subscribe to the returned
 * {@link Mono}.
 */
@Service
@Slf4j
public class RestClientLoginService extends RestClientService {

    private final AuthProperties authProperties;
    private final MenuProperties menuProperties;

    public RestClientLoginService(AuthProperties authProperties, MenuProperties menuProperties,
                                  TokenRefreshService tokenRefreshService) {
        super(tokenRefreshService);
        this.authProperties = authProperties;
        this.menuProperties = menuProperties;
    }

    /**
     * Authenticates the given credentials and returns the issued JWT pair.
     *
     * @param loginDto the username/password pair
     * @return a {@link Mono} of the JWT token pair
     */
    public Mono<JwtDto> login(LoginDto loginDto) {
        String url = authProperties.getUrl().getLogin();
        log.debug("Sending login request username={} url={}", loginDto.getUsername(), url);
        return post(url, Mono.just(loginDto), LoginDto.class, JwtDto.class);
    }

    /**
     * Fetches the user identified by {@code username} using an explicit token.
     *
     * @param username the user to look up
     * @param token    the bearer token, or {@code null}
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> getUser(String username, String token) {
        String url = String.format(menuProperties.getUrl().getUsers().getByName(), username);
        log.debug("Sending get-user request username={} url={} hasToken={}", username, url, ObjectUtils.isNotEmpty(token));
        return get(url, token);
    }

}
