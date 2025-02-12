package com.harmoni.menu.dashboard.service.data.rest;

import com.harmoni.menu.dashboard.configuration.MenuProperties;
import com.harmoni.menu.dashboard.dto.*;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@RequiredArgsConstructor
@Service
@Slf4j
public class RestClientOrganizationService extends RestClientService {

    private final MenuProperties menuProperties;
    private static final String URL_FORMAT = "%s/%d";

    public Mono<RestAPIResponse> createChain(ChainDto chainDto) {
        return post(menuProperties.getUrl().getChain(), Mono.just(chainDto), ChainDto.class);
    }

    public Mono<RestAPIResponse> updateChain(ChainDto chainDto) {
        return put(URL_FORMAT.formatted(menuProperties.getUrl().getChain(), chainDto.getId()),
                Mono.just(chainDto), ChainDto.class);
    }

    public Mono<RestAPIResponse> createBrand(BrandDto brandDto) {
        return post(menuProperties.getUrl().getBrand(), Mono.just(brandDto), BrandDto.class);
    }

    public Mono<RestAPIResponse> updateBrand(BrandDto brandDto) {
        return put(URL_FORMAT.formatted(menuProperties.getUrl().getChain(), brandDto.getId()),
                Mono.just(brandDto), BrandDto.class);
    }

    public Mono<RestAPIResponse> deleteBrand(BrandDto brandDto) {
        return delete(URL_FORMAT.formatted(menuProperties.getUrl().getBrand(), brandDto.getId()));
    }

    public Mono<RestAPIResponse> createTier(TierDto tierDto) {
        return post(menuProperties.getUrl().getTier(), Mono.just(tierDto), TierDto.class);
    }

    public Mono<RestAPIResponse> updateTier(TierDto tierDto) {
        return put(URL_FORMAT.formatted(menuProperties.getUrl().getTier(), tierDto.getId()),
                Mono.just(tierDto), TierDto.class);
    }

    public Mono<RestAPIResponse> updateTierService(TierDto tierDto, List<TierSubServiceDto> tierServiceDtos) {
        return put(String.format(menuProperties.getUrl().getTiers().getServices().getUpdate(), tierDto.getId()),
                Mono.just(tierServiceDtos), List.class);
    }

    public Mono<RestAPIResponse> updateTierMenu(TierDto tierDto, List<TierMenuDto> tierMenuDtos) {
        return put(String.format(menuProperties.getUrl().getTiers().getMenus().getUpdate(), tierDto.getId()),
                Mono.just(tierMenuDtos), List.class);
    }

    public Mono<RestAPIResponse> deleteTier(TierDto tierDto) {
        return delete(URL_FORMAT.formatted(menuProperties.getUrl().getTier(), tierDto.getId()));
    }

    public Mono<RestAPIResponse> deleteChain(ChainDto chainDto) {
        return delete(URL_FORMAT.formatted(menuProperties.getUrl().getChain(), chainDto.getId()));
    }

    public Mono<RestAPIResponse> createStore(StoreDto storeDto) {
        return post(menuProperties.getUrl().getStore(), Mono.just(storeDto), StoreDto.class);
    }

    public Mono<RestAPIResponse> updateStore(StoreDto storeDto) {
        return put(URL_FORMAT.formatted(menuProperties.getUrl().getStore(), storeDto.getId()),
                Mono.just(storeDto), StoreDto.class);
    }

    public Mono<RestAPIResponse> deleteStore(StoreDto storeDto) {
        return delete(URL_FORMAT.formatted(menuProperties.getUrl().getStore(), storeDto.getId()));
    }

    public Mono<RestAPIResponse> getStore(Integer chainId, int page, int size, String search) {
        String uri = String.format("%s?chainId=%d&page=%d&size=%d&search=%s",
                menuProperties.getUrl().getStore(),
                chainId, page, size, search);
        return get(uri);
    }

    public Mono<RestAPIResponse> createUser(UserDto userDto) {
        return post(menuProperties.getUrl().getUser(), Mono.just(userDto), UserDto.class);
    }

    public Mono<RestAPIResponse> deleteUser(UserDto userDto) {
        return delete(menuProperties.getUrl().getUser().concat("/").concat(userDto.getId().toString()));
    }

    public Mono<RestAPIResponse> updateUser(UserDto userDto) {
        return put(menuProperties.getUrl().getUser().concat("/").concat(userDto.getId().toString()),
                Mono.just(userDto), UserDto.class);
    }
}
