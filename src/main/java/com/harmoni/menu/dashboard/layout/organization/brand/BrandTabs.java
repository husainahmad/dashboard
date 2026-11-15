package com.harmoni.menu.dashboard.layout.organization.brand;

import com.harmoni.menu.dashboard.layout.AbstractListView;
import com.harmoni.menu.dashboard.layout.component.AbstractListViewTabs;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tab sheet hosting the brand browse view. Adds the "All Brands" tab with the
 * {@link BrandListView} and its toolbar.
 */
@RequiredArgsConstructor
@Slf4j
public class BrandTabs extends AbstractListViewTabs {

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;

    @Override
    protected String tabLabelKey() {
        return "tab.brandList";
    }

    @Override
    protected String pageTitleKey() {
        return Messages.Keys.PAGE_BRAND_TITLE;
    }

    @Override
    protected String pageDescriptionKey() {
        return Messages.Keys.PAGE_BRAND_DESCRIPTION;
    }

    @Override
    protected AbstractListView createListView() {
        return new BrandListView(asyncRestClientOrganizationService, restClientOrganizationService);
    }
}
