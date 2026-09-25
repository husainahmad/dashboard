package com.harmoni.menu.dashboard.layout.organization.tier.service;

import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tab sheet hosting the tier-service browse view. Adds the "All Tier Services"
 * tab with the {@link TierServiceListView} and its toolbar.
 */
@AllArgsConstructor
@Slf4j
public class TierServiceTabs extends VerticalLayout {

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final AccessService accessService;

    /**
     * Renders the tab sheet with the "All Tier Services" tab and the
     * {@link TierServiceListView}. Adds the toolbar above the tab sheet.
     */
    private void renderTabSheet() {
        TabSheet tabSheet = new TabSheet();
        Tab browseTab = new Tab();
        browseTab.setLabel(Messages.get("tab.tierServiceList"));
        TierServiceListView tierServiceListView = new TierServiceListView(asyncRestClientOrganizationService,
                accessService, restClientOrganizationService);
        tabSheet.add(browseTab, tierServiceListView);
        tabSheet.setSizeFull();

        add(tierServiceListView.getToolbarComponent());
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
