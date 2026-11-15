package com.harmoni.menu.dashboard.layout.customer;

import com.harmoni.menu.dashboard.layout.AbstractListView;
import com.harmoni.menu.dashboard.layout.component.AbstractListViewTabs;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientCustomerService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.router.Route;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tab sheet hosting the customer browse view. Adds the "All Customers" tab
 * with the {@link CustomerListView} and its toolbar; customer details open as
 * additional tabs through {@link com.harmoni.menu.dashboard.layout.component.TabManager}.
 */
@RequiredArgsConstructor
@Slf4j
@Route("customer-tabs")
public class CustomerTabs extends AbstractListViewTabs {

    private final AsyncRestClientCustomerService asyncRestClientCustomerService;

    @Override
    protected String tabLabelKey() {
        return "tab.customerList";
    }

    @Override
    protected String pageTitleKey() {
        return Messages.Keys.PAGE_CUSTOMER_TITLE;
    }

    @Override
    protected String pageDescriptionKey() {
        return Messages.Keys.PAGE_CUSTOMER_DESCRIPTION;
    }

    @Override
    protected AbstractListView createListView() {
        return new CustomerListView(asyncRestClientCustomerService);
    }
}
