package com.harmoni.menu.dashboard.layout.customer;

import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientCustomerService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
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
public class CustomerTabs extends VerticalLayout {

    private final AsyncRestClientCustomerService asyncRestClientCustomerService;

    /**
     * Renders the tab sheet with the "All Customers" tab and the
     * {@link CustomerListView}. Adds the toolbar above the tab sheet.
     */
    private void renderTabSheet() {
        TabSheet tabSheet = new TabSheet();
        Tab browseTab = new Tab();
        browseTab.setLabel(Messages.get("tab.customerList"));
        CustomerListView customerListView = new CustomerListView(asyncRestClientCustomerService);
        tabSheet.add(browseTab, customerListView);
        tabSheet.setSizeFull();

        add(customerListView.getToolbarComponent());
        add(tabSheet);
        setFlexGrow(1, tabSheet);
        setPadding(false);
        setSizeFull();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        renderTabSheet();
    }
}
