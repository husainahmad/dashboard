package com.harmoni.menu.dashboard.service.data.rest;

import com.harmoni.menu.dashboard.configuration.MenuProperties;
import com.harmoni.menu.dashboard.dto.PromotionDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.Serializable;

/**
 * REST client for the promotion endpoints: the aggregate itself plus the
 * order-line discounts that record what a promotion actually granted.
 * <p>
 * Extends {@link RestClientService} so every call carries the session JWT and
 * transparently refreshes it on a 401.
 */
@Service
@Slf4j
public class RestClientPromotionService extends RestClientService implements Serializable {

    private final transient MenuProperties menuProperties;

    public RestClientPromotionService(MenuProperties menuProperties,
                                      TokenRefreshService tokenRefreshService) {
        super(tokenRefreshService);
        this.menuProperties = menuProperties;
    }

    /**
     * Creates a promotion together with its schedules, targets, rules and special
     * prices.
     *
     * @param promotionDto the aggregate to create
     * @return the created promotion
     */
    public Mono<RestAPIResponse> createPromotion(PromotionDto promotionDto) {
        return post(menuProperties.getUrl().getPromotions().getPromotion(),
                Mono.just(promotionDto), PromotionDto.class);
    }

    /**
     * Updates a promotion. An omitted child collection is left untouched, an empty
     * one is cleared.
     *
     * @param promotionDto the aggregate to update, including its id
     * @return the updated promotion
     */
    public Mono<RestAPIResponse> updatePromotion(PromotionDto promotionDto) {
        return put(menuProperties.getUrl().getPromotions().getPromotion(),
                Mono.just(promotionDto), PromotionDto.class);
    }

    /**
     * Retrieves a promotion with all child collections populated, so the form can
     * be opened for editing.
     *
     * @param promotionId the promotion id
     * @return the promotion
     */
    public Mono<RestAPIResponse> getPromotionById(Long promotionId) {
        return get(URL_FORMAT.formatted(menuProperties.getUrl().getPromotions().getPromotion(), promotionId));
    }

    /**
     * Retrieves a promotion by its unique code.
     *
     * @param code the promotion code
     * @return the promotion
     */
    public Mono<RestAPIResponse> getPromotionByCode(String code) {
        return get(menuProperties.getUrl().getPromotions().getByCode().formatted(code));
    }

    /**
     * Moves a promotion to a new lifecycle state.
     *
     * @param promotionId the promotion id
     * @param status      the new lifecycle state
     * @return the number of updated rows
     */
    public Mono<RestAPIResponse> updatePromotionStatus(Long promotionId, String status) {
        String url = menuProperties.getUrl().getPromotions().getStatus().formatted(promotionId);
        return patch(url + "?status=" + status);
    }

    /**
     * Deletes a promotion and its child collections.
     *
     * @param promotionId the promotion id
     * @return the number of deleted rows
     */
    public Mono<RestAPIResponse> deletePromotion(Long promotionId) {
        return delete(URL_FORMAT.formatted(menuProperties.getUrl().getPromotions().getPromotion(), promotionId));
    }

    /**
     * Deletes every promotion matching the given filter, mirroring the list query.
     *
     * @param status        optional lifecycle state filter
     * @param promotionType optional mechanism filter
     * @param search        optional keyword matched against code and name
     * @return the number of deleted rows
     */
    public Mono<RestAPIResponse> deletePromotionsByFilter(String status, String promotionType, String search) {
        String url = String.format("%s?status=%s&promotionType=%s&search=%s",
                menuProperties.getUrl().getPromotions().getPromotion(),
                orEmpty(status), orEmpty(promotionType), orEmpty(search));
        return delete(url);
    }

    /**
     * Retrieves the discounts recorded against one order line, which is how a
     * basket shows what was actually granted.
     *
     * @param orderItemId the order line id
     * @return list of discounts, oldest first
     */
    public Mono<RestAPIResponse> getDiscountsByOrderItem(Long orderItemId) {
        return get(menuProperties.getUrl().getPromotions().getOrderItemDiscounts().formatted(orderItemId));
    }

    private static String orEmpty(String value) {
        return value == null ? "" : value;
    }
}
