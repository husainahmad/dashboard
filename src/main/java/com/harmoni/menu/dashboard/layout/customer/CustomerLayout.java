package com.harmoni.menu.dashboard.layout.customer;

import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.ai.AiContextProvider;
import com.harmoni.menu.dashboard.layout.ai.AiPageContext;
import com.harmoni.menu.dashboard.layout.security.RequiresPermission;
import com.harmoni.menu.dashboard.layout.util.Permissions;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientCustomerService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.RequiredArgsConstructor;

/**
 * Route ({@code /customers}) for the customer domain. Hosts the unified
 * {@link CustomerView} inside the {@link MainLayout} shell.
 */
@RequiredArgsConstructor
@RequiresPermission(Permissions.CUSTOMER_READ)
@Route(value = "customers", layout = MainLayout.class)
@PageTitle("Customers | POSHarmoni")
public class CustomerLayout extends VerticalLayout implements AiContextProvider {

    private final AsyncRestClientCustomerService asyncRestClientCustomerService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final AccessService accessService;

    private CustomerView currentView;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        removeAll();
        addClassName(Css.LIST_VIEW);
        currentView = new CustomerView(asyncRestClientCustomerService,
                asyncRestClientOrganizationService, accessService);
        add(currentView);
        setSizeFull();
    }

    @Override
    public AiPageContext aiPageContext() {
        return currentView == null ? null : currentView.aiPageContext();
    }
}
