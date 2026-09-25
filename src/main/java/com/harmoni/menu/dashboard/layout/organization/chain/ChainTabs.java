package com.harmoni.menu.dashboard.layout.organization.chain;

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
 * Tab sheet hosting the chain browse view. Adds the "All Chains" tab with the
 * {@link ChainListView} and its toolbar.
 */
@AllArgsConstructor
@Slf4j
public class ChainTabs extends VerticalLayout {

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final AccessService accessService;

    /**
     * Renders the tab sheet with the "All Chains" tab and the {@link ChainListView}.
     * Adds the toolbar above the tab sheet.
     */
    private void renderTabSheet() {
        TabSheet tabSheet = new TabSheet();
        Tab browseTab = new Tab();
        browseTab.setLabel(Messages.get("tab.chainList"));
        ChainListView chainListView = new ChainListView(asyncRestClientOrganizationService,
                restClientOrganizationService, accessService);
        tabSheet.add(browseTab, chainListView);
        tabSheet.setSizeFull();

        add(chainListView.getToolbarComponent());
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