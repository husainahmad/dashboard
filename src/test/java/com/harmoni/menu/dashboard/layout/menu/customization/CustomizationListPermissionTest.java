package com.harmoni.menu.dashboard.layout.menu.customization;

import com.harmoni.menu.dashboard.layout.util.Permissions;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.support.UiTestSupport;
import com.vaadin.flow.component.button.Button;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class CustomizationListPermissionTest {

    @Test
    void newCustomizationIsHiddenForViewers() {
        Button add = addButton(Permissions.MENU_VIEW);

        assertNotNull(add);
        assertFalse(add.isVisible());
    }

    @Test
    void newCustomizationIsShownForCatalogEditors() {
        Button add = addButton(Permissions.MENU_EDIT);

        assertNotNull(add);
        assertTrue(add.isVisible());
    }

    private static Button addButton(String... permissions) {
        CustomizationListView view = new CustomizationListView(
                mock(AsyncRestClientMenuService.class),
                mock(RestClientMenuService.class),
                mock(AccessService.class),
                UiTestSupport.authorization(permissions));
        return UiTestSupport.buttonByText(view.getToolbarComponent(), com.harmoni.menu.dashboard.util.Messages.get("action.newCustomization"));
    }
}
