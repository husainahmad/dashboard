package com.harmoni.menu.dashboard.layout.organization.chain;

import com.harmoni.menu.dashboard.layout.AbstractListView;
import com.harmoni.menu.dashboard.layout.component.AbstractListViewTabs;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tab sheet hosting the chain browse view. Adds the "All Chains" tab with the
 * {@link ChainListView} and its toolbar.
 */
@RequiredArgsConstructor
@Slf4j
public class ChainTabs extends AbstractListViewTabs {

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final AccessService accessService;

    @Override
    protected String tabLabelKey() {
        return "tab.chainList";
    }

    @Override
    protected String pageTitleKey() {
        return Messages.Keys.PAGE_CHAIN_TITLE;
    }

    @Override
    protected String pageDescriptionKey() {
        return Messages.Keys.PAGE_CHAIN_DESCRIPTION;
    }

    @Override
    protected AbstractListView createListView() {
        return new ChainListView(asyncRestClientOrganizationService, restClientOrganizationService, accessService);
    }
}
