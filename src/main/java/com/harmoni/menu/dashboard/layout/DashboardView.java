package com.harmoni.menu.dashboard.layout;

import com.harmoni.menu.dashboard.dto.report.SettlementReportDto;
import com.harmoni.menu.dashboard.dto.report.TopProductReportDto;
import com.harmoni.menu.dashboard.layout.component.KpiCard;
import com.harmoni.menu.dashboard.layout.component.OrderTypeFilter;
import com.harmoni.menu.dashboard.layout.security.RequiresPermission;
import com.harmoni.menu.dashboard.layout.util.LoadingBar;
import com.harmoni.menu.dashboard.layout.util.Permissions;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientReportService;
import com.harmoni.menu.dashboard.util.Messages;
import com.harmoni.menu.dashboard.util.VaadinSessionUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.Configuration;
import com.vaadin.flow.component.charts.model.ListSeries;
import com.vaadin.flow.component.charts.model.PlotOptionsBar;
import com.vaadin.flow.component.charts.model.Stacking;
import com.vaadin.flow.component.charts.model.style.SolidColor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Operational landing page shown after login: today's sales, transaction count,
 * payment split and best-selling menu, with the order-type scope chips above.
 *
 * <p>Sales, transactions, the per-transaction average, the payment breakdown and
 * the top-menu ranking come from the order service's reports. Occupancy, new
 * customers, the hourly chart and the attention list have no backing endpoint
 * yet, so they render honest empty/em-dash states rather than invented numbers.</p>
 */
@Slf4j
@RequiredArgsConstructor
@RequiresPermission(Permissions.DASHBOARD_VIEW)
@Route(value = "", layout = MainLayout.class)
@PageTitle("Dashboard | POSHarmoni")
public class DashboardView extends VerticalLayout {

    private static final int TOP_MENU_LIMIT = 5;

    /** Palette for the payment segments and their legend dots, in order. */
    private static final String[] PAYMENT_COLORS = {"#1f2937", "#10b981", "#6ee7b7", "#a7f3d0"};

    private final AsyncRestClientReportService reportService;

    private final LoadingBar loadingBar = new LoadingBar();
    private final AtomicInteger pending = new AtomicInteger();

    private final KpiCard salesCard = new KpiCard(Messages.get("dashboard.kpi.sales"));
    private final KpiCard transactionsCard = new KpiCard(Messages.get("dashboard.kpi.transactions"));
    private final KpiCard tablesCard = new KpiCard(Messages.get("dashboard.kpi.tables"));
    private final KpiCard customersCard = new KpiCard(Messages.get("dashboard.kpi.newCustomers"));

    private final VerticalLayout topMenuBody = new VerticalLayout();
    private final VerticalLayout paymentBody = new VerticalLayout();

    private UI ui;
    private boolean rendered;
    private volatile boolean loadErrored;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        ui = attachEvent.getUI();
        if (rendered) {
            return;
        }
        rendered = true;

        addClassName("dashboard-view");
        setSizeFull();
        setPadding(false);
        setSpacing(true);

        add(loadingBar);
        add(createHeader());
        add(createKpiRow());
        add(createBody());

