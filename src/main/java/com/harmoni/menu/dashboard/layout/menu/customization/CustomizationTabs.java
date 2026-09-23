package com.harmoni.menu.dashboard.layout.menu.customization;

import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tab sheet host for the customization page.
 *
 * <p>
 * Adds a single "All Customizations" tab holding a {@link CustomizationListView}
 * and places the list's toolbar above the sheet.
 * </p>
 */
@AllArgsConstructor
@Slf4j
public class CustomizationTabs extends VerticalLayout {

    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final RestClientMenuService restClientMenuService;
    private final AccessService accessService;

    private void renderTabSheet() {
        TabSheet tabSheet = new TabSheet();
        Tab browseTab = new Tab();
        browseTab.setLabel(Messages.get("tab.customizationList"));
        CustomizationListView customizationListView = new CustomizationListView(
                asyncRestClientMenuService, restClientMenuService, accessService);
        tabSheet.add(browseTab, customizationListView);
        tabSheet.setSizeFull();

        add(customizationListView.getToolbarComponent());
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
