package com.harmoni.menu.dashboard.layout.organization.tier.price;

import com.harmoni.menu.dashboard.layout.util.Permissions;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.support.UiTestSupport;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.button.Button;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class TierPriceListPermissionTest {

    @Test
    void newTierIsHiddenWithoutOrganizationWrite() {
        Button add = addButton(Permissions.ORGANIZATION_READ);

        assertNotNull(add);
        assertFalse(add.isVisible());
    }

    @Test
    void newTierIsShownWithOrganizationWrite() {
        Button add = addButton(Permissions.ORGANIZATION_WRITE);

        assertNotNull(add);
        assertTrue(add.isVisible());
    }

    private static Button addButton(String... permissions) {
        TierPriceListView view = new TierPriceListView(
                mock(AsyncRestClientOrganizationService.class),
                mock(RestClientOrganizationService.class),
                mock(AccessService.class),
                UiTestSupport.authorization(permissions));
        return UiTestSupport.buttonByText(view.getToolbarComponent(), Messages.get("action.newTier"));
    }
}
