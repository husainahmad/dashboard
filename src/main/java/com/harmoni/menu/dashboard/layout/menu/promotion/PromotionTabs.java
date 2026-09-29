package com.harmoni.menu.dashboard.layout.menu.promotion;

import com.harmoni.menu.dashboard.layout.component.PageHeader;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientPromotionService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientPromotionService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tab sheet host for the promotion page. The list toolbar sits above the sheet so it
 * stays visible while an operator edits a promotion in a second tab.
 */
@AllArgsConstructor
@Slf4j
public class PromotionTabs extends VerticalLayout {

    private final AsyncRestClientPromotionService asyncRestClientPromotionService;
    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientPromotionService restClientPromotionService;
    private final AccessService accessService;

    private void renderTabSheet() {
        TabSheet tabSheet = new TabSheet();
        Tab browseTab = new Tab();
        browseTab.setLabel(Messages.get("tab.promotionList"));
        PromotionListView promotionListView = new PromotionListView(asyncRestClientPromotionService,
                asyncRestClientMenuService, asyncRestClientOrganizationService, restClientPromotionService,
                accessService);
        tabSheet.add(browseTab, promotionListView);
        tabSheet.setSizeFull();

        add(PageHeader.of(Messages.Keys.PAGE_PROMOTION_TITLE, Messages.Keys.PAGE_PROMOTION_DESCRIPTION));
        add(promotionListView.getToolbarComponent());
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
