package com.harmoni.menu.dashboard.layout.customer;

import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientCustomerService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import lombok.AllArgsConstructor;

/**
 * Main route ({@code /customers}) for the customer domain. Hosts the
 * {@link CustomerTabs} component inside the {@link MainLayout} shell.
 */
@AllArgsConstructor
@Route(value = "customers", layout = MainLayout.class)
public class CustomerLayout extends VerticalLayout {

    private final AsyncRestClientCustomerService asyncRestClientCustomerService;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        addClassName(Css.LIST_VIEW);
        add(new CustomerTabs(asyncRestClientCustomerService));
        setSizeFull();
    }
}
