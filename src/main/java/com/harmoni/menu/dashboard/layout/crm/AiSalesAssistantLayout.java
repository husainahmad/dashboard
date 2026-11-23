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
@Route(value = "ai-sales-assistant", layout = MainLayout.class)
@PageTitle("AI Sales Assistant | POSHarmoni")
public class AiSalesAssistantLayout extends VerticalLayout {

    public AiSalesAssistantLayout(GrowthRestClientService client) {
        addClassName(Css.LIST_VIEW);
        setSizeFull();
        add(new AiSalesAssistantView(client));
    }
}
