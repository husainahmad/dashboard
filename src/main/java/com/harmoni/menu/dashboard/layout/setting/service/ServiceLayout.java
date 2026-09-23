package com.harmoni.menu.dashboard.layout.setting.service;

import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientSettingService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.AllArgsConstructor;
import com.harmoni.menu.dashboard.layout.util.Css;

/**
 * Setting view for services at {@code /service}. Hosts the {@link ServiceTabs}
 * tab sheet and provides the {@link AsyncRestClientSettingService} used to
 * fetch service data.
 */
@AllArgsConstructor
@Route(value = "service", layout = MainLayout.class)
@PageTitle("Service | POSHarmoni")
public class ServiceLayout extends VerticalLayout {

    private final AsyncRestClientSettingService asyncRestClientSettingService;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        addClassName(Css.LIST_VIEW);
        add(new ServiceTabs(asyncRestClientSettingService));
        setSizeFull();
    }
}