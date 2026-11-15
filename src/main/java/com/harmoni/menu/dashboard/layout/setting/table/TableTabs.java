package com.harmoni.menu.dashboard.layout.setting.table;

import com.harmoni.menu.dashboard.layout.component.PageHeader;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientSettingService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientSettingService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tab sheet for the table setting, hosting the floor plan and the table list.
 * The plan is the first tab because arranging tables is the page's primary
 * job; the list tab carries its own two-row toolbar. Delegates data access to
 * the async client and persistence to the blocking
 * {@link RestClientSettingService}.
 */
@RequiredArgsConstructor
@Slf4j
public class TableTabs extends VerticalLayout {

    private final AsyncRestClientSettingService asyncRestClientSettingService;
    private final RestClientSettingService restClientSettingService;
    private final AccessService accessService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;

    /**
     * Renders the tab sheet with a single "All Tables" tab and the list's
     * toolbar above it.
     */
    private void renderTabSheet() {
        TabSheet tabSheet = new TabSheet();
        tabSheet.addClassName("table-tabs");

        Tab planTab = tab(Messages.get("tab.floorPlan"), VaadinIcon.GRID.create());
        FloorPlanView floorPlanView = new FloorPlanView(
                asyncRestClientSettingService, restClientSettingService, accessService,
                asyncRestClientOrganizationService);
        tabSheet.add(planTab, floorPlanView);

        Tab browseTab = tab(Messages.get("tab.tableList"), VaadinIcon.TABLE.create());
        TableListView tableListView = new TableListView(
                asyncRestClientSettingService, restClientSettingService, accessService,
                asyncRestClientOrganizationService);
        VerticalLayout listPanel = new VerticalLayout(tableListView.getToolbarComponent(), tableListView);
        listPanel.addClassName("table-list-panel");
        listPanel.setPadding(false);
        listPanel.setSpacing(true);
        listPanel.setSizeFull();
        listPanel.setFlexGrow(1, tableListView);
        tabSheet.add(browseTab, listPanel);

        tabSheet.setSizeFull();
        tabSheet.addClassName("table-workspace");

        add(PageHeader.of(Messages.Keys.PAGE_TABLE_TITLE, Messages.Keys.PAGE_TABLE_DESCRIPTION));
        add(tabSheet);
        setFlexGrow(1, tabSheet);
        setPadding(false);
        setSpacing(true);
        setSizeFull();
    }

    /**
     * Builds a tab whose label sits beside an icon, so the two views of this
     * page read as a plan and a list at a glance.
     *
     * @param label the tab caption
     * @param icon  the icon shown before the caption
     * @return the tab
     */
    private static Tab tab(String label, Icon icon) {
        icon.addClassName("table-tab-icon");
        Tab tab = new Tab(icon, new Span(label));
        tab.addClassName("table-tab");
        return tab;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        renderTabSheet();
    }
}
