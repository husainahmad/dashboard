package com.harmoni.menu.dashboard.layout.organization.store;

import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import lombok.AllArgsConstructor;
import com.harmoni.menu.dashboard.layout.util.Css;

/**
 * Main route ({@code /store}) for the store administration domain. Hosts the
 * {@link StoreTabs} component inside the {@link MainLayout} shell.
 */
@AllArgsConstructor
@Route(value = "store", layout = MainLayout.class)
public class StoreLayout extends VerticalLayout {

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final AccessService accessService;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        addClassName(Css.LIST_VIEW);
        add(new StoreTabs(asyncRestClientOrganizationService, restClientOrganizationService, accessService));
        setSizeFull();
    }
}
