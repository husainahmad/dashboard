package com.harmoni.menu.dashboard.layout.report;

import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.security.RequiresPermission;
import com.harmoni.menu.dashboard.layout.util.Permissions;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteAlias;

/**
 * Keeps the old per-report URLs working by forwarding them to the unified
 * {@link ReportLayout} page.
 */
@Route(value = "report/settlement", layout = MainLayout.class)
@RouteAlias(value = "report/top-products", layout = MainLayout.class)
@RouteAlias(value = "report/daily", layout = MainLayout.class)
@RouteAlias(value = "report/sales", layout = MainLayout.class)
@RouteAlias(value = "report/order-volume", layout = MainLayout.class)
@RequiresPermission(Permissions.REPORT_VIEW)
public class ReportRedirectView extends VerticalLayout implements BeforeEnterObserver {

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        event.forwardTo(ReportLayout.class);
    }
}
