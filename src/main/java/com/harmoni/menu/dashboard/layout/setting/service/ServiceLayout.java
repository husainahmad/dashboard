package com.harmoni.menu.dashboard.layout.setting.service;

import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.security.RequiresPermission;
import com.harmoni.menu.dashboard.layout.util.Permissions;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientSettingService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.RequiredArgsConstructor;

/**
 * Route ({@code /service}) for service administration. Hosts the unified
 * {@link ServiceView} inside the {@link MainLayout} shell.
 */
@RequiredArgsConstructor
@RequiresPermission(Permissions.TERMINAL_VIEW)
@Route(value = "service", layout = MainLayout.class)
@PageTitle("Services | POSHarmoni")
public class ServiceLayout extends VerticalLayout {

    private final AsyncRestClientSettingService asyncRestClientSettingService;
    private final RestClientMenuService restClientMenuService;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        addClassName(Css.LIST_VIEW);
        add(new ServiceView(asyncRestClientSettingService, restClientMenuService));
        setSizeFull();
    }
}