        loadDashboard();
    }

    /**
     * Builds the page header: the title and today's date caption on the left,
     * the order-type chips on the right.
     *
     * @return the header row
     */
    private HorizontalLayout createHeader() {
        H2 title = new H2(Messages.get("nav.dashboard"));
        title.addClassName("dashboard-title");
        Span subtitle = new Span(subtitleText());
        subtitle.addClassName("dashboard-subtitle");
        VerticalLayout titles = new VerticalLayout(title, subtitle);
        titles.setPadding(false);
        titles.setSpacing(false);

        OrderTypeFilter filter = new OrderTypeFilter();

        HorizontalLayout header = new HorizontalLayout(titles, filter);
        header.addClassName("dashboard-header");
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        return header;
    }

    /**
     * Builds the four summary tiles.
     *
     * @return the tile row
     */
    private HorizontalLayout createKpiRow() {
        tablesCard.setValue("—");
        tablesCard.setCaption(Messages.get("dashboard.unavailable"));
        customersCard.setValue("—");
        customersCard.setCaption(Messages.get("dashboard.unavailable"));

        HorizontalLayout row = new HorizontalLayout(salesCard, transactionsCard, tablesCard, customersCard);
        row.addClassName("kpi-row");
        row.setWidthFull();
        row.setSpacing(true);
        row.setPadding(false);
        row.setFlexGrow(1, salesCard, transactionsCard, tablesCard, customersCard);
        row.setAlignItems(FlexComponent.Alignment.STRETCH);
        return row;
    }

    /**
     * Builds the two-column body: sales-by-hour and attention on the left,
     * top-menu and payment on the right.
     *
     * @return the body row
     */
    private HorizontalLayout createBody() {
        VerticalLayout left = new VerticalLayout(
                createPanel("dashboard.panel.salesByHour", emptyState("dashboard.hourlyUnavailable", "tall")),
                createPanel("dashboard.panel.attention", emptyState("dashboard.attentionEmpty", "tall")));
        left.addClassName("dashboard-column");
        left.setPadding(false);
        left.setSpacing(true);

        VerticalLayout right = new VerticalLayout(
                createPanel("dashboard.panel.topMenu", topMenuBody),
                createPanel("dashboard.panel.payment", paymentBody));
        right.addClassName("dashboard-column");
        right.setPadding(false);
        right.setSpacing(true);

        topMenuBody.setPadding(false);
        topMenuBody.setSpacing(true);
        topMenuBody.add(emptyState("dashboard.noData", "tall"));
        paymentBody.setPadding(false);
        paymentBody.setSpacing(true);
        paymentBody.add(emptyState("dashboard.noData", "tall"));

        HorizontalLayout body = new HorizontalLayout(left, right);
        body.addClassName("dashboard-body");
        body.setWidthFull();
        body.setSpacing(true);
        body.setPadding(false);
        body.setAlignItems(FlexComponent.Alignment.STRETCH);
        body.setFlexGrow(2, left);
        body.setFlexGrow(1, right);
        return body;
    }

    /**
     * Wraps a title and a body in the shared panel chrome.
     *
     * @param titleKey message key for the panel title
     * @param body     the panel content
     * @return the panel
     */
    private VerticalLayout createPanel(String titleKey, Component body) {
        H2 title = new H2(Messages.get(titleKey));
        title.addClassName("panel-title");
        VerticalLayout panel = new VerticalLayout(title, body);
        panel.addClassName("panel");
        panel.setPadding(true);
        panel.setSpacing(true);
        panel.setWidthFull();
        panel.setFlexGrow(1, body);
        return panel;
    }

    /**
     * Builds a muted empty-state block.
     *
     * @param messageKey message key for the empty-state text
     * @param sizeClass  extra class controlling the block height ("tall" fills the panel)
     * @return the empty-state component
     */
    private Component emptyState(String messageKey, String sizeClass) {
        Span text = new Span(Messages.get(messageKey));
        Div box = new Div(text);
        box.addClassName("dashboard-empty");
        if (sizeClass != null) {
            box.addClassName("dashboard-empty--" + sizeClass);
        }
        return box;
    }

    /**
     * Loads the settlement and top-product reports for today in parallel.
     */
    private void loadDashboard() {
        loadingBar.start();
        loadErrored = false;
        pending.set(2);

        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDateTime.now();

        reportService.getSettlementAsync(
                settlement -> UiUtil.safeAccess(ui, () -> {
                    applySettlement(settlement);
                    checkDone();
                }),
                error -> onLoadError("settlement", error), start, end, java.util.List.of());
        reportService.getTopProductsAsync(
                products -> UiUtil.safeAccess(ui, () -> {
                    applyTopProducts(products);
                    checkDone();
                }),
                error -> onLoadError("top-products", error), start, end, TOP_MENU_LIMIT, java.util.List.of());
    }

    /**
     * Marks one report as finished; when the last one lands it stops the loading
     * bar and, if any report failed, offers a retry. Safe to call from any thread.
     */
    private void checkDone() {
        if (pending.decrementAndGet() != 0) {
            return;
        }
        UiUtil.safeAccess(ui, () -> {
            loadingBar.stop();
            if (loadErrored) {
                UiUtil.errorWithRetry(Messages.get("dashboard.loadFailed"), this::reload);
            }
        });
    }

    private void onLoadError(String what, Throwable error) {
        log.warn("Could not load dashboard {}", what, error);
        loadErrored = true;
        checkDone();
    }

    private void reload() {
        UiUtil.safeAccess(ui, () -> {
            salesCard.setValue("—");
            transactionsCard.setValue("—");
            loadDashboard();
        });
    }

    /**
     * Applies the settlement report to the sales/transaction tiles and the
     * payment panel.
     *
     * @param settlement the settlement totals
     */
    private void applySettlement(SettlementReportDto settlement) {
        if (settlement == null) {
            return;
        }
        BigDecimal sales = settlement.getTotalSales();
        int orders = settlement.getTotalOrders();

        if (sales != null) {
            salesCard.setValue("Rp", formatAmount(sales));
        } else {
            salesCard.setValue("—");
        }
        salesCard.setCaption(Messages.get("dashboard.caption.today"));

        transactionsCard.setValue(String.valueOf(orders));
        if (sales != null && orders > 0) {
            BigDecimal average = sales.divide(BigDecimal.valueOf(orders), 0, RoundingMode.HALF_UP);
            transactionsCard.setCaption(
                    Messages.get("dashboard.caption.average", "Rp " + formatAmount(average)));
        } else {
            transactionsCard.setCaption(Messages.get("dashboard.caption.noTransactions"));
        }

        renderPayment(settlement.getPaymentBreakdown());
    }

    /**
     * Applies the top-product ranking to the menu panel, drawing a horizontal
     * bar per product scaled to the best seller.
     *
     * @param products the ranking, highest first
     */
    private void applyTopProducts(List<TopProductReportDto> products) {
        topMenuBody.removeAll();
        if (products == null || products.isEmpty()) {
            topMenuBody.add(emptyState("dashboard.noData", "tall"));
            return;
        }
        List<TopProductReportDto> ranked = new ArrayList<>(products);
        ranked.sort(Comparator.comparing(
                p -> p.getQuantitySold() == null ? 0 : p.getQuantitySold(), Comparator.reverseOrder()));
        int max = ranked.stream().mapToInt(p -> p.getQuantitySold() == null ? 0 : p.getQuantitySold())
                .max().orElse(1);
        if (max <= 0) {
            max = 1;
        }
        for (TopProductReportDto product : ranked) {
            topMenuBody.add(menuRow(product, max));
        }
    }

    private static HorizontalLayout menuRow(TopProductReportDto product, int max) {
        int quantity = product.getQuantitySold() == null ? 0 : product.getQuantitySold();
        double percent = Math.max(4, quantity * 100.0 / max);

        Span name = new Span(product.getProductName());
        name.addClassName("menu-name");

        Div fill = new Div();
        fill.addClassName("menu-bar-fill");
        fill.getStyle().set("width", percent + "%");
        Div track = new Div(fill);
        track.addClassName("menu-bar-track");

        Span qty = new Span(String.valueOf(quantity));
        qty.addClassName("menu-qty");

        HorizontalLayout row = new HorizontalLayout(name, track, qty);
        row.addClassName("menu-row");
        row.setWidthFull();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setFlexGrow(1, track);
        return row;
    }

    /**
     * Renders the payment-method split as a stacked bar chart plus a legend.
     *
     * @param breakdown the per-method totals, may be {@code null}
     */
    private void renderPayment(Map<String, BigDecimal> breakdown) {
        paymentBody.removeAll();
        if (breakdown == null || breakdown.isEmpty()) {
            paymentBody.add(emptyState("dashboard.noData", "tall"));
            return;
        }
        BigDecimal total = breakdown.values().stream()
                .filter(java.util.Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() <= 0) {
            paymentBody.add(emptyState("dashboard.noData", "tall"));
            return;
        }

        Chart chart = new Chart(ChartType.BAR);
        Configuration conf = chart.getConfiguration();
        conf.setTitle("");
        conf.getChart().setBackgroundColor(new SolidColor("transparent"));
        conf.getChart().setSpacing(new Number[]{0, 0, 0, 0});
        conf.getChart().setAnimation(false);
        conf.getLegend().setEnabled(false);
        conf.getxAxis().setVisible(false);
        conf.getyAxis().setVisible(false);
        conf.getyAxis().setCategories("");

        PlotOptionsBar plot = new PlotOptionsBar();
        plot.setStacking(Stacking.NORMAL);
        plot.setBorderRadius(6);
        plot.setPointPadding(0);
        plot.setGroupPadding(0);
        conf.setPlotOptions(plot);

        HorizontalLayout legend = new HorizontalLayout();
        legend.addClassName("payment-legend");
        legend.setWidthFull();
        legend.setSpacing(true);
        legend.setPadding(false);

        int index = 0;
        for (Map.Entry<String, BigDecimal> entry : breakdown.entrySet()) {
            BigDecimal value = entry.getValue() == null ? BigDecimal.ZERO : entry.getValue();
            int percent = value.multiply(BigDecimal.valueOf(100))
                    .divide(total, 0, RoundingMode.HALF_UP).intValue();
            ListSeries series = new ListSeries(paymentLabel(entry.getKey()), percent);
            conf.addSeries(series);
            legend.add(legendItem(paymentLabel(entry.getKey()), percent, colorAt(index)));
            index++;
        }

        chart.setHeight("28px");
        chart.setWidthFull();
        paymentBody.add(chart, legend);
    }

    private static HorizontalLayout legendItem(String label, int percent, String color) {
        Div dot = new Div();
        dot.addClassName("payment-dot");
        dot.getStyle().set("background-color", color);
        Span text = new Span(label + " " + percent + "%");
        text.addClassName("payment-legend-label");
        HorizontalLayout item = new HorizontalLayout(dot, text);
        item.addClassName("payment-legend-item");
        item.setAlignItems(FlexComponent.Alignment.CENTER);
        item.setSpacing(true);
        return item;
    }

    private static String colorAt(int index) {
        return PAYMENT_COLORS[index % PAYMENT_COLORS.length];
    }

    /**
     * Maps a payment-method key from the settlement report to a display label.
     *
     * @param key the raw key, e.g. "CASH" or "QRIS"
     * @return the translated label
     */
    private static String paymentLabel(String key) {
        if (key == null) {
            return Messages.get("dashboard.payment.other");
        }
        return switch (key.toUpperCase(Locale.ROOT)) {
            case "CASH" -> Messages.get("dashboard.payment.cash");
            case "QRIS", "QR" -> Messages.get("dashboard.payment.qris");
            case "CARD", "CREDIT", "DEBIT", "BANK" -> Messages.get("dashboard.payment.card");
            default -> Messages.get("dashboard.payment.other");
        };
    }

    /**
     * Builds the "today · date · data until time" subtitle in the session locale.
     *
     * @return the subtitle text
     */
    private static String subtitleText() {
        Locale locale = VaadinSessionUtil.getLocale();
        if (locale == null) {
            locale = Locale.getDefault();
        }
        LocalDateTime now = LocalDateTime.now();
        String date = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale).format(now);
        String time = DateTimeFormatter.ofPattern("HH:mm", locale).format(now);
        return Messages.get("dashboard.subtitle", date, time);
    }

    /**
     * Formats an amount with Indonesian thousands separators and no decimals.
     *
     * @param value the amount
     * @return the formatted amount
     */
    private static String formatAmount(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("id", "ID"));
        format.setMaximumFractionDigits(0);
        format.setMinimumFractionDigits(0);
        return format.format(value);
    }
}
