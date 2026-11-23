package com.harmoni.menu.dashboard.layout.crm;

import com.harmoni.menu.dashboard.dto.growth.CustomerMetricDto;
import com.harmoni.menu.dashboard.dto.growth.InsightDto;
import com.harmoni.menu.dashboard.dto.growth.SalesSummaryDto;
import com.harmoni.menu.dashboard.layout.component.PageHeader;
import com.harmoni.menu.dashboard.layout.component.StatCard;
import com.harmoni.menu.dashboard.service.data.rest.GrowthRestClientService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.IntegerField;

public class CustomerAnalyticsView extends AbstractGrowthView {

    private final GrowthRestClientService client;
    private final IntegerField customerId = new IntegerField(Messages.get("growth.customerId"));
    private final StatCard orders = new StatCard(VaadinIcon.SHOP, Messages.get("growth.orders"));
    private final StatCard revenue = new StatCard(VaadinIcon.MONEY, Messages.get("growth.revenue"));
    private final StatCard average = new StatCard(VaadinIcon.CHART_LINE, Messages.get("growth.avgOrder"));
    private final Div metrics = new Div();
    private final Grid<InsightDto> insights = new Grid<>(InsightDto.class, false);

    public CustomerAnalyticsView(GrowthRestClientService client) {
        super("customer-analytics-view");
        this.client = client;
        customerId.setValue(1);
        metrics.addClassName("growth-result");

        insights.addColumn(InsightDto::type).setHeader(Messages.get("growth.type"));
        insights.addColumn(InsightDto::title).setHeader(Messages.get("growth.name"));
        insights.addColumn(InsightDto::body).setHeader(Messages.get("growth.result"));

        render(PageHeader.of("page.customerAnalytics.title", "page.customerAnalytics.description"),
                toolbar(customerId,
                        button("growth.sales", VaadinIcon.CHART, event -> loadSales()),
                        button("growth.customer", VaadinIcon.USER, event -> loadCustomer()),
                        button("growth.refresh", VaadinIcon.REFRESH, event -> refreshInsights())),
                cardRow(orders, revenue, average),
                metrics,
                gridSlot(insights));
    }

    private void loadSales() {
        try {
            SalesSummaryDto summary = client.sales();
            orders.setValue((int) summary.orderCount());
            revenue.setValue(summary.totalRevenue().toPlainString());
            average.setValue(summary.avgOrderValue().toPlainString());
        } catch (Exception e) {
            loadError(this::loadSales);
        }
    }

    private void loadCustomer() {
        try {
            CustomerMetricDto metric = client.metrics(customerId.getValue());
            metrics.setText(Messages.get("growth.spend") + ": " + metric.totalSpend()
                    + " | " + Messages.get("growth.visits") + ": " + metric.visitCount()
                    + " | " + Messages.get("growth.segment") + ": " + metric.segment());
            insights.setItems(client.insights(metric.customerId()));
        } catch (Exception e) {
            loadError(this::loadCustomer);
        }
    }

    private void refreshInsights() {
        try {
            insights.setItems(client.refreshInsights(customerId.getValue()));
        } catch (Exception e) {
            actionError(this::refreshInsights);
        }
    }
}
