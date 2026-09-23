package com.harmoni.menu.dashboard.layout.organization.store;

import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.Route;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tab sheet hosting the store browse view. Adds the "All Stores" tab with the
 * {@link StoreListView} and its toolbar.
 */
@RequiredArgsConstructor
@Slf4j
@Route("store-tabs")
public class StoreTabs extends VerticalLayout {

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final AccessService accessService;

    private void renderTabSheet() {
        TabSheet tabSheet = new TabSheet();
        Tab browseTab = new Tab();
        browseTab.setLabel(Messages.get("tab.storeList"));
        StoreListView storeListView = new StoreListView(asyncRestClientOrganizationService,
                restClientOrganizationService, accessService);
        tabSheet.add(browseTab, storeListView);
        tabSheet.setSizeFull();

        add(storeListView.getToolbarComponent());
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
