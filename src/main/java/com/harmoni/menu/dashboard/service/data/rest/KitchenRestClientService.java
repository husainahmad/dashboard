package com.harmoni.menu.dashboard.service.data.rest;

import com.harmoni.menu.dashboard.configuration.MenuProperties;
import com.harmoni.menu.dashboard.dto.KitchenCategoryRoutingDto;
import com.harmoni.menu.dashboard.dto.KitchenPrinterDto;
import com.harmoni.menu.dashboard.dto.KitchenProductRoutingDto;
import com.harmoni.menu.dashboard.dto.KitchenStationDto;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

/**
 * Blocking client for the kitchen configuration endpoints.
 */
@Service
public class KitchenRestClientService extends RestClientService {

    private final transient MenuProperties menuProperties;

    public KitchenRestClientService(MenuProperties menuProperties,
                                    TokenRefreshService tokenRefreshService) {
        super(tokenRefreshService);
        this.menuProperties = menuProperties;
    }

    public Mono<RestAPIResponse> listStations(Integer storeId) {
        return get(String.format(menuProperties.getUrl().getKitchen().getStations(), storeId));
    }

    public Mono<RestAPIResponse> createStation(Integer storeId, KitchenStationDto dto) {
        return post(String.format(menuProperties.getUrl().getKitchen().getStations(), storeId),
                Mono.just(dto), KitchenStationDto.class);
    }

    public Mono<RestAPIResponse> updateStation(Integer id, KitchenStationDto dto) {
        return put(String.format(menuProperties.getUrl().getKitchen().getStationById(), id),
                Mono.just(dto), KitchenStationDto.class);
    }

    public Mono<RestAPIResponse> deleteStation(Integer id) {
        return delete(String.format(menuProperties.getUrl().getKitchen().getStationById(), id));
    }

    public Mono<RestAPIResponse> listPrinters(Integer stationId) {
        return get(String.format(menuProperties.getUrl().getKitchen().getPrintersByStation(), stationId));
    }

    public Mono<RestAPIResponse> createPrinter(Integer stationId, KitchenPrinterDto dto) {
        return post(String.format(menuProperties.getUrl().getKitchen().getPrintersByStation(), stationId),
                Mono.just(dto), KitchenPrinterDto.class);
    }

    public Mono<RestAPIResponse> updatePrinter(Integer id, KitchenPrinterDto dto) {
        return put(String.format(menuProperties.getUrl().getKitchen().getPrinterById(), id),
                Mono.just(dto), KitchenPrinterDto.class);
    }

    public Mono<RestAPIResponse> deletePrinter(Integer id) {
        return delete(String.format(menuProperties.getUrl().getKitchen().getPrinterById(), id));
    }

    public Mono<RestAPIResponse> getRoutings(Integer storeId) {
        return get(String.format(menuProperties.getUrl().getKitchen().getRoutings(), storeId));
    }

    public Mono<RestAPIResponse> routeCategory(Integer storeId, KitchenCategoryRoutingDto dto) {
        return post(String.format(menuProperties.getUrl().getKitchen().getRoutingCategories(), storeId),
                Mono.just(dto), KitchenCategoryRoutingDto.class);
    }

    public Mono<RestAPIResponse> routeProduct(Integer storeId, KitchenProductRoutingDto dto) {
        return post(String.format(menuProperties.getUrl().getKitchen().getRoutingProducts(), storeId),
                Mono.just(dto), KitchenProductRoutingDto.class);
    }

    public Mono<RestAPIResponse> deleteCategoryRouting(Integer storeId, Integer categoryId) {
        return delete(String.format(menuProperties.getUrl().getKitchen().getRoutingCategories(), storeId)
                + "/" + categoryId);
    }

    public Mono<RestAPIResponse> deleteProductRouting(Integer storeId, Integer productId) {
        return delete(String.format(menuProperties.getUrl().getKitchen().getRoutingProducts(), storeId)
                + "/" + productId);
    }
}
