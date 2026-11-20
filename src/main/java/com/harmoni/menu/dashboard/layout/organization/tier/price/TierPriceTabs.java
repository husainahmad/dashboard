package com.harmoni.menu.dashboard.layout.organization.tier.price;

import com.harmoni.menu.dashboard.layout.AbstractListView;
import com.harmoni.menu.dashboard.layout.component.AbstractListViewTabs;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.AuthorizationService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tab sheet hosting the tier-price browse view. Adds the "All Tier Prices" tab
 * with the {@link TierPriceListView} and its toolbar.
 */
@RequiredArgsConstructor
@Slf4j
public class TierPriceTabs extends AbstractListViewTabs {

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final AccessService accessService;
    private final AuthorizationService authorizationService;

    @Override
    protected String tabLabelKey() {
        return "tab.tierPriceList";
    }

    @Override
    protected String pageTitleKey() {
        return Messages.Keys.PAGE_TIER_PRICE_TITLE;
    }

    @Override
    protected String pageDescriptionKey() {
        return Messages.Keys.PAGE_TIER_PRICE_DESCRIPTION;
    }

    @Override
    protected AbstractListView createListView() {
        return new TierPriceListView(asyncRestClientOrganizationService, restClientOrganizationService, accessService, authorizationService);
    }
}
