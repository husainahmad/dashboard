package com.harmoni.menu.dashboard.layout.report;

import com.harmoni.menu.dashboard.dto.report.OrderVolumeReportDto;
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

/**
 * How many orders the store took over a date range, split by peak and off-peak
 * hours.
 *
 * <p>The split is shown both as counts and as a share bar, because the useful
 * question about order volume is usually how lopsided the day was rather than
 * the raw numbers.</p>
 */
@Route(value = OrderVolumeReportView.ROUTE, layout = com.harmoni.menu.dashboard.layout.MainLayout.class)
@PageTitle("Order volume | POSHarmoni")
public class OrderVolumeReportView extends AbstractReportView {

    /** Route the order volume report is reachable at. */
    public static final String ROUTE = "report/order-volume";

    private final transient AsyncRestClientReportService reportService;

    private final StatCard totalCard = new StatCard(VaadinIcon.CART, Messages.get("report.orderVolume.total"));

    private final StatCard peakCard = new StatCard(VaadinIcon.CLOCK, Messages.get("report.orderVolume.peak"));

    private final StatCard offPeakCard =
            new StatCard(VaadinIcon.HOURGLASS, Messages.get("report.orderVolume.offPeak"));

    private final Span shareCaption = new Span();

    private final HorizontalLayout shareBar = new HorizontalLayout();

    /**
     * Creates the order volume report view.
     *
     * @param reportService report client for the order service
     */
    public OrderVolumeReportView(AsyncRestClientReportService reportService) {
        super();
        this.reportService = reportService;
    }

    @Override
    protected String getTitle() {
        return Messages.get("nav.report.orderVolume");
    }

    @Override
    protected String getDescription() {
        return Messages.get("report.orderVolume.description");
    }

    @Override
    protected Component createContent() {
        HorizontalLayout cards = new HorizontalLayout(totalCard, peakCard, offPeakCard);
        cards.addClassName("stat-cards");
        cards.setWidthFull();
        cards.setSpacing(true);
        cards.setPadding(false);
        cards.setAlignItems(FlexComponent.Alignment.STRETCH);
        cards.setFlexGrow(1, totalCard, peakCard, offPeakCard);

        shareCaption.addClassName("health-caption");
        shareBar.setWidthFull();
        shareBar.setHeight("24px");
        shareBar.setPadding(false);
        shareBar.addClassName("stat-cards");

        VerticalLayout panel = new VerticalLayout(
                new Span(Messages.get("report.orderVolume.split")), shareCaption, shareBar);
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
        reportService.getOrderVolumeAsync(result -> UiUtil.safeAccess(ui, () -> {
            onReportLoaded();
            render(result);
        }), error -> UiUtil.safeAccess(ui, () -> onReportFailed(error)),
                range.startDateTime(), range.endDateTime());
    }

    /**
     * Fills the stat cards and the peak/off-peak split bar.
     *
     * @param report the order volume totals, may be {@code null}
     */
    private void render(OrderVolumeReportDto report) {
        int total = report == null ? 0 : value(report.getTotalOrders());
        int peak = report == null ? 0 : value(report.getPeakTimeOrders());
        int offPeak = report == null ? 0 : value(report.getNonPeakTimeOrders());

        totalCard.setValue(total);
        totalCard.setWarn(false);
        peakCard.setValue(peak);
        offPeakCard.setValue(offPeak);
        renderSplit(peak, offPeak, total);
    }

    /**
     * Renders the two-segment split bar, falling back to the caption alone when
     * the range had no orders at all.
     *
     * @param peak    orders taken during peak hours
     * @param offPeak orders taken outside peak hours
     * @param total   total orders in the range
     */
    private void renderSplit(int peak, int offPeak, int total) {
        shareBar.removeAll();
        if (total <= 0) {
            shareCaption.setText(Messages.get("report.orderVolume.empty"));
            return;
        }
        int peakPercent = Math.round(peak * 100f / total);
        int offPeakPercent = 100 - peakPercent;
        shareCaption.setText(Messages.get("report.orderVolume.share", peakPercent, offPeakPercent));
        shareBar.add(segment(peakPercent, Messages.get("report.orderVolume.peak")),
                segment(offPeakPercent, Messages.get("report.orderVolume.offPeak")));
    }

    /**
     * Builds one labelled segment of the split bar.
     *
     * @param percent the segment's share of the total
     * @param label   the label shown inside the segment
     * @return the styled segment
     */
    private static Span segment(int percent, String label) {
        Span span = new Span(percent + "% " + label);
        span.addClassName(percent > 0 ? "stat-value--ok" : "stat-value--warn");
        span.getStyle().set("flex", String.valueOf(Math.max(percent, 1)));
        span.getStyle().set("padding", "0 8px");
        return span;
    }

    /**
     * Returns a count, defaulting to zero when the service omitted it.
     *
     * @param value the reported count
     * @return the count, or zero
     */
    private static int value(Integer value) {
        return value == null ? 0 : value;
    }
}
