package com.harmoni.menu.dashboard.service.data.rest;

import com.harmoni.menu.dashboard.configuration.SettingProperties;
import com.harmoni.menu.dashboard.dto.TableDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
@Service
@Slf4j
public class RestClientSettingService extends RestClientService {

    private final SettingProperties settingProperties;
    private static final String URL_FORMAT = "%s/%d";

    public Mono<RestAPIResponse> createTable(TableDto tableDto) {
        return post(settingProperties.getUrl().getTable(), Mono.just(tableDto), TableDto.class);
    }

    public Mono<RestAPIResponse> updateTable(TableDto tableDto) {
        return put(URL_FORMAT.formatted(settingProperties.getUrl().getTable(), tableDto.getId()),
                Mono.just(tableDto), TableDto.class);
    }

    public Mono<RestAPIResponse> deleteTable(TableDto tableDto) {
        return delete(URL_FORMAT.formatted(settingProperties.getUrl().getTable(), tableDto.getId()));
    }
}
