package com.harmoni.menu.dashboard.layout.menu.promotion;

import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the promotion page's entry points: the route an operator navigates to and
 * the annotations Vaadin relies on to build the sidebar item.
 */
class PromotionLayoutTest {

    @Test
    void promotionRoute_isRegisteredUnderTheCatalogPath() {
        Route route = PromotionLayout.class.getAnnotation(Route.class);

        assertNotNull(route, "PromotionLayout must carry a @Route to be reachable");
        assertEquals("promotion", route.value());
    }

    @Test
    void promotionRoute_usesTheMainLayoutShell() {
        Route route = PromotionLayout.class.getAnnotation(Route.class);

        assertNotNull(route);
        assertEquals(com.harmoni.menu.dashboard.layout.MainLayout.class, route.layout());
    }

    @Test
    void promotionRoute_hasAPageTitle() {
        PageTitle pageTitle = PromotionLayout.class.getAnnotation(PageTitle.class);

        assertNotNull(pageTitle, "Vaadin needs a @PageTitle for the browser tab");
        assertTrue(pageTitle.value().startsWith("Promotion"), pageTitle.value());
    }
}
