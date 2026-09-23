package com.harmoni.menu.dashboard.layout.organization.tier.price;

import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.AllArgsConstructor;
import com.harmoni.menu.dashboard.layout.util.Css;

/**
 * Main route ({@code /tier-price}) for the tier-price administration domain.
 * Hosts the {@link TierPriceTabs} component inside the {@link MainLayout} shell.
 */
@AllArgsConstructor
@Route(value = "tier-price", layout = MainLayout.class)
@PageTitle("Tier Price | POSHarmoni")
public class TierPriceLayout extends VerticalLayout {

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final AccessService accessService;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        addClassName(Css.LIST_VIEW);
        add(new TierPriceTabs(asyncRestClientOrganizationService, restClientOrganizationService, accessService));
        setSizeFull();
    }
}
