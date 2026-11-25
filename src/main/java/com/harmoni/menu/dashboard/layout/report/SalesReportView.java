package com.harmoni.menu.dashboard.layout.report;

import com.harmoni.menu.dashboard.dto.report.ProductSalesReportDto;
import com.harmoni.menu.dashboard.layout.util.GridSkeleton;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientReportService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;

import java.util.List;

/**
 * Per-product sales for a date range, grouped by category in the table so a
 * category's contribution can be read without a separate rollup.
 *
 * <p>Unlike the daily report this one is already flat: the service returns one
 * row per product with its category attached.</p>
 */
public class SalesReportView extends AbstractReportView {


    private static final int SKELETON_ROWS = 10;

    private final transient AsyncRestClientReportService reportService;

    private final Grid<ProductSalesReportDto> grid = new Grid<>(ProductSalesReportDto.class, false);

    private final GridSkeleton skeleton = new GridSkeleton(SKELETON_ROWS);

    private List<ProductSalesReportDto> latestRows = List.of();

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
                .setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.END);
        grid.addColumn(row -> TopProductReportView.money(row.getDiscount()))
                .setHeader(Messages.get("report.column.discount"))
                .setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.END);
        grid.addColumn(row -> TopProductReportView.money(row.getNetSales()))
                .setHeader(Messages.get("report.column.netSales"))
                .setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.END);
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
            latestRows = result == null ? List.of() : result;
            grid.setItems(latestRows);
        }), error -> UiUtil.safeAccess(ui, () -> onReportFailed(error)),
                range.startDateTime(), range.endDateTime(), storeIds);
    }

    @Override
    protected String reportContext() {
        if (latestRows.isEmpty()) {
            return Messages.get("ai.context.noReportData") + "\n";
        }
        return latestRows.stream().limit(5)
                .map(row -> row.getProductName() + " (" + row.getCategoryName() + "): "
                        + (row.getQuantity() == null ? 0 : row.getQuantity()) + " / "
                        + TopProductReportView.money(row.getNetSales()))
                .reduce((left, right) -> left + "\n" + right)
                .orElse(Messages.get("ai.context.noReportData")) + "\n";
    }
}
