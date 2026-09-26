package com.harmoni.menu.dashboard.configuration;

import lombok.Data;

import java.io.Serializable;

/**
 * Holds the REST URLs of the promotion endpoints.
 *
 * <p>
 * Binds the {@code menu.url.promotions.*} keys; consumed as the
 * {@code promotions} subtree of {@link UrlProperties}.
 * </p>
 */
@Data
public class PromotionProperties implements Serializable {

    /** Collection endpoint, also the target of a filtered delete. */
    private String promotion;

    /** Paged and filtered query, formatted with page, size, status, type and search. */
    private String query;

    /** Redeemable-now lookup. */
    private String redeemable;

    /** Lookup by unique code, formatted with the code. */
    private String byCode;

    /** Status transition, formatted with the promotion id. */
    private String status;

    /** Discounts recorded against one order line, formatted with the order line id. */
    private String orderItemDiscounts;
}
