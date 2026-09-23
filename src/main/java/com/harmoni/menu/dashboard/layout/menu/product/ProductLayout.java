package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import lombok.AllArgsConstructor;
import com.harmoni.menu.dashboard.layout.util.Css;

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
        addClassName(Css.LIST_VIEW);
        add(new ProductTabs(asyncRestClientMenuService, restClientMenuService, accessService));
        setSizeFull();
    }
}
