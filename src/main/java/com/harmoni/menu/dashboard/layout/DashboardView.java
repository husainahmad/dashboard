package com.harmoni.menu.dashboard.layout;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

@Route(value = "", layout = MainLayout.class)
@PageTitle("Dashboard | POSHarmoni")
public class DashboardView extends VerticalLayout {
}
