package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.Route;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Hosts the product management tab sheet.
 *
 * <p>Renders an "All Products" tab backed by {@link ProductListView}, sharing
 * the list toolbar and the injected menu/state services.
 */
@AllArgsConstructor
@Slf4j
@Route("product-tabs")
public class ProductTabs extends VerticalLayout {

    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final RestClientMenuService restClientMenuService;
    private final AccessService accessService;

    private void renderTabSheet() {
        TabSheet tabSheet = new TabSheet();
        Tab browseTab = new Tab();
        browseTab.setLabel(Messages.get("tab.productList"));
        ProductListView productListView = new ProductListView(asyncRestClientMenuService, restClientMenuService,
                accessService, browseTab);
        tabSheet.add(browseTab, productListView);
        tabSheet.setSizeFull();

        add(productListView.getToolbarComponent());
        add(tabSheet);
        setFlexGrow(1, tabSheet);
        setPadding(false);
        setSizeFull();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        renderTabSheet();
    }
}
