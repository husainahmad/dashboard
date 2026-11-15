package com.harmoni.menu.dashboard.layout.menu.promotion;

import com.harmoni.menu.dashboard.layout.AbstractListView;
import com.harmoni.menu.dashboard.layout.component.AbstractListViewTabs;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientPromotionService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientPromotionService;
import com.harmoni.menu.dashboard.util.Messages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tab sheet host for the promotion page. The list toolbar sits above the sheet so it
 * stays visible while an operator edits a promotion in a second tab.
 */
@RequiredArgsConstructor
@Slf4j
public class PromotionTabs extends AbstractListViewTabs {

    private final AsyncRestClientPromotionService asyncRestClientPromotionService;
    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientPromotionService restClientPromotionService;
    private final AccessService accessService;

    @Override
    protected String tabLabelKey() {
        return "tab.promotionList";
    }

    @Override
    protected String pageTitleKey() {
        return Messages.Keys.PAGE_PROMOTION_TITLE;
    }

    @Override
    protected String pageDescriptionKey() {
        return Messages.Keys.PAGE_PROMOTION_DESCRIPTION;
    }

    @Override
    protected AbstractListView createListView() {
        return new PromotionListView(asyncRestClientPromotionService, asyncRestClientMenuService,
                asyncRestClientOrganizationService, restClientPromotionService, accessService);
    }
}
