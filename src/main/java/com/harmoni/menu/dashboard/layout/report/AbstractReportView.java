package com.harmoni.menu.dashboard.layout.report;

import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.layout.util.GridSkeleton;
import com.harmoni.menu.dashboard.layout.util.LoadingBar;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Shared shell for the five order service reports.
 *
 * <p>All of them take the same {@code start}/{@code end} range, and all of them
 * are scoped to the signed-in store by the API gateway, so each view only has to
 * describe its own content and how to request it. This class owns the title, the
 * {@link ReportRangeBar}, the loading bar and the error handling, and calls
 * {@link #loadReport(DateRange)} whenever a range is applied.</p>
 *
 * <p>Subclasses build their body from {@link #createContent()} rather than their
 * constructor, because Vaadin instantiates the view before the layout is built
 * and any field initialisers must already have run by then.</p>
 */
@Slf4j
public abstract class AbstractReportView extends VerticalLayout {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    /** The UI this view is attached to; used to marshal callbacks back to the session. */
    protected UI ui;

    private final LoadingBar loadingBar = new LoadingBar();

    private final ReportRangeBar rangeBar;

    private final VerticalLayout content = new VerticalLayout();

    private boolean rendered;

    private boolean loading;

    /**
     * Creates the report shell, wiring the range bar to {@link #load(DateRange)}.
     */
    protected AbstractReportView() {
        this.rangeBar = new ReportRangeBar(this::load);
        setPadding(false);
        setSizeFull();
        addClassName(Css.LIST_VIEW);
        add(loadingBar);
        add(createHeader());
        add(rangeBar);
        content.setPadding(false);
        content.setWidthFull();
        add(content);
        setFlexGrow(1, content);
    }

    /**
     * Renders the report body and loads the initial range. Guarded so that
     * re-attaching the same view does not stack a second copy of the layout.
     *
     * @param attachEvent the attach event
     */
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (ui == null) {
            ui = attachEvent.getUI();
        }
        if (rendered) {
            return;
        }
        rendered = true;
        content.add(createContent());
        load(rangeBar.getRange());
    }

    /**
     * Returns the report title shown above the range bar.
     *
     * @return the report title
     */
    protected abstract String getTitle();

    /**
     * Returns a short explanation of what the report covers.
     *
     * @return the report description
     */
    protected abstract String getDescription();

    /**
     * Builds the report body, for example a grid or a row of stat cards.
     *
     * @return the report content
     */
    protected abstract Component createContent();

    /**
     * Requests the report for the given range. Implementations must eventually
     * call {@link #onReportLoaded()} or {@link #onReportFailed(Throwable)}.
     *
     * @param range the selected date range
     */
    protected abstract void loadReport(DateRange range);

    /**
     * Shows this view's loading affordances, for example a grid skeleton.
     */
    protected void showLoading() {
        // Views without a skeleton rely on the progress bar alone.
    }

    /**
     * Hides this view's loading affordances once a response has landed.
     */
    protected void hideLoading() {
        // Views without a skeleton rely on the progress bar alone.
    }

    /**
     * Returns the content slot, so subclasses can reach sibling components.
     *
     * @return the content layout
     */
    protected VerticalLayout getContent() {
        return content;
    }

    /**
     * Wraps a grid and its skeleton in the shared full-height grid slot, so
     * report tables get the same sizing and padding as the admin lists.
     *
     * @param grid     the report grid
     * @param skeleton the placeholder shown while loading
     * @return the sized slot holding the grid and skeleton
     */
    protected static Component gridSlot(Component grid, GridSkeleton skeleton) {
        VerticalLayout slot = new VerticalLayout(grid, skeleton);
        slot.addClassName("grid-slot");
        slot.setSizeFull();
        slot.setPadding(false);
        slot.setFlexGrow(1, grid);
        HorizontalLayout wrapper = new HorizontalLayout(slot);
        wrapper.setFlexGrow(1, slot);
        wrapper.addClassNames("content");
        wrapper.setSizeFull();
        return wrapper;
    }

    /**
     * Marks a report request as finished, stopping the progress bar and
     * revealing the rendered data.
     */
    protected void onReportLoaded() {
        loading = false;
        loadingBar.stop();
        hideLoading();
    }

    /**
     * Handles a failed report request by logging it and offering a retry that
     * reuses the currently selected range.
     *
     * @param error the failure
     */
    protected void onReportFailed(Throwable error) {
        loading = false;
        loadingBar.stop();
        hideLoading();
        log.error("Could not load {}", getTitle(), error);
        UiUtil.errorWithRetry(Messages.get("notification.report.loadFailed"), this::reload);
    }

    /**
     * Re-runs the report for the range currently shown in the range bar.
     */
    private void reload() {
        load(rangeBarRange());
    }

    /**
     * Starts a report request for the given range, guarding against overlapping
     * requests.
     *
     * @param range the date range to report on
     */
    private void load(DateRange range) {
        if (loading || !range.isValid()) {
            return;
        }
        loading = true;
        loadingBar.start();
        showLoading();
        loadReport(range);
    }

    /**
     * Returns the range currently selected in the view's range bar.
     *
     * @return the selected range
     */
    private DateRange rangeBarRange() {
        return rangeBar.getRange();
    }

    /**
     * Builds the report title and its explanatory caption.
     *
     * @return the header layout
     */
    private VerticalLayout createHeader() {
        H2 title = new H2(getTitle());
        Paragraph description = new Paragraph(getDescription());
        description.addClassName(Css.HEALTH_CAPTION);

        VerticalLayout header = new VerticalLayout(title, description);
        header.setPadding(false);
        header.setAlignItems(FlexComponent.Alignment.START);
        header.setWidthFull();
        return header;
    }

    /**
     * Formats a report date for display, falling back to {@code -} for a value
     * that is missing or not an ISO date.
     *
     * @param date the ISO date to format
     * @return the formatted date
     */
    protected static String formatDate(String date) {
        if (ObjectUtils.isEmpty(date)) {
            return "-";
        }
        try {
            return DATE_FORMAT.format(LocalDate.parse(date));
        } catch (DateTimeParseException e) {
            return date;
        }
    }
}
