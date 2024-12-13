package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.rest.data.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.rest.data.RestClientMenuService;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.Route;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@Route("product-tabs")
public class ProductTabs extends VerticalLayout {

    public ProductTabs(AsyncRestClientMenuService asyncRestClientMenuService,
                       RestClientMenuService restClientMenuService) {
        TabSheet tabSheet = new TabSheet();
        tabSheet.add("Browse", new ProductListView(asyncRestClientMenuService, restClientMenuService));
        tabSheet.setSizeFull();
        add(tabSheet);
        setSizeFull();
    }
}
