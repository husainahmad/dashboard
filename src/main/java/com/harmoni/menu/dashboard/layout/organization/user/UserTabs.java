package com.harmoni.menu.dashboard.layout.organization.user;

import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.Route;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tab sheet hosting the user browse view. Adds the "All Users" tab with the
 * {@link UserListView} and its toolbar.
 */
@RequiredArgsConstructor
@Slf4j
@Route("users-tabs")
public class UserTabs extends VerticalLayout {

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final AccessService accessService;

    /**
     * Renders the tab sheet with the "All Users" tab and the
     * {@link UserListView}. Adds the toolbar above the tab sheet.
     */
    private void renderTabSheet() {
        TabSheet tabSheet = new TabSheet();
        Tab browseTab = new Tab();
        browseTab.setLabel(Messages.get("tab.userList"));
        UserListView userListView = new UserListView(asyncRestClientOrganizationService,
                restClientOrganizationService, accessService);
        tabSheet.add(browseTab, userListView);
        tabSheet.setSizeFull();

        add(userListView.getToolbarComponent());
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
