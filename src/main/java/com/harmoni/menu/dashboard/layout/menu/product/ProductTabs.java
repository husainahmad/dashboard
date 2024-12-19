package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.rest.data.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.rest.data.RestClientMenuService;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Route("product-tabs")
public class ProductTabs extends VerticalLayout {

    public ProductTabs(AsyncRestClientMenuService asyncRestClientMenuService,
                       RestClientMenuService restClientMenuService) {
        TabSheet tabSheet = new TabSheet();
        Tab browseTab = new Tab();
        browseTab.setLabel("Browse");
        tabSheet.add(browseTab, new ProductListView(asyncRestClientMenuService, restClientMenuService, browseTab));
        tabSheet.setSizeFull();
        add(tabSheet);
        setSizeFull();
    }
}
