package com.harmoni.menu.dashboard.layout.organization.tier.menu;

import com.harmoni.menu.dashboard.layout.AbstractListView;
import com.harmoni.menu.dashboard.layout.component.AbstractListViewTabs;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tab sheet hosting the tier-menu browse view. Adds the "All Tier Menus" tab
 * with the {@link TierMenuListView} and its toolbar.
 */
@RequiredArgsConstructor
@Slf4j
public class TierMenuTabs extends AbstractListViewTabs {

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final AccessService accessService;
    private final RestClientOrganizationService restClientOrganizationService;

    @Override
    protected String tabLabelKey() {
        return "tab.tierMenuList";
    }

    @Override
    protected String pageTitleKey() {
        return Messages.Keys.PAGE_TIER_MENU_TITLE;
    }

    @Override
    protected String pageDescriptionKey() {
        return Messages.Keys.PAGE_TIER_MENU_DESCRIPTION;
    }

    @Override
    protected AbstractListView createListView() {
        return new TierMenuListView(asyncRestClientOrganizationService, asyncRestClientMenuService,
                accessService, restClientOrganizationService);
    }
}
