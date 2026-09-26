package com.harmoni.menu.dashboard.layout.report;

import com.harmoni.menu.dashboard.dto.report.DailyProductReportDto;
import com.harmoni.menu.dashboard.dto.report.DailyReportDto;
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
import java.util.Comparator;
import java.util.List;

/**
 * Per-day product breakdown for a date range.
 *
 * <p>The service returns one entry per day, each holding its own product list.
 * The dashboard flattens that into a single table of day/product rows sorted
 * newest first, which keeps the grid sortable and paginated by the standard
 * {@code Grid} behaviour instead of nesting a grid inside every row.</p>
 */
@Route(value = DailyReportView.ROUTE, layout = com.harmoni.menu.dashboard.layout.MainLayout.class)
@PageTitle("Daily sales | POSHarmoni")
public class DailyReportView extends AbstractReportView {

    /** Route the daily sales report is reachable at. */
    public static final String ROUTE = "report/daily";

    private static final int SKELETON_ROWS = 10;

    private final transient AsyncRestClientReportService reportService;

    private final Grid<DailyRow> grid = new Grid<>(DailyRow.class, false);

    private final GridSkeleton skeleton = new GridSkeleton(SKELETON_ROWS);

    /**
     * Creates the daily sales report view.
     *
     * @param reportService report client for the order service
     */
    public DailyReportView(AsyncRestClientReportService reportService) {
        super();
        this.reportService = reportService;
    }

    @Override
    protected String getTitle() {
        return Messages.get("nav.report.daily");
    }

    @Override
    protected String getDescription() {
        return Messages.get("report.daily.description");
    }

    @Override
    protected Component createContent() {
        grid.setSizeFull();
        grid.setEmptyStateText(Messages.get("report.daily.empty"));
        grid.addColumn(row -> formatDate(row.date()))
                .setHeader(Messages.get("report.column.date"))
                .setAutoWidth(true);
        grid.addColumn(row -> row.productName() == null ? "-" : row.productName())
                .setHeader(Messages.get(Messages.Keys.GRID_HEADER_NAME))
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.addColumn(row -> row.quantity() == null ? 0 : row.quantity())
                .setHeader(Messages.get("report.column.quantity"))
                .setAutoWidth(true);
        grid.addColumn(row -> TopProductReportView.money(row.discount()))
                .setHeader(Messages.get("report.column.discount"))
                .setAutoWidth(true);
        grid.addColumn(row -> TopProductReportView.money(row.netSales()))
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
        reportService.getDailyAsync(result -> UiUtil.safeAccess(ui, () -> {
            onReportLoaded();
            grid.setItems(flatten(result));
        }), error -> UiUtil.safeAccess(ui, () -> onReportFailed(error)),
                range.startDateTime(), range.endDateTime());
    }

    /**
     * Flattens the per-day structure into one row per day/product pair, newest
     * day first. Days without any products are skipped so the table only lists
     * days that actually traded.
     *
     * @param reports the per-day reports, may be {@code null}
     * @return the flattened rows
     */
    static List<DailyRow> flatten(List<DailyReportDto> reports) {
        List<DailyRow> rows = new ArrayList<>();
        if (reports == null) {
            return rows;
        }
        for (DailyReportDto report : reports) {
            if (report == null || report.getProducts() == null) {
                continue;
            }
            for (DailyProductReportDto product : report.getProducts()) {
                if (product == null) {
                    continue;
                }
                rows.add(new DailyRow(report.getDate(), product.getProductName(), product.getQuantity(),
                        product.getDiscount(), product.getNetSales()));
            }
        }
        rows.sort(Comparator.comparing(DailyRow::date, Comparator.nullsLast(Comparator.reverseOrder())));
        return rows;
    }

    /**
     * One product's sales on one day, the flattened shape of the daily report.
     *
     * @param date        the day, as an ISO date
     * @param productName the product sold
     * @param quantity    units sold
     * @param discount    discount applied
     * @param netSales    sales after discount
     */
    record DailyRow(String date, String productName, Integer quantity, BigDecimal discount,
                    BigDecimal netSales) {
    }
}
