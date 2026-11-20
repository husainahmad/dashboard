package com.harmoni.menu.dashboard.layout.organization.store.kitchen;

import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.security.RequiresPermission;
import com.harmoni.menu.dashboard.layout.util.Permissions;
import com.harmoni.menu.dashboard.layout.component.PageHeader;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.KitchenRestClientService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.RequiredArgsConstructor;

/**
 * Main route for kitchen printer management under Store Management.
 */
@RequiredArgsConstructor
@RequiresPermission(Permissions.OPERATION_READ)
@Route(value = "kitchen", layout = MainLayout.class)
@PageTitle("Kitchen Printer | POSHarmoni")
public class KitchenLayout extends VerticalLayout {

    private final KitchenRestClientService kitchenRestClientService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final AccessService accessService;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        removeAll();
        addClassName(Css.LIST_VIEW);
        add(PageHeader.of("page.kitchen.title", "page.kitchen.description"));
        add(new KitchenView(kitchenRestClientService, asyncRestClientOrganizationService,
                asyncRestClientMenuService, accessService));
        setSizeFull();
    }
}
