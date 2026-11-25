package com.harmoni.menu.dashboard.layout;

import com.harmoni.menu.dashboard.dto.PromotionDto;
import com.harmoni.menu.dashboard.dto.order.OrderPageDto;
import com.harmoni.menu.dashboard.dto.order.RecentOrderDto;
import com.harmoni.menu.dashboard.dto.report.DailyProductReportDto;
import com.harmoni.menu.dashboard.dto.report.DailyReportDto;
import com.harmoni.menu.dashboard.dto.report.OrderVolumeReportDto;
import com.harmoni.menu.dashboard.dto.report.ProductSalesReportDto;
import com.harmoni.menu.dashboard.dto.report.SettlementReportDto;
import com.harmoni.menu.dashboard.dto.report.TopProductReportDto;
import com.harmoni.menu.dashboard.layout.ai.AiContextProvider;
import com.harmoni.menu.dashboard.layout.ai.AiPageContext;
import com.harmoni.menu.dashboard.layout.ai.AiTools;
import com.harmoni.menu.dashboard.layout.component.KpiCard;
import com.harmoni.menu.dashboard.layout.component.OrderTypeFilter;
import com.harmoni.menu.dashboard.layout.enums.PromotionStatus;
import com.harmoni.menu.dashboard.layout.security.RequiresPermission;
import com.harmoni.menu.dashboard.layout.util.DashboardAttentionRules;
import com.harmoni.menu.dashboard.layout.util.LoadingBar;
import com.harmoni.menu.dashboard.layout.util.Permissions;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrderService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientPromotionService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientReportService;
import com.harmoni.menu.dashboard.util.Messages;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.harmoni.menu.dashboard.util.VaadinSessionUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
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
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

@Slf4j
@RequiredArgsConstructor
@RequiresPermission(Permissions.DASHBOARD_VIEW)
@Route(value = "", layout = MainLayout.class)
@PageTitle("Dashboard | POSHarmoni")
public class DashboardView extends VerticalLayout implements AiContextProvider {

    private static final int TOP_MENU_LIMIT = 5;

    private static final int RECENT_ORDER_LIMIT = 10;

    private static final int RECENT_ORDER_DISPLAY_LIMIT = 5;

    private static final int PROMOTION_LIMIT = 20;

    private static final int PROMOTION_DISPLAY_LIMIT = 5;

    private static final int CATEGORY_LIMIT = 5;

    private static final String[] PAYMENT_COLORS = {"#1f2937", "#10b981", "#6ee7b7", "#a7f3d0"};

    private final AsyncRestClientReportService reportService;
    private final AsyncRestClientOrderService orderService;
    private final AsyncRestClientPromotionService promotionService;

    private final LoadingBar loadingBar = new LoadingBar();
    private final AtomicInteger pending = new AtomicInteger();

    private final KpiCard salesCard = new KpiCard(Messages.get("dashboard.kpi.sales"));
    private final KpiCard transactionsCard = new KpiCard(Messages.get("dashboard.kpi.transactions"));
    private final KpiCard peakOrdersCard = new KpiCard(Messages.get("dashboard.kpi.peakOrders"));
    private final KpiCard refundsCard = new KpiCard(Messages.get("dashboard.kpi.refunds"));

    private final VerticalLayout dailyBody = new VerticalLayout();
    private final VerticalLayout categoryBody = new VerticalLayout();
    private final VerticalLayout recentOrdersBody = new VerticalLayout();
    private final VerticalLayout attentionBody = new VerticalLayout();
    private final VerticalLayout topMenuBody = new VerticalLayout();
    private final VerticalLayout paymentBody = new VerticalLayout();
    private final VerticalLayout orderVolumeBody = new VerticalLayout();
    private final VerticalLayout promotionsBody = new VerticalLayout();

