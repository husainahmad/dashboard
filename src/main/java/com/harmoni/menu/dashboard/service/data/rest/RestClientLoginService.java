package com.harmoni.menu.dashboard.service.data.rest;

import com.harmoni.menu.dashboard.configuration.AuthProperties;
import com.harmoni.menu.dashboard.configuration.MenuProperties;
import com.harmoni.menu.dashboard.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@RequiredArgsConstructor
@Service
@Slf4j
public class RestClientLoginService extends RestClientService {

    private final AuthProperties authProperties;
    private final MenuProperties menuProperties;

    public Mono<RestAPIResponse> login(LoginDto loginDto) {
        return post(authProperties.getUrl().getLogin(), Mono.just(loginDto), LoginDto.class);
    }

    public Mono<RestAPIResponse> getUser(String username) {
        return get(menuProperties.getUrl().getUser().concat("/").concat(username));
    }

}
