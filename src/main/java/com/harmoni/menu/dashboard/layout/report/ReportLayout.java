package com.harmoni.menu.dashboard.layout.report;

import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.ai.AiContextProvider;
import com.harmoni.menu.dashboard.layout.ai.AiPageContext;
import com.harmoni.menu.dashboard.layout.component.PageHeader;
import com.harmoni.menu.dashboard.layout.security.RequiresPermission;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.layout.util.Permissions;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientReportService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Route ({@code /report}) that hosts the five order-service reports in one
 * tabbed page, with a shared Brand/Chain/Store and date-range filter.
 */
@RequiredArgsConstructor
@Route(value = "report", layout = MainLayout.class)
@PageTitle("Report | POSHarmoni")
@RequiresPermission(Permissions.REPORT_VIEW)
public class ReportLayout extends VerticalLayout implements AiContextProvider {

    private final AsyncRestClientReportService reportService;
    private final AsyncRestClientOrganizationService organizationService;
    private final AccessService accessService;

    private final List<AbstractReportView> views = new ArrayList<>();

    private AbstractReportView activeView;
    private boolean built;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (built) {
            return;
        }
        built = true;
        addClassName(Css.LIST_VIEW);
        setSizeFull();
        setPadding(false);

        SettlementReportView settlement = new SettlementReportView(reportService);
        TopProductReportView topProducts = new TopProductReportView(reportService);
        DailyReportView daily = new DailyReportView(reportService);
        SalesReportView sales = new SalesReportView(reportService);
        OrderVolumeReportView orderVolume = new OrderVolumeReportView(reportService);
        views.addAll(List.of(settlement, topProducts, daily, sales, orderVolume));

        TabSheet tabSheet = new TabSheet();
        tabSheet.add(new Tab(Messages.get("nav.report.settlement")), settlement);
        tabSheet.add(new Tab(Messages.get("nav.report.topProducts")), topProducts);
        tabSheet.add(new Tab(Messages.get("nav.report.daily")), daily);
        tabSheet.add(new Tab(Messages.get("nav.report.sales")), sales);
        tabSheet.add(new Tab(Messages.get("nav.report.orderVolume")), orderVolume);
        tabSheet.setSizeFull();
        activeView = settlement;
        tabSheet.addSelectedChangeListener(event -> {
            int index = tabSheet.getSelectedIndex();
            if (index >= 0 && index < views.size()) {
                activeView = views.get(index);
            }
        });

        ReportFilterBar filterBar = new ReportFilterBar(this::applyFilter);

        add(PageHeader.of("page.report.title", "page.report.description"), filterBar, tabSheet);
        setFlexGrow(1, tabSheet);

        loadOrganization(filterBar);
    }

    private void loadOrganization(ReportFilterBar filterBar) {
        organizationService.getOrganizationTreeAsync(
                tree -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    filterBar.setOrganization(tree, accessService.getStoreId());
                    filterBar.apply();
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null), filterBar::apply));
    }

    private void applyFilter(DateRange range, List<Integer> storeIds) {
        views.forEach(view -> view.applyFilter(range, storeIds));
    }

    @Override
    public AiPageContext aiPageContext() {
        return activeView == null ? null : activeView.aiPageContext();
    }
}
