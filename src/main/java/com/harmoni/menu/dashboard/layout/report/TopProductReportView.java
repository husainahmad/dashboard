package com.harmoni.menu.dashboard.layout.report;

import com.harmoni.menu.dashboard.dto.report.TopProductReportDto;
import com.harmoni.menu.dashboard.layout.util.GridSkeleton;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientReportService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Best selling products for a date range, ranked by revenue.
 *
 * <p>The order service caps the ranking with a {@code limit}; it defaults to ten
 * products, which is a sensible length for a "top products" board, so the limit
 * is left at the service default.</p>
 */
@Route(value = TopProductReportView.ROUTE, layout = com.harmoni.menu.dashboard.layout.MainLayout.class)
@PageTitle("Top products | POSHarmoni")
public class TopProductReportView extends AbstractReportView {

    /** Route the top products report is reachable at. */
    public static final String ROUTE = "report/top-products";

    private static final int SKELETON_ROWS = 8;

    private final transient AsyncRestClientReportService reportService;

    private final Grid<TopProductReportDto> grid = new Grid<>(TopProductReportDto.class, false);

    private final GridSkeleton skeleton = new GridSkeleton(SKELETON_ROWS);

    /**
     * Creates the top products report view.
     *
     * @param reportService report client for the order service
     */
    public TopProductReportView(AsyncRestClientReportService reportService) {
        super();
        this.reportService = reportService;
    }

    @Override
    protected String getTitle() {
        return Messages.get("nav.report.topProducts");
    }

    @Override
    protected String getDescription() {
        return Messages.get("report.topProducts.description");
    }

    @Override
    protected Component createContent() {
        grid.setSizeFull();
        grid.setEmptyStateText(Messages.get("report.topProducts.empty"));
        grid.addColumn(row -> row.getProductName() == null ? "-" : row.getProductName())
                .setHeader(Messages.get(Messages.Keys.GRID_HEADER_NAME))
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(row -> row.getQuantitySold() == null ? 0 : row.getQuantitySold())
                .setHeader(Messages.get("report.column.quantity"))
                .setAutoWidth(true);
        grid.addColumn(row -> money(row.getTotalSales()))
                .setHeader(Messages.get("report.column.sales"))
                .setAutoWidth(true);
        return gridSlot(grid, skeleton);
    }

    @Override
    protected void showLoading() {
        skeleton.show();
    }

    @Override
    protected void hideLoading() {
        skeleton.hide();
    }

    @Override
    protected void loadReport(DateRange range) {
        reportService.getTopProductsAsync(result -> UiUtil.safeAccess(ui, () -> {
            onReportLoaded();
            grid.setItems(result == null ? new ArrayList<>() : result);
        }), error -> UiUtil.safeAccess(ui, () -> onReportFailed(error)),
                range.startDateTime(), range.endDateTime(), 0);
    }

    /**
     * Formats a monetary total, falling back to a zero when it is missing.
     *
     * @param value the amount to format
     * @return the formatted amount
     */
    static String money(BigDecimal value) {
        return UiUtil.rupiah(value == null ? BigDecimal.ZERO : value);
    }
}
