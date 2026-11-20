package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.layout.util.Permissions;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.support.UiTestSupport;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.button.Button;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class ProductListPermissionTest {

    @Test
    void newProductButtonIsHiddenWithoutEditPermission() {
        ProductListView view = view(Permissions.MENU_VIEW);

        Button add = UiTestSupport.buttonByText(
                view.getToolbarComponent(), Messages.get(Messages.Keys.ACTION_NEW_PRODUCT));

        assertNotNull(add);
        assertFalse(add.isVisible());
    }

    @Test
    void newProductButtonIsShownWithEditPermission() {
        ProductListView view = view(Permissions.MENU_EDIT);

        Button add = UiTestSupport.buttonByText(
                view.getToolbarComponent(), Messages.get(Messages.Keys.ACTION_NEW_PRODUCT));

        assertNotNull(add);
        assertTrue(add.isVisible());
    }

    @Test
    void priceCellIsReadOnlyWithoutPricePermission() {
        ProductTreeItem item = ProductTreeItem.builder()
                .id("sku:1")
                .skuId(1)
                .productItemType(com.harmoni.menu.dashboard.layout.enums.ProductItemType.SKU)
                .build();
        TierPriceCell cell = new TierPriceCell(item, 1,
                mock(RestClientMenuService.class), null, new java.util.HashSet<>(), false);

        assertTrue(cell.isReadOnly());
    }

    private static ProductListView view(String... permissions) {
        return new ProductListView(mock(AsyncRestClientMenuService.class),
                mock(RestClientMenuService.class), UiTestSupport.authorization(permissions));
    }
}
