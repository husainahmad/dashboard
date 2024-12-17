package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.rest.data.RestClientMenuService;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;

@Route(value = "product", layout = MainLayout.class)
public class ProductLayout extends VerticalLayout {

    public ProductLayout(AsyncRestClientMenuService asyncRestClientMenuService,
                         RestClientMenuService restClientMenuService) {
        add(new ProductTabs(asyncRestClientMenuService, restClientMenuService));
        setSizeFull();
    }
}
