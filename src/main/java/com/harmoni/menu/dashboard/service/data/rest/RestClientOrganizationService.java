package com.harmoni.menu.dashboard.service.data.rest;

import com.harmoni.menu.dashboard.configuration.MenuProperties;
import com.harmoni.menu.dashboard.dto.*;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * Blocking REST client for the organization / administration endpoints:
 * chains, brands, tiers (with their services and menus), stores (paginated)
 * and users. Builds URLs from {@link MenuProperties}, extends
 * {@link RestClientService} and returns {@link Mono} responses.
 */
@Service
@Slf4j
public class RestClientOrganizationService extends RestClientService {

    private final MenuProperties menuProperties;

    public RestClientOrganizationService(MenuProperties menuProperties,
                                         TokenRefreshService tokenRefreshService) {
        super(tokenRefreshService);
        this.menuProperties = menuProperties;
    }

    /**
     * Creates a chain.
     *
     * @param chainDto the chain to create
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> createChain(ChainDto chainDto) {
        return post(menuProperties.getUrl().getChain(), Mono.just(chainDto), ChainDto.class);
    }

    /**
     * Updates an existing chain.
     *
     * @param chainDto the chain to update
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> updateChain(ChainDto chainDto) {
        return put(URL_FORMAT.formatted(menuProperties.getUrl().getChain(), chainDto.getId()),
                Mono.just(chainDto), ChainDto.class);
    }

    /**
     * Creates a brand under the owning chain.
     *
     * @param brandDto the brand to create
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> createBrand(BrandDto brandDto) {
        return post(menuProperties.getUrl().getBrand(), Mono.just(brandDto), BrandDto.class);
    }

    /**
     * Updates an existing brand.
     *
     * @param brandDto the brand to update
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> updateBrand(BrandDto brandDto) {
        return put(URL_FORMAT.formatted(menuProperties.getUrl().getChain(), brandDto.getId()),
                Mono.just(brandDto), BrandDto.class);
    }

    /**
     * Deletes a brand.
     *
     * @param brandDto the brand to delete
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> deleteBrand(BrandDto brandDto) {
        return delete(URL_FORMAT.formatted(menuProperties.getUrl().getBrand(), brandDto.getId()));
    }

    /**
     * Creates a tier.
     *
     * @param tierDto the tier to create
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> createTier(TierDto tierDto) {
        return post(menuProperties.getUrl().getTier(), Mono.just(tierDto), TierDto.class);
    }

    /**
     * Updates an existing tier.
     *
     * @param tierDto the tier to update
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> updateTier(TierDto tierDto) {
        return put(URL_FORMAT.formatted(menuProperties.getUrl().getTier(), tierDto.getId()),
                Mono.just(tierDto), TierDto.class);
    }

    /**
     * Replaces the services assigned to a tier.
     *
     * @param tierDto        the target tier
     * @param tierServiceDtos the new service assignments
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> updateTierService(TierDto tierDto, List<TierSubServiceDto> tierServiceDtos) {
        return put(String.format(menuProperties.getUrl().getTiers().getServices().getUpdate(), tierDto.getId()),
                Mono.just(tierServiceDtos), List.class);
    }

    /**
     * Replaces the menus assigned to a tier.
     *
     * @param tierDto     the target tier
     * @param tierMenuDtos the new menu assignments
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> updateTierMenu(TierDto tierDto, List<TierMenuDto> tierMenuDtos) {
        return put(String.format(menuProperties.getUrl().getTiers().getMenus().getUpdate(), tierDto.getId()),
                Mono.just(tierMenuDtos), List.class);
    }

    /**
     * Deletes a tier.
     *
     * @param tierDto the tier to delete
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> deleteTier(TierDto tierDto) {
        return delete(URL_FORMAT.formatted(menuProperties.getUrl().getTier(), tierDto.getId()));
    }

    /**
     * Deletes a chain.
     *
     * @param chainDto the chain to delete
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> deleteChain(ChainDto chainDto) {
        return delete(URL_FORMAT.formatted(menuProperties.getUrl().getChain(), chainDto.getId()));
    }

    /**
     * Creates a store.
     *
     * @param storeDto the store to create
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> createStore(StoreDto storeDto) {
        return post(menuProperties.getUrl().getStore(), Mono.just(storeDto), StoreDto.class);
    }

    /**
     * Updates an existing store.
     *
     * @param storeDto the store to update
     * @return a {@link Mono} with the server response
     */
    public Mono<RestAPIResponse> updateStore(StoreDto storeDto) {
        return put(URL_FORMAT.formatted(menuProperties.getUrl().getStore(), storeDto.getId()),
                Mono.just(storeDto), StoreDto.class);
    }

    public Mono<RestAPIResponse> deleteStore(StoreDto storeDto) {
        return delete(URL_FORMAT.formatted(menuProperties.getUrl().getStore(), storeDto.getId()));
    }

    public Mono<RestAPIResponse> getStore(Integer chainId, int page, int size, String search) {
        String uri = String.format(menuProperties.getUrl().getStoreQuery(), chainId, page, size, search);
        return get(uri);
    }

    public Mono<RestAPIResponse> createUser(UserDto userDto) {
        return post(menuProperties.getUrl().getUser(), Mono.just(userDto), UserDto.class);
    }

    public Mono<RestAPIResponse> deleteUser(UserDto userDto) {
        return delete(String.format(menuProperties.getUrl().getUsers().getById(), userDto.getId()));
    }

    public Mono<RestAPIResponse> updateUser(UserDto userDto) {
        return put(String.format(menuProperties.getUrl().getUsers().getById(), userDto.getId()),
                Mono.just(userDto), UserDto.class);
    }
}
