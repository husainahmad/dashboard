package com.harmoni.menu.dashboard.layout.setting.service;

import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientSettingService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tab sheet for the service setting, hosting the {@link ServiceListView} as
 * its "All Services" tab together with the shared toolbar. Delegates all data
 * access to the injected {@link AsyncRestClientSettingService}.
 */
@AllArgsConstructor
@Slf4j
public class ServiceTabs extends VerticalLayout {

    private final AsyncRestClientSettingService asyncRestClientSettingService;

    /**
     * Renders the tab sheet with a single "All Services" tab and the list's
     * toolbar above it.
     */
    private void renderTabSheet() {
        TabSheet tabSheet = new TabSheet();
        Tab browseTab = new Tab();
        browseTab.setLabel("All Services");
        ServiceListView serviceListView = new ServiceListView(asyncRestClientSettingService);
        tabSheet.add(browseTab, serviceListView);
        tabSheet.setSizeFull();

        add(serviceListView.getToolbarComponent());
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