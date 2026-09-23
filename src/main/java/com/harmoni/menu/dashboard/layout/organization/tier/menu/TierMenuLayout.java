package com.harmoni.menu.dashboard.layout.organization.tier.menu;

import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.AllArgsConstructor;
import com.harmoni.menu.dashboard.layout.util.Css;

/**
 * Main route ({@code /tier-menu}) for the tier-menu administration domain.
 * Hosts the {@link TierMenuTabs} component inside the {@link MainLayout} shell.
 */
@AllArgsConstructor
@Route(value = "tier-menu", layout = MainLayout.class)
@PageTitle("Tier Menu | POSHarmoni")
public class TierMenuLayout extends VerticalLayout {

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final AccessService accessService;
    private final RestClientOrganizationService restClientOrganizationService;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        addClassName(Css.LIST_VIEW);
        add(new TierMenuTabs(asyncRestClientOrganizationService, asyncRestClientMenuService,
                accessService, restClientOrganizationService));
        setSizeFull();
    }
}
