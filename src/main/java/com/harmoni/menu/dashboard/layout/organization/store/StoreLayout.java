package com.harmoni.menu.dashboard.layout.organization.store;

import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import lombok.AllArgsConstructor;

@AllArgsConstructor
@Route(value = "store", layout = MainLayout.class)
public class StoreLayout extends VerticalLayout {

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        add(new StoreTabs(asyncRestClientOrganizationService, restClientOrganizationService));
        setSizeFull();
    }
}
