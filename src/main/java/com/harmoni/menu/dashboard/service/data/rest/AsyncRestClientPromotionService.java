package com.harmoni.menu.dashboard.service.data.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.harmoni.menu.dashboard.configuration.MenuProperties;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

/**
 * Non-blocking reads for the promotion views, so the grid never blocks the Vaadin
 * UI thread while the menu service is queried.
 */
@Service
public class AsyncRestClientPromotionService extends AsyncRestClientBase implements Serializable {

    private final transient MenuProperties menuProperties;

    public AsyncRestClientPromotionService(MenuProperties menuProperties,
                                           TokenRefreshService tokenRefreshService) {
        super(tokenRefreshService);
        this.menuProperties = menuProperties;
    }

    /**
     * Fetches one page of promotions.
     *
     * @param callback      receives the page envelope, whose {@code data} holds the
     *                      rows and whose {@code page} holds the total page count
     * @param errorCallback receives the failure
     * @param page          the 1-based page number
     * @param size          the page size
     * @param status        optional lifecycle state filter
     * @param promotionType optional mechanism filter
     * @param search        optional keyword matched against code and name
     */
    public void getPromotionsAsync(AsyncRestCallback<Map<String, Object>> callback,
                                   AsyncRestCallback<Throwable> errorCallback,
                                   int page, int size, String status,
                                   String promotionType, String search) {
        makeAsyncRequest(buildPromotionUri(page, size, status, promotionType, search),
                new TypeReference<>() {
                }, callback, errorCallback);
    }

    /**
     * Builds the paged query URI. An unset filter is sent blank rather than omitted,
     * which the menu service reads as "no filter".
     *
     * @param page          the 1-based page number
     * @param size          the page size
     * @param status        optional lifecycle state filter
     * @param promotionType optional mechanism filter
     * @param search        optional keyword matched against code and name
     * @return the fully formatted query URI
     */
    String buildPromotionUri(int page, int size, String status, String promotionType, String search) {
        return String.format(menuProperties.getUrl().getPromotions().getQuery(),
                page, size, orEmpty(status), orEmpty(promotionType), orEmpty(search));
    }

    /**
     * Fetches the promotions that may be redeemed right now.
     *
     * @param callback      receives the promotions, ordered by priority
     * @param errorCallback receives the failure
     */
    public void getRedeemablePromotionsAsync(AsyncRestCallback<List<Map<String, Object>>> callback,
                                              AsyncRestCallback<Throwable> errorCallback) {
        makeAsyncRequest(menuProperties.getUrl().getPromotions().getRedeemable(),
                new TypeReference<>() {
                }, callback, errorCallback);
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
