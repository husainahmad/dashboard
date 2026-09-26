package com.harmoni.menu.dashboard.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Binds the real {@code application.properties} so a mistyped or missing
 * {@code menu.url.promotions.*} key fails here rather than as a null URL inside a
 * WebClient call at runtime.
 */
@SpringBootTest
class PromotionPropertiesBindingTest {

    private static final String MENU_BASE = "http://localhost:8080";

    @Autowired
    private MenuProperties menuProperties;

    private PromotionProperties promotions() {
        assertNotNull(menuProperties.getUrl(), "menu.url should be bound");
        PromotionProperties promotions = menuProperties.getUrl().getPromotions();
        assertNotNull(promotions, "menu.url.promotions should be bound");
        return promotions;
    }

    @Test
    void promotionCollectionUrl_isBound() {
        assertEquals(MENU_BASE + "/api/v1/promotion", promotions().getPromotion());
    }

    @Test
    void promotionQueryUrl_isBoundAndCarriesEveryFilterPlaceholder() {
        String query = promotions().getQuery();

        assertNotNull(query);
        assertTrue(query.startsWith(MENU_BASE + "/api/v1/promotion?"), query);
        assertTrue(query.contains("page=%d"), query);
        assertTrue(query.contains("size=%d"), query);
        assertTrue(query.contains("status=%s"), query);
        assertTrue(query.contains("promotionType=%s"), query);
        assertTrue(query.contains("search=%s"), query);
    }

    @Test
    void promotionSubResourceUrls_areBound() {
        PromotionProperties promotions = promotions();

        assertEquals(MENU_BASE + "/api/v1/promotion/redeemable", promotions.getRedeemable());
        assertEquals(MENU_BASE + "/api/v1/promotion/code/%s", promotions.getByCode());
        assertEquals(MENU_BASE + "/api/v1/promotion/%d/status", promotions.getStatus());
        assertEquals(MENU_BASE + "/api/v1/order-item-discount/order-item/%d",
                promotions.getOrderItemDiscounts());
    }

    @Test
    void promotionUrlsFormatIntoThePathsTheMenuServiceExposes() {
        PromotionProperties promotions = promotions();

        assertEquals(MENU_BASE + "/api/v1/promotion/42",
                String.format("%s/%d", promotions.getPromotion(), 42L));
        assertEquals(MENU_BASE + "/api/v1/promotion?page=1&size=15&status=ACTIVE&promotionType=&search=happy",
                String.format(promotions.getQuery(), 1, 15, "ACTIVE", "", "happy"));
        assertEquals(MENU_BASE + "/api/v1/promotion/code/HAPPY10",
                promotions.getByCode().formatted("HAPPY10"));
        assertEquals(MENU_BASE + "/api/v1/promotion/42/status?status=PAUSED",
                promotions.getStatus().formatted(42L) + "?status=PAUSED");
    }
}
