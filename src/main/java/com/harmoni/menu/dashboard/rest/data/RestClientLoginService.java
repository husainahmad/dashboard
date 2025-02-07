package com.harmoni.menu.dashboard.rest.data;

import com.harmoni.menu.dashboard.configuration.AuthProperties;
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

    public Mono<RestAPIResponse> login(LoginDto loginDto) {
        return post(authProperties.getUrl().getLogin(), Mono.just(loginDto), LoginDto.class);
    }

}
