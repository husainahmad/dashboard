package com.harmoni.menu.dashboard.layout.organization.brand;

import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import lombok.RequiredArgsConstructor;
import com.harmoni.menu.dashboard.layout.util.Css;

/**
 * Main route ({@code /brand}) for the brand administration domain. Hosts the
 * {@link BrandTabs} component inside the {@link MainLayout} shell.
 */
@RequiredArgsConstructor
@PageTitle("Brand | POSHarmoni")
public class BrandLayout extends VerticalLayout {

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        addClassName(Css.LIST_VIEW);
        add(new BrandTabs(asyncRestClientOrganizationService, restClientOrganizationService));
        setSizeFull();
    }
}