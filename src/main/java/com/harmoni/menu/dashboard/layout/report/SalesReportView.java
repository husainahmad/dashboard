package com.harmoni.menu.dashboard.layout.report;

import com.harmoni.menu.dashboard.dto.report.ProductSalesReportDto;
import com.harmoni.menu.dashboard.layout.util.GridSkeleton;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientReportService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import java.util.ArrayList;

/**
 * Per-product sales for a date range, grouped by category in the table so a
 * category's contribution can be read without a separate rollup.
 *
 * <p>Unlike the daily report this one is already flat: the service returns one
 * row per product with its category attached.</p>
 */
@Route(value = SalesReportView.ROUTE, layout = com.harmoni.menu.dashboard.layout.MainLayout.class)
@PageTitle("Sales by product | POSHarmoni")
public class SalesReportView extends AbstractReportView {

    /** Route the sales by product report is reachable at. */
    public static final String ROUTE = "report/sales";

    private static final int SKELETON_ROWS = 10;

    private final transient AsyncRestClientReportService reportService;

    private final Grid<ProductSalesReportDto> grid = new Grid<>(ProductSalesReportDto.class, false);

    private final GridSkeleton skeleton = new GridSkeleton(SKELETON_ROWS);

    /**
     * Creates the sales by product report view.
     *
     * @param reportService report client for the order service
     */
    public SalesReportView(AsyncRestClientReportService reportService) {
        super();
        this.reportService = reportService;
    }

    @Override
    protected String getTitle() {
        return Messages.get("nav.report.sales");
    }

    @Override
    protected String getDescription() {
        return Messages.get("report.sales.description");
    }

    @Override
    protected Component createContent() {
        grid.setSizeFull();
        grid.setEmptyStateText(Messages.get("report.sales.empty"));
        grid.addColumn(row -> row.getCategoryName() == null ? "-" : row.getCategoryName())
                .setHeader(Messages.get(Messages.Keys.LABEL_CATEGORY))
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(row -> row.getProductName() == null ? "-" : row.getProductName())
                .setHeader(Messages.get(Messages.Keys.GRID_HEADER_NAME))
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(row -> row.getQuantity() == null ? 0 : row.getQuantity())
                .setHeader(Messages.get("report.column.quantity"))
                .setAutoWidth(true);
        grid.addColumn(row -> TopProductReportView.money(row.getGrossSales()))
                .setHeader(Messages.get("report.column.gross"))
                .setAutoWidth(true);
        grid.addColumn(row -> TopProductReportView.money(row.getDiscount()))
                .setHeader(Messages.get("report.column.discount"))
                .setAutoWidth(true);
        grid.addColumn(row -> TopProductReportView.money(row.getNetSales()))
                .setHeader(Messages.get("report.column.netSales"))
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
        reportService.getSalesAsync(result -> UiUtil.safeAccess(ui, () -> {
            onReportLoaded();
            grid.setItems(result == null ? new ArrayList<>() : result);
        }), error -> UiUtil.safeAccess(ui, () -> onReportFailed(error)),
                range.startDateTime(), range.endDateTime());
    }
}
