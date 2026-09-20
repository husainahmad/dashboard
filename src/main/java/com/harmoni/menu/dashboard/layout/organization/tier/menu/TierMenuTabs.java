package com.harmoni.menu.dashboard.layout.organization.tier.menu;

import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tab sheet hosting the tier-menu browse view. Adds the "All Tier Menus" tab
 * with the {@link TierMenuListView} and its toolbar.
 */
@AllArgsConstructor
@Slf4j
public class TierMenuTabs extends VerticalLayout {

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final AccessService accessService;
    private final RestClientOrganizationService restClientOrganizationService;

    private void renderTabSheet() {
        TabSheet tabSheet = new TabSheet();
        Tab browseTab = new Tab();
        browseTab.setLabel("All Tier Menus");
        TierMenuListView tierMenuListView = new TierMenuListView(asyncRestClientOrganizationService,
                asyncRestClientMenuService, accessService, restClientOrganizationService);
        tabSheet.add(browseTab, tierMenuListView);
        tabSheet.setSizeFull();

        add(tierMenuListView.getToolbarComponent());
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
