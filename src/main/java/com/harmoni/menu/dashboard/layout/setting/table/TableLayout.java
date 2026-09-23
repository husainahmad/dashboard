package com.harmoni.menu.dashboard.layout.setting.table;

import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientSettingService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientSettingService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.AllArgsConstructor;
import com.harmoni.menu.dashboard.layout.util.Css;

/**
 * Setting view for tables at {@code /table}. Hosts the {@link TableTabs} tab
 * sheet and hands it the async and blocking REST clients, plus the
 * {@link AccessService} used to scope tables to the current store.
 */
@AllArgsConstructor
@Route(value = "table", layout = MainLayout.class)
@PageTitle("Table | POSHarmoni")
public class TableLayout extends VerticalLayout {

    private final AsyncRestClientSettingService asyncRestClientSettingService;
    private final RestClientSettingService restClientSettingService;
    private final AccessService accessService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        addClassName(Css.LIST_VIEW);
        add(new TableTabs(asyncRestClientSettingService, restClientSettingService, accessService,
                asyncRestClientOrganizationService));
        setSizeFull();
    }
}
