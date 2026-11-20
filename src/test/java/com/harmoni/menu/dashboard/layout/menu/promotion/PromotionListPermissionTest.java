package com.harmoni.menu.dashboard.layout.menu.promotion;

import com.harmoni.menu.dashboard.layout.util.Permissions;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientPromotionService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientPromotionService;
import com.harmoni.menu.dashboard.support.UiTestSupport;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.button.Button;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class PromotionListPermissionTest {

    @Test
    void newPromotionIsHiddenForViewers() {
        Button add = addButton(Permissions.MENU_VIEW);

        assertNotNull(add);
        assertFalse(add.isVisible());
    }

    @Test
    void newPromotionIsShownForCatalogEditors() {
        Button add = addButton(Permissions.MENU_EDIT);

        assertNotNull(add);
        assertTrue(add.isVisible());
    }

    private static Button addButton(String... permissions) {
        PromotionListView view = new PromotionListView(
                mock(AsyncRestClientPromotionService.class),
                mock(AsyncRestClientMenuService.class),
                mock(AsyncRestClientOrganizationService.class),
                mock(RestClientPromotionService.class),
                mock(AccessService.class),
                UiTestSupport.authorization(permissions));
        return UiTestSupport.buttonByText(view.getToolbarComponent(), Messages.get("action.newPromotion"));
    }
}
