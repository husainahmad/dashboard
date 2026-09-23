package com.harmoni.menu.dashboard.layout.menu.category;

import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.AllArgsConstructor;
import com.harmoni.menu.dashboard.layout.util.Css;

/**
 * Main route for the category administration page.
 *
 * <p>
 * Embeds a single {@link CategoryTabs} tab sheet inside the shared
 * {@link MainLayout}; the page title is {@code Category | POSHarmoni} and all
 * data access flows through the injected (async) menu and organization REST
 * clients plus {@link AccessService}.
 * </p>
 */
@AllArgsConstructor
@Route(value = "category", layout = MainLayout.class)
@PageTitle("Category | POSHarmoni")
public class CategoryLayout extends VerticalLayout {

    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientMenuService restClientMenuService;
    private final AccessService accessService;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        addClassName(Css.LIST_VIEW);
        add(new CategoryTabs(asyncRestClientMenuService,
                asyncRestClientOrganizationService, restClientMenuService, accessService));
        setSizeFull();
    }
}