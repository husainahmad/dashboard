package com.harmoni.menu.dashboard.service.data.rest;

import com.harmoni.menu.dashboard.configuration.AuthProperties;
import com.harmoni.menu.dashboard.configuration.MenuProperties;
import com.harmoni.menu.dashboard.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@RequiredArgsConstructor
@Service
@Slf4j
public class RestClientLoginService extends RestClientService {

    private final AuthProperties authProperties;
    private final MenuProperties menuProperties;

    public Mono<JwtDto> login(LoginDto loginDto) {
        String url = authProperties.getUrl().getLogin();
        log.debug("Sending login request username={} url={}", loginDto.getUsername(), url);
        return post(url, Mono.just(loginDto), LoginDto.class, JwtDto.class);
    }

    public Mono<RestAPIResponse> getUser(String username, String token) {
        String url = menuProperties.getUrl().getUser().concat("/").concat(username);
        log.debug("Sending get-user request username={} url={} hasToken={}", username, url, ObjectUtils.isNotEmpty(token));
        return get(url, token);
    }

}
