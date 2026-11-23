package com.harmoni.menu.dashboard.layout.crm;

import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.security.RequiresPermission;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.layout.util.Permissions;
import com.harmoni.menu.dashboard.service.data.rest.GrowthRestClientService;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@RequiresPermission(Permissions.CUSTOMER_READ)
@Route(value = "recommendations", layout = MainLayout.class)
@PageTitle("Recommendations | POSHarmoni")
public class RecommendationsLayout extends VerticalLayout {

    public RecommendationsLayout(GrowthRestClientService client) {
        addClassName(Css.LIST_VIEW);
        setSizeFull();
        add(new RecommendationsView(client));
    }
}
