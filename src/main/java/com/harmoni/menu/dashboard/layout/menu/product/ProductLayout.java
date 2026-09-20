package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import lombok.AllArgsConstructor;

/**
 * The main layout for the product page.
 *
 * <p>
 * Renders a {@link ProductTabs} component inside a {@link VerticalLayout}.
 * </p>
 */

@AllArgsConstructor
@Route(value = "product", layout = MainLayout.class)
public class ProductLayout extends VerticalLayout {

    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final RestClientMenuService restClientMenuService;
    private final AccessService accessService;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        addClassName("list-view");
        add(new ProductTabs(asyncRestClientMenuService, restClientMenuService, accessService));
        setSizeFull();
    }
}