    private SettlementReportDto latestSettlement;
    private List<TopProductReportDto> latestTopProducts = List.of();
    private List<DailyReportDto> latestDaily = List.of();
    private List<ProductSalesReportDto> latestSales = List.of();
    private OrderVolumeReportDto latestOrderVolume;
    private OrderPageDto latestOrders;
    private List<PromotionDto> latestPromotions = List.of();
    private List<DashboardAttentionRules.Signal> latestAttention = List.of();

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

    @Override
    public AiPageContext aiPageContext() {
        return new AiPageContext(Messages.get("nav.dashboard"), dashboardSummary(), AiTools.SALES_REPORT);
    }

    public String dashboardSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append(subtitleText()).append('\n');
        appendCard(summary, Messages.get("dashboard.kpi.sales"), salesCard);
        appendCard(summary, Messages.get("dashboard.kpi.transactions"), transactionsCard);
        appendCard(summary, Messages.get("dashboard.kpi.peakOrders"), peakOrdersCard);
        appendCard(summary, Messages.get("dashboard.kpi.refunds"), refundsCard);
        appendAmountLine(summary, "dashboard.summary.netSales",
                latestSettlement == null ? null : latestSettlement.getTotalNetSales());
        appendAmountLine(summary, "dashboard.summary.discounts",
                latestSettlement == null ? null : latestSettlement.getTotalDiscounts());
        appendAmountLine(summary, "dashboard.summary.tax",
                latestSettlement == null ? null : latestSettlement.getTotalTax());
        summary.append(Messages.get("dashboard.panel.payment")).append(": ")
                .append(paymentSummary()).append('\n');
        summary.append(Messages.get("dashboard.panel.topMenu")).append(": ")
                .append(topMenuSummary()).append('\n');
        summary.append(Messages.get("dashboard.summary.daily")).append(": ")
                .append(dailySummary()).append('\n');
        summary.append(Messages.get("dashboard.summary.category")).append(": ")
                .append(categorySummary()).append('\n');
        summary.append(Messages.get("dashboard.summary.orderVolume")).append(": ")
                .append(orderVolumeSummary()).append('\n');
        summary.append(Messages.get("dashboard.summary.recentOrders")).append(": ")
                .append(recentOrdersSummary()).append('\n');
        summary.append(Messages.get("dashboard.summary.promotions")).append(": ")
                .append(promotionsSummary()).append('\n');
        summary.append(Messages.get("dashboard.panel.attention")).append(": ")
                .append(attentionSummary()).append('\n');
        return summary.toString();
    }

    private static void appendCard(StringBuilder summary, String label, KpiCard card) {
        String prefix = card.getPrefix();
        summary.append(label).append(": ")
                .append(prefix.isBlank() ? card.getValue() : prefix + " " + card.getValue());
        if (!card.getCaption().isBlank()) {
            summary.append(" (").append(card.getCaption()).append(')');
        }
        summary.append('\n');
    }

    private static void appendAmountLine(StringBuilder summary, String labelKey, BigDecimal value) {
        summary.append(Messages.get(labelKey)).append(": ");
        summary.append(value == null ? Messages.get("dashboard.noData") : "Rp " + formatAmount(value))
                .append('\n');
    }

    private String paymentSummary() {
        if (latestSettlement == null || latestSettlement.getPaymentBreakdown() == null
                || latestSettlement.getPaymentBreakdown().isEmpty()) {
            return Messages.get("dashboard.noData");
        }
        return String.join(", ", latestSettlement.getPaymentBreakdown().entrySet().stream()
                .map(entry -> paymentLabel(entry.getKey()) + " "
                        + formatAmount(entry.getValue() == null ? BigDecimal.ZERO : entry.getValue()))
                .toList());
    }

    private String topMenuSummary() {
        if (latestTopProducts.isEmpty()) {
            return Messages.get("dashboard.noData");
        }
        return String.join(", ", latestTopProducts.stream()
                .map(product -> product.getProductName() + " ("
                        + (product.getQuantitySold() == null ? 0 : product.getQuantitySold()) + ")")
                .toList());
    }

    private String dailySummary() {
        if (latestDaily.isEmpty()) {
            return Messages.get("dashboard.noData");
        }
        BigDecimal netSales = BigDecimal.ZERO;
        int items = 0;
        for (DailyReportDto day : latestDaily) {
            if (day.getProducts() == null) {
                continue;
            }
            for (DailyProductReportDto product : day.getProducts()) {
                netSales = netSales.add(product.getNetSales() == null ? BigDecimal.ZERO : product.getNetSales());
                items += product.getQuantity() == null ? 0 : product.getQuantity();
            }
        }
        return Messages.get("dashboard.summary.dailyValue", "Rp " + formatAmount(netSales), items);
    }

    private String categorySummary() {
        if (latestSales.isEmpty()) {
            return Messages.get("dashboard.noData");
        }
        return categoryTotals().entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(3)
                .map(entry -> entry.getKey() + " Rp " + formatAmount(entry.getValue()))
                .reduce((left, right) -> left + ", " + right)
                .orElse(Messages.get("dashboard.noData"));
    }

    private String orderVolumeSummary() {
        if (latestOrderVolume == null) {
            return Messages.get("dashboard.noData");
        }
        return Messages.get("dashboard.summary.volumeValue",
                intOrZero(latestOrderVolume.getTotalOrders()),
                intOrZero(latestOrderVolume.getPeakTimeOrders()),
                intOrZero(latestOrderVolume.getNonPeakTimeOrders()));
    }

    private String recentOrdersSummary() {
        List<RecentOrderDto> orders = recentOrders();
        if (orders.isEmpty()) {
            return Messages.get("dashboard.noData");
        }
        return String.join(", ", orders.stream()
                .limit(RECENT_ORDER_DISPLAY_LIMIT)
                .map(order -> order.getOrderNo() + " " + statusLabel(order.getStatus()) + " Rp "
                        + formatAmount(order.getGrandTotal()))
                .toList());
    }

    private String promotionsSummary() {
        if (latestPromotions.isEmpty()) {
            return Messages.get("dashboard.noData");
        }
        return String.join(", ", latestPromotions.stream()
                .limit(PROMOTION_DISPLAY_LIMIT)
                .map(promotion -> promotion.getName() + " (" + promotionEnd(promotion) + ")")
                .toList());
    }

    private String attentionSummary() {
        if (latestAttention.isEmpty()) {
            return Messages.get("dashboard.attentionEmpty");
        }
        return String.join(", ", latestAttention.stream().map(this::attentionText).toList());
    }

    private HorizontalLayout createKpiRow() {
        peakOrdersCard.setValue("—");
        peakOrdersCard.setCaption(Messages.get("dashboard.unavailable"));
        refundsCard.setValue("—");
        refundsCard.setCaption(Messages.get("dashboard.unavailable"));

        HorizontalLayout row = new HorizontalLayout(salesCard, transactionsCard, peakOrdersCard, refundsCard);
        row.addClassName("kpi-row");
        row.setWidthFull();
        row.setSpacing(true);
        row.setPadding(false);
        row.setFlexGrow(1, salesCard, transactionsCard, peakOrdersCard, refundsCard);
        row.setAlignItems(FlexComponent.Alignment.STRETCH);
        return row;
    }

    private HorizontalLayout createBody() {
        VerticalLayout left = new VerticalLayout(
                createPanel("dashboard.panel.daily", dailyBody),
                createPanel("dashboard.panel.category", categoryBody),
                createPanel("dashboard.panel.recentOrders", recentOrdersBody),
                createPanel("dashboard.panel.attention", attentionBody));
        left.addClassName("dashboard-column");
        left.setPadding(false);
        left.setSpacing(true);

        VerticalLayout right = new VerticalLayout(
                createPanel("dashboard.panel.topMenu", topMenuBody),
                createPanel("dashboard.panel.payment", paymentBody),
                createPanel("dashboard.panel.orderVolume", orderVolumeBody),
                createPanel("dashboard.panel.promotions", promotionsBody));
        right.addClassName("dashboard-column");
        right.setPadding(false);
        right.setSpacing(true);

        for (VerticalLayout body : List.of(dailyBody, categoryBody, recentOrdersBody, attentionBody,
                topMenuBody, paymentBody, orderVolumeBody, promotionsBody)) {
            body.setPadding(false);
            body.setSpacing(true);
            body.add(emptyState("dashboard.noData", "tall"));
        }

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

    private Component emptyState(String messageKey, String sizeClass) {
        Span text = new Span(Messages.get(messageKey));
        Div box = new Div(text);
        box.addClassName("dashboard-empty");
        if (sizeClass != null) {
            box.addClassName("dashboard-empty--" + sizeClass);
        }
        return box;
    }

    private void loadDashboard() {
        loadingBar.start();
        loadErrored = false;
        latestSettlement = null;
        latestTopProducts = List.of();
        latestDaily = List.of();
        latestSales = List.of();
        latestOrderVolume = null;
        latestOrders = null;
        latestPromotions = List.of();
        latestAttention = List.of();
        pending.set(7);

        LocalDateTime start = LocalDate.now().atStartOfDay();
        LocalDateTime end = LocalDateTime.now();

        reportService.getSettlementAsync(
                settlement -> UiUtil.safeAccess(ui, () -> {
                    applySettlement(settlement);
                    checkDone();
                }),
                error -> onLoadError("settlement", error), start, end, List.of());
        reportService.getTopProductsAsync(
                products -> UiUtil.safeAccess(ui, () -> {
                    applyTopProducts(products);
                    checkDone();
                }),
                error -> onLoadError("top-products", error), start, end, TOP_MENU_LIMIT, List.of());
        reportService.getDailyAsync(
                days -> UiUtil.safeAccess(ui, () -> {
                    applyDaily(days);
                    checkDone();
                }),
                error -> onLoadError("daily", error), start, end, List.of());
        reportService.getSalesAsync(
                rows -> UiUtil.safeAccess(ui, () -> {
                    applySales(rows);
                    checkDone();
                }),
                error -> onLoadError("sales", error), start, end, List.of());
        reportService.getOrderVolumeAsync(
                volume -> UiUtil.safeAccess(ui, () -> {
                    applyOrderVolume(volume);
                    checkDone();
                }),
                error -> onLoadError("order-volume", error), start, end, List.of());
        orderService.getRecentOrdersAsync(
                page -> UiUtil.safeAccess(ui, () -> {
                    applyOrders(page);
                    checkDone();
                }),
                error -> onLoadError("orders", error), RECENT_ORDER_LIMIT, null);
        promotionService.getPromotionsAsync(
                result -> UiUtil.safeAccess(ui, () -> {
                    applyPromotions(result);
                    checkDone();
                }),
                error -> onLoadError("promotions", error), 1, PROMOTION_LIMIT,
                PromotionStatus.ACTIVE.name(), null, null, null);
    }

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
            peakOrdersCard.setValue("—");
            refundsCard.setValue("—");
            loadDashboard();
        });
    }

    private void applySettlement(SettlementReportDto settlement) {
        if (settlement == null) {
            return;
        }
        latestSettlement = settlement;
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

        BigDecimal refunds = settlement.getTotalRefunds();
        refundsCard.setValue("Rp", formatAmount(refunds == null ? BigDecimal.ZERO : refunds));
        refundsCard.setCaption(Messages.get("dashboard.caption.today"));

        renderPayment(settlement.getPaymentBreakdown());
        refreshAttention();
    }

    private void applyTopProducts(List<TopProductReportDto> products) {
        topMenuBody.removeAll();
        if (products == null || products.isEmpty()) {
            latestTopProducts = List.of();
            topMenuBody.add(emptyState("dashboard.noData", "tall"));
            return;
        }
        List<TopProductReportDto> ranked = new ArrayList<>(products);
        ranked.sort(Comparator.comparing(
                p -> p.getQuantitySold() == null ? 0 : p.getQuantitySold(), Comparator.reverseOrder()));
        latestTopProducts = List.copyOf(ranked);
        int max = ranked.stream().mapToInt(p -> p.getQuantitySold() == null ? 0 : p.getQuantitySold())
                .max().orElse(1);
        if (max <= 0) {
            max = 1;
        }
        for (TopProductReportDto product : ranked) {
            int quantity = product.getQuantitySold() == null ? 0 : product.getQuantitySold();
            topMenuBody.add(barRow(product.getProductName(), quantity, max, String.valueOf(quantity)));
        }
    }

    private void applyDaily(List<DailyReportDto> days) {
        latestDaily = days == null ? List.of() : List.copyOf(days);
        renderDaily();
        refreshAttention();
    }

    private void applySales(List<ProductSalesReportDto> rows) {
        latestSales = rows == null ? List.of() : List.copyOf(rows);
        renderCategories();
        refreshAttention();
    }

    private void applyOrderVolume(OrderVolumeReportDto volume) {
        latestOrderVolume = volume;
        if (volume != null) {
            int peak = intOrZero(volume.getPeakTimeOrders());
            int total = intOrZero(volume.getTotalOrders());
            peakOrdersCard.setValue(String.valueOf(peak));
            if (total > 0) {
                int percent = Math.round(peak * 100f / total);
                peakOrdersCard.setCaption(Messages.get("dashboard.caption.peakShare", percent));
            } else {
                peakOrdersCard.setCaption(Messages.get("dashboard.caption.noTransactions"));
            }
        }
        renderOrderVolume();
        refreshAttention();
    }

    private void applyOrders(OrderPageDto page) {
        latestOrders = page;
        renderRecentOrders();
        refreshAttention();
    }

    private void applyPromotions(Map<String, Object> result) {
        List<PromotionDto> promotions = ObjectUtil.extractDataList(result, PromotionDto.class);
        latestPromotions = List.copyOf(promotions);
        renderPromotions();
        refreshAttention();
    }

    private void refreshAttention() {
        latestAttention = DashboardAttentionRules.evaluate(latestSettlement, recentOrders(),
                latestPromotions, LocalDate.now());
        renderAttention();
    }

    private void renderDaily() {
        dailyBody.removeAll();
        if (latestDaily.isEmpty()) {
            dailyBody.add(emptyState("dashboard.daily.empty", "tall"));
            return;
        }
        BigDecimal netSales = BigDecimal.ZERO;
        int items = 0;
        for (DailyReportDto day : latestDaily) {
            if (day.getProducts() == null) {
                continue;
            }
            for (DailyProductReportDto product : day.getProducts()) {
                netSales = netSales.add(product.getNetSales() == null ? BigDecimal.ZERO : product.getNetSales());
                items += product.getQuantity() == null ? 0 : product.getQuantity();
            }
        }
        dailyBody.add(metricRow(Messages.get("dashboard.daily.net"), "Rp " + formatAmount(netSales)));
        dailyBody.add(metricRow(Messages.get("dashboard.daily.quantity"), String.valueOf(items)));
        topDailyProduct().ifPresent(product -> dailyBody.add(metricRow(
                Messages.get("dashboard.daily.topProduct"), product.getProductName())));
    }

    private void renderCategories() {
        categoryBody.removeAll();
        if (latestSales.isEmpty()) {
            categoryBody.add(emptyState("dashboard.category.empty", "tall"));
            return;
        }
        List<Map.Entry<String, BigDecimal>> ranked = categoryTotals().entrySet().stream()
                .sorted(Map.Entry.<String, BigDecimal>comparingByValue().reversed())
                .limit(CATEGORY_LIMIT)
                .toList();
        double max = ranked.isEmpty() ? 1 : ranked.getFirst().getValue().doubleValue();
        for (Map.Entry<String, BigDecimal> entry : ranked) {
            categoryBody.add(barRow(entry.getKey(), entry.getValue().doubleValue(), max,
                    "Rp " + formatAmount(entry.getValue())));
        }
    }

    private void renderOrderVolume() {
        orderVolumeBody.removeAll();
        if (latestOrderVolume == null) {
            orderVolumeBody.add(emptyState("dashboard.volume.empty", "tall"));
            return;
        }
        int total = intOrZero(latestOrderVolume.getTotalOrders());
        int peak = intOrZero(latestOrderVolume.getPeakTimeOrders());
        int offPeak = intOrZero(latestOrderVolume.getNonPeakTimeOrders());

        orderVolumeBody.add(metricRow(Messages.get("dashboard.volume.total"), String.valueOf(total)));
        orderVolumeBody.add(metricRow(Messages.get("dashboard.volume.peak"), String.valueOf(peak)));
        orderVolumeBody.add(metricRow(Messages.get("dashboard.volume.offPeak"), String.valueOf(offPeak)));

        if (total > 0) {
            Div bar = new Div();
            bar.addClassName("volume-bar");
            bar.add(volumeSegment(peak, "volume-segment--peak"));
            bar.add(volumeSegment(offPeak, "volume-segment--offPeak"));
            orderVolumeBody.add(bar);
        }
    }

    private void renderRecentOrders() {
        recentOrdersBody.removeAll();
        List<RecentOrderDto> orders = recentOrders();
        if (orders.isEmpty()) {
            recentOrdersBody.add(emptyState("dashboard.orders.empty", "tall"));
            return;
        }
        orders.stream().limit(RECENT_ORDER_DISPLAY_LIMIT).forEach(order -> recentOrdersBody.add(orderRow(order)));
    }

    private void renderPromotions() {
        promotionsBody.removeAll();
        if (latestPromotions.isEmpty()) {
            promotionsBody.add(emptyState("dashboard.promo.empty", "tall"));
            return;
        }
        latestPromotions.stream().limit(PROMOTION_DISPLAY_LIMIT)
                .forEach(promotion -> promotionsBody.add(promotionRow(promotion)));
    }

    private void renderAttention() {
        attentionBody.removeAll();
        if (latestAttention.isEmpty()) {
            attentionBody.add(emptyState("dashboard.attentionEmpty", "tall"));
            return;
        }
        for (DashboardAttentionRules.Signal signal : latestAttention) {
            attentionBody.add(attentionRow(attentionText(signal)));
        }
    }

    private List<RecentOrderDto> recentOrders() {
        if (latestOrders == null || latestOrders.getOrders() == null) {
            return List.of();
        }
        return latestOrders.getOrders();
    }

    private Map<String, BigDecimal> categoryTotals() {
        Map<String, BigDecimal> totals = new LinkedHashMap<>();
        for (ProductSalesReportDto row : latestSales) {
            String name = row.getCategoryName() == null || row.getCategoryName().isBlank()
                    ? Messages.get("dashboard.category.other") : row.getCategoryName();
            totals.merge(name, row.getNetSales() == null ? BigDecimal.ZERO : row.getNetSales(), BigDecimal::add);
        }
        return totals;
    }

    private java.util.Optional<DailyProductReportDto> topDailyProduct() {
        return latestDaily.stream()
                .flatMap(day -> day.getProducts() == null ? Stream.empty() : day.getProducts().stream())
                .max(Comparator.comparing(product -> product.getNetSales() == null
                        ? BigDecimal.ZERO : product.getNetSales()));
    }

    private static HorizontalLayout barRow(String nameText, double value, double max, String valueText) {
        double percent = max <= 0 ? 0 : Math.max(4, value * 100.0 / max);

        Span name = new Span(nameText == null ? "" : nameText);
        name.addClassName("menu-name");

        Div fill = new Div();
        fill.addClassName("menu-bar-fill");
        fill.getStyle().set("width", percent + "%");
        Div track = new Div(fill);
        track.addClassName("menu-bar-track");

        Span amount = new Span(valueText);
        amount.addClassName("menu-qty");

        HorizontalLayout row = new HorizontalLayout(name, track, amount);
        row.addClassName("menu-row");
        row.setWidthFull();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setSpacing(true);
        row.setFlexGrow(1, track);
        return row;
    }

    private static HorizontalLayout metricRow(String label, String value) {
        Span labelSpan = new Span(label);
        labelSpan.addClassName("metric-label");
        Span valueSpan = new Span(value);
        valueSpan.addClassName("metric-value");
        HorizontalLayout row = new HorizontalLayout(labelSpan, valueSpan);
        row.addClassName("metric-row");
        row.setWidthFull();
        row.setSpacing(true);
        row.setPadding(false);
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        return row;
    }

    private static Div volumeSegment(int value, String className) {
        Div segment = new Div();
        segment.addClassName("volume-segment");
        segment.addClassName(className);
        segment.getStyle().set("flex-grow", String.valueOf(Math.max(value, 0)));
        segment.getStyle().set("flex-basis", "0");
        return segment;
    }

    private static HorizontalLayout orderRow(RecentOrderDto order) {
        Span number = new Span(order.getOrderNo() == null ? "" : order.getOrderNo());
        number.addClassName("order-number");

        List<String> metaParts = new ArrayList<>();
        String time = formatTime(order.getCreatedAt());
        if (!time.isBlank()) {
            metaParts.add(time);
        }
        if (order.getTableNumber() != null && !order.getTableNumber().isBlank()) {
            metaParts.add(Messages.get("dashboard.order.table", order.getTableNumber()));
        }
        int itemCount = order.getOrderDetails() == null ? 0 : order.getOrderDetails().size();
        if (itemCount > 0) {
            metaParts.add(Messages.get("dashboard.order.items", itemCount));
        }
        Span meta = new Span(metaParts.isEmpty()
                ? Messages.get("dashboard.order.noMeta") : String.join(" · ", metaParts));
        meta.addClassName("order-meta");

        VerticalLayout left = new VerticalLayout(number, meta);
        left.addClassName("order-main");
        left.setPadding(false);
        left.setSpacing(false);

        Span amount = new Span("Rp " + formatAmount(order.getGrandTotal()));
        amount.addClassName("order-amount");
        Span badge = statusBadge(order.getStatus());

        VerticalLayout right = new VerticalLayout(amount, badge);
        right.addClassName("order-side");
        right.setPadding(false);
        right.setSpacing(false);

        HorizontalLayout row = new HorizontalLayout(left, right);
        row.addClassName("order-row");
        row.setWidthFull();
        row.setSpacing(true);
        row.setPadding(false);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setFlexGrow(1, left);
        return row;
    }

    private static Span statusBadge(String status) {
        String value = status == null ? "unknown" : status.toLowerCase(Locale.ROOT);
        Span badge = new Span(statusLabel(status));
        badge.addClassName("status-badge");
        badge.addClassName("status-badge--" + value);
        return badge;
    }

    private static String statusLabel(String status) {
        if (status == null || status.isBlank()) {
            return Messages.get("dashboard.order.status.unknown");
        }
        return Messages.get("dashboard.order.status." + status.toLowerCase(Locale.ROOT));
    }

    private static HorizontalLayout promotionRow(PromotionDto promotion) {
        Span name = new Span(promotion.getName() == null ? "" : promotion.getName());
        name.addClassName("promo-name");
        Span meta = new Span(promotionEnd(promotion));
        meta.addClassName("promo-meta");
        VerticalLayout left = new VerticalLayout(name, meta);
        left.setPadding(false);
        left.setSpacing(false);
        left.addClassName("promo-main");

        Span status = new Span(promotion.getStatus() == null
                ? Messages.get("dashboard.order.status.unknown") : promotion.getStatus().getLabel());
        status.addClassName("promo-status");

        HorizontalLayout row = new HorizontalLayout(left, status);
        row.addClassName("promo-row");
        row.setWidthFull();
        row.setSpacing(true);
        row.setPadding(false);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setFlexGrow(1, left);
        return row;
    }

    private static String promotionEnd(PromotionDto promotion) {
        if (promotion.getEndDate() == null) {
            return Messages.get("dashboard.promo.noEnd");
        }
        return Messages.get("dashboard.promo.ends", formatDate(promotion.getEndDate()));
    }

    private static HorizontalLayout attentionRow(String text) {
        Div dot = new Div();
        dot.addClassName("attention-dot");
        Span label = new Span(text);
        label.addClassName("attention-text");
        HorizontalLayout row = new HorizontalLayout(dot, label);
        row.addClassName("attention-item");
        row.setWidthFull();
        row.setSpacing(true);
        row.setPadding(false);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        return row;
    }

    private String attentionText(DashboardAttentionRules.Signal signal) {
        return switch (signal.type()) {
            case REFUNDS -> Messages.get("dashboard.attention.refunds", "Rp " + formatAmount(signal.amount()));
            case HIGH_DISCOUNT -> Messages.get("dashboard.attention.discounts", signal.count());
            case UNPAID_ORDERS -> Messages.get("dashboard.attention.pendingOrders", signal.count());
            case EXPIRING_PROMOTIONS -> Messages.get("dashboard.attention.expiringPromotions", signal.count());
        };
    }

    private void renderPayment(Map<String, BigDecimal> breakdown) {
        paymentBody.removeAll();
        if (breakdown == null || breakdown.isEmpty()) {
            paymentBody.add(emptyState("dashboard.noData", "tall"));
            return;
        }
        BigDecimal total = breakdown.values().stream()
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (total.signum() <= 0) {
            paymentBody.add(emptyState("dashboard.noData", "tall"));
            return;
        }

        Div bar = new Div();
        bar.addClassName("payment-bar");

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
            String label = paymentLabel(entry.getKey());

            Div segment = new Div();
            segment.addClassName("payment-segment");
            segment.getStyle().set("flex-grow", value.toPlainString());
            segment.getStyle().set("flex-basis", "0");
            segment.getStyle().set("background-color", colorAt(index));
            segment.getElement().setAttribute("title", label + " " + percent + "%");
            bar.add(segment);

            legend.add(legendItem(label, percent, colorAt(index)));
            index++;
        }

        paymentBody.add(bar, legend);
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

    private static String subtitleText() {
        Locale locale = locale();
        LocalDateTime now = LocalDateTime.now();
        String date = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", locale).format(now);
        String time = DateTimeFormatter.ofPattern("HH:mm", locale).format(now);
        return Messages.get("dashboard.subtitle", date, time);
    }

    private static String formatDate(LocalDate date) {
        return DateTimeFormatter.ofPattern("d MMM", locale()).format(date);
    }

    private static String formatTime(Date date) {
        if (date == null) {
            return "";
        }
        return DateTimeFormatter.ofPattern("HH:mm", locale())
                .withZone(ZoneId.systemDefault())
                .format(date.toInstant());
    }

    private static Locale locale() {
        Locale locale = VaadinSessionUtil.getLocale();
        return locale == null ? Locale.getDefault() : locale;
    }

    private static int intOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private static String formatAmount(BigDecimal value) {
        NumberFormat format = NumberFormat.getNumberInstance(new Locale("id", "ID"));
        format.setMaximumFractionDigits(0);
        format.setMinimumFractionDigits(0);
        return format.format(value == null ? BigDecimal.ZERO : value);
    }
}
