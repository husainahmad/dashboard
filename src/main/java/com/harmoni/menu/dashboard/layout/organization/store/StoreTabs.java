package com.harmoni.menu.dashboard.layout.organization.store;

import com.harmoni.menu.dashboard.layout.AbstractListView;
import com.harmoni.menu.dashboard.layout.component.AbstractListViewTabs;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
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
public class StoreTabs extends AbstractListViewTabs {

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final AccessService accessService;

    @Override
    protected String tabLabelKey() {
        return "tab.storeList";
    }

    @Override
    protected String pageTitleKey() {
        return Messages.Keys.PAGE_STORE_TITLE;
    }

    @Override
    protected String pageDescriptionKey() {
        return Messages.Keys.PAGE_STORE_DESCRIPTION;
    }

    @Override
    protected AbstractListView createListView() {
        return new StoreListView(asyncRestClientOrganizationService, restClientOrganizationService, accessService);
    }
}
