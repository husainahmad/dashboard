package com.harmoni.menu.dashboard.layout.organization.user;

import com.harmoni.menu.dashboard.layout.AbstractListView;
import com.harmoni.menu.dashboard.layout.component.AbstractListViewTabs;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
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
public class UserTabs extends AbstractListViewTabs {

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final AccessService accessService;

    @Override
    protected String tabLabelKey() {
        return "tab.userList";
    }

    @Override
    protected String pageTitleKey() {
        return Messages.Keys.PAGE_USER_TITLE;
    }

    @Override
    protected String pageDescriptionKey() {
        return Messages.Keys.PAGE_USER_DESCRIPTION;
    }

    @Override
    protected AbstractListView createListView() {
        return new UserListView(asyncRestClientOrganizationService, restClientOrganizationService, accessService);
    }
}
