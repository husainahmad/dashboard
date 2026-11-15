package com.harmoni.menu.dashboard.layout.menu.category;

import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.RequiredArgsConstructor;

/**
 * Route ({@code /category}) for category administration. Hosts the unified
 * {@link CategoryView} inside the {@link MainLayout} shell.
 */
@RequiredArgsConstructor
@Route(value = "category", layout = MainLayout.class)
@PageTitle("Categories | POSHarmoni")
public class CategoryLayout extends VerticalLayout {

    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientMenuService restClientMenuService;
    private final AccessService accessService;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        addClassName(Css.LIST_VIEW);
        add(new CategoryView(asyncRestClientMenuService,
                asyncRestClientOrganizationService, restClientMenuService, accessService));
        setSizeFull();
    }
}
