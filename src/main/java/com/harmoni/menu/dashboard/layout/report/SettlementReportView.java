package com.harmoni.menu.dashboard.layout.report;

import com.harmoni.menu.dashboard.dto.report.SettlementReportDto;
import com.harmoni.menu.dashboard.layout.component.StatCard;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientReportService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import org.apache.commons.lang3.ObjectUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Settlement summary for a date range: the day's order count, the money that
 * came in, and how it was split across payment methods.
 *
 * <p>The order service does not scope this report by store even though it
 * accepts a store id, so the totals here cover every store. The other four
 * reports on this menu are correctly scoped, which makes settlement look larger
 * than the neighbouring reports until that is fixed server-side.</p>
 */
@Route(value = SettlementReportView.ROUTE, layout = com.harmoni.menu.dashboard.layout.MainLayout.class)
@PageTitle("Settlement | POSHarmoni")
public class SettlementReportView extends AbstractReportView {

    /** Route the settlement report is reachable at. */
    public static final String ROUTE = "report/settlement";

    private final transient AsyncRestClientReportService reportService;

    private final StatCard ordersCard = new StatCard(VaadinIcon.CART, Messages.get("report.settlement.orders"));

    private final StatCard grossCard = new StatCard(VaadinIcon.CASH, Messages.get("report.settlement.gross"));

    private final StatCard netCard = new StatCard(VaadinIcon.COINS, Messages.get("report.settlement.net"));

    private final StatCard discountCard = new StatCard(VaadinIcon.TAG, Messages.get("report.settlement.discount"));

    private final StatCard taxCard = new StatCard(VaadinIcon.INVOICE, Messages.get("report.settlement.tax"));

    private final StatCard refundCard =
            new StatCard(VaadinIcon.ARROW_BACKWARD, Messages.get("report.settlement.refunds"));

    private final VerticalLayout breakdown = new VerticalLayout();

    private final Span breakdownEmpty = new Span(Messages.get("report.settlement.noPayments"));

    /**
     * Creates the settlement report view.
     *
     * @param reportService  report client for the order service
     */
    public SettlementReportView(AsyncRestClientReportService reportService) {
        super();
        this.reportService = reportService;
    }

    @Override
    protected String getTitle() {
        return Messages.get("nav.report.settlement");
    }

    @Override
    protected String getDescription() {
        return Messages.get("report.settlement.description");
    }

    @Override
    protected Component createContent() {
        HorizontalLayout cards = new HorizontalLayout(ordersCard, grossCard, netCard,
                discountCard, taxCard, refundCard);
        cards.addClassName("stat-cards");
        cards.setWidthFull();
        cards.setSpacing(true);
        cards.setPadding(false);
        cards.setAlignItems(FlexComponent.Alignment.STRETCH);
        cards.setFlexGrow(1, ordersCard, grossCard, netCard, discountCard, taxCard, refundCard);

        breakdown.setPadding(false);
        breakdownEmpty.addClassName("health-empty");
        breakdownEmpty.setVisible(false);

        VerticalLayout panel = new VerticalLayout(
                new Span(Messages.get("report.settlement.byPayment")), breakdown, breakdownEmpty);
        panel.addClassName("panel");
        panel.setWidthFull();
        panel.setPadding(true);

        VerticalLayout layout = new VerticalLayout(cards, panel);
        layout.setPadding(false);
        layout.setWidthFull();
        return layout;
    }

    @Override
    protected void loadReport(DateRange range) {
        reportService.getSettlementAsync(result -> UiUtil.safeAccess(ui, () -> {
            onReportLoaded();
            render(result);
        }), error -> UiUtil.safeAccess(ui, () -> onReportFailed(error)),
                range.startDateTime(), range.endDateTime());
    }

    /**
     * Fills the stat cards and the payment breakdown from the report.
     *
     * @param report the settlement totals, may be {@code null}
     */
    private void render(SettlementReportDto report) {
        if (report == null) {
            resetCards();
            return;
        }
        ordersCard.setValue(report.getTotalOrders());
        grossCard.setValue(money(report.getTotalSales()));
        netCard.setValue(money(report.getTotalNetSales()));
        discountCard.setValue(money(report.getTotalDiscounts()));
        taxCard.setValue(money(report.getTotalTax()));
        refundCard.setValue(money(report.getTotalRefunds()));
        ordersCard.setWarn(false);
        renderBreakdown(report.getPaymentBreakdown());
    }

    /**
     * Zeroes every card, used when the service answers with an empty payload.
     */
    private void resetCards() {
        ordersCard.setValue(0);
        ordersCard.setWarn(false);
        grossCard.setValue(money(BigDecimal.ZERO));
        netCard.setValue(money(BigDecimal.ZERO));
        discountCard.setValue(money(BigDecimal.ZERO));
        taxCard.setValue(money(BigDecimal.ZERO));
        refundCard.setValue(money(BigDecimal.ZERO));
        breakdown.removeAll();
        breakdownEmpty.setVisible(true);
    }

    /**
     * Renders one row per payment method, largest first, so the dominant tender
     * is visible without reading every row.
     *
     * @param payments payment method to total, may be {@code null}
     */
    private void renderBreakdown(Map<String, BigDecimal> payments) {
        breakdown.removeAll();
        if (ObjectUtils.isEmpty(payments)) {
            breakdownEmpty.setVisible(true);
            return;
        }
        breakdownEmpty.setVisible(false);
        List<Map.Entry<String, BigDecimal>> rows = new ArrayList<>(payments.entrySet());
        rows.sort(Map.Entry.<String, BigDecimal>comparingByValue().reversed());
        rows.forEach(row -> {
            Span method = new Span(row.getKey());
            Span amount = new Span(money(row.getValue()));
            amount.addClassName("tertiary-inline");
            HorizontalLayout line = new HorizontalLayout(method, amount);
            line.setWidthFull();
            line.setJustifyContentMode(HorizontalLayout.JustifyContentMode.BETWEEN);
            breakdown.add(line);
        });
    }

    /**
     * Formats a monetary total, falling back to a zero when it is missing.
     *
     * @param value the amount to format
     * @return the formatted amount
     */
    private static String money(BigDecimal value) {
        return UiUtil.rupiah(value == null ? BigDecimal.ZERO : value);
    }
}
