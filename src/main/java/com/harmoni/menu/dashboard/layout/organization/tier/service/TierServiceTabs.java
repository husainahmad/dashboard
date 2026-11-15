package com.harmoni.menu.dashboard.layout.organization.tier.service;

import com.harmoni.menu.dashboard.layout.AbstractListView;
import com.harmoni.menu.dashboard.layout.component.AbstractListViewTabs;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tab sheet hosting the tier-service browse view. Adds the "All Tier Services"
 * tab with the {@link TierServiceListView} and its toolbar.
 */
@RequiredArgsConstructor
@Slf4j
public class TierServiceTabs extends AbstractListViewTabs {

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final AccessService accessService;

    @Override
    protected String tabLabelKey() {
        return "tab.tierServiceList";
    }

    @Override
    protected String pageTitleKey() {
        return Messages.Keys.PAGE_TIER_SERVICE_TITLE;
    }

    @Override
    protected String pageDescriptionKey() {
        return Messages.Keys.PAGE_TIER_SERVICE_DESCRIPTION;
    }

    @Override
    protected AbstractListView createListView() {
        return new TierServiceListView(asyncRestClientOrganizationService, accessService, restClientOrganizationService);
    }
}
