package com.harmoni.menu.dashboard.layout.setting.service;

import com.harmoni.menu.dashboard.layout.AbstractListView;
import com.harmoni.menu.dashboard.layout.component.AbstractListViewTabs;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientSettingService;
import com.harmoni.menu.dashboard.util.Messages;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Tab sheet for the service setting, hosting the {@link ServiceListView} as
 * its "All Services" tab together with the shared toolbar. Delegates all data
 * access to the injected {@link AsyncRestClientSettingService}.
 */
@RequiredArgsConstructor
@Slf4j
public class ServiceTabs extends AbstractListViewTabs {

    private final AsyncRestClientSettingService asyncRestClientSettingService;

    @Override
    protected String tabLabelKey() {
        return "tab.serviceList";
    }

    @Override
    protected String pageTitleKey() {
        return Messages.Keys.PAGE_SERVICE_TITLE;
    }

    @Override
    protected String pageDescriptionKey() {
        return Messages.Keys.PAGE_SERVICE_DESCRIPTION;
    }

    @Override
    protected AbstractListView createListView() {
        return new ServiceListView(asyncRestClientSettingService);
    }
}
