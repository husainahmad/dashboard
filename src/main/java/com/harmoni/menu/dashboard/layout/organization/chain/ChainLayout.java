package com.harmoni.menu.dashboard.layout.organization.chain;

import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.AllArgsConstructor;

/**
 * Main route ({@code /chain}) for the chain administration domain. Hosts the
 * {@link ChainTabs} component inside the {@link MainLayout} shell.
 */
@AllArgsConstructor
@Route(value = "chain", layout = MainLayout.class)
@PageTitle(MainLayout.TITLE)
public class ChainLayout extends VerticalLayout {

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final AccessService accessService;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        addClassName("list-view");
        add(new ChainTabs(asyncRestClientOrganizationService,
                restClientOrganizationService, accessService));
        setSizeFull();
    }
}