package com.harmoni.menu.dashboard.layout.menu.customization;

import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.AllArgsConstructor;
import com.harmoni.menu.dashboard.layout.util.Css;

/**
 * Main route for the customization administration page.
 *
 * <p>
 * Embeds a single {@link CustomizationTabs} tab sheet inside the shared
 * {@link MainLayout}; the page title is {@code Customization | POSHarmoni} and
 * all data access flows through the injected (async) menu REST clients plus
 * {@link AccessService}.
 * </p>
 */
@AllArgsConstructor
@Route(value = "customization", layout = MainLayout.class)
@PageTitle("Customization | POSHarmoni")
public class CustomizationLayout extends VerticalLayout {

    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final RestClientMenuService restClientMenuService;
    private final AccessService accessService;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        addClassName(Css.LIST_VIEW);
        add(new CustomizationTabs(asyncRestClientMenuService, restClientMenuService, accessService));
        setSizeFull();
    }
}
