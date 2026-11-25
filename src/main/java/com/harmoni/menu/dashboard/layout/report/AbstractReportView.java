package com.harmoni.menu.dashboard.layout.report;

import com.harmoni.menu.dashboard.layout.ai.AiContextProvider;
import com.harmoni.menu.dashboard.layout.ai.AiPageContext;
import com.harmoni.menu.dashboard.layout.ai.AiTools;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.layout.util.GridSkeleton;
import com.harmoni.menu.dashboard.layout.util.LoadingBar;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * Shared shell for the five order service reports.
 *
 * <p>The page owns the Brand/Chain/Store and date-range filter and pushes it to
 * each report through {@link #applyFilter(DateRange, List)}. A report builds its
 * body once, then reloads whenever the filter changes.</p>
 */
@Slf4j
public abstract class AbstractReportView extends VerticalLayout implements AiContextProvider {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    /** The UI this view is attached to; used to marshal callbacks back to the session. */
    protected UI ui;

    private final LoadingBar loadingBar = new LoadingBar();

    private final VerticalLayout content = new VerticalLayout();

    /** The range currently applied to this report. */
    protected DateRange range;

    /** Store ids the report is scoped to; empty means the signed-in user's store. */
    protected List<Integer> storeIds = List.of();

    private boolean rendered;

    private boolean loading;

    /**
     * Creates the report shell.
     */
    protected AbstractReportView() {
        setPadding(false);
        setSizeFull();
        setSpacing(false);
        addClassName(Css.LIST_VIEW);
        add(loadingBar);
        content.setPadding(false);
        content.setWidthFull();
        add(content);
        setFlexGrow(1, content);
    }

    /**
     * Renders the report body and loads it with the filter already applied, if any.
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
        if (range != null) {
            load(range);
        }
    }

    /**
     * Applies the page filter and reloads the report when it is already rendered.
     *
     * @param range    the selected date range
     * @param storeIds the store ids in scope, empty for the user's store
     */
    public void applyFilter(DateRange range, List<Integer> storeIds) {
        this.range = range;
        this.storeIds = storeIds == null ? List.of() : List.copyOf(storeIds);
        if (rendered) {
            load(range);
        }
    }

    @Override
    public AiPageContext aiPageContext() {
        StringBuilder summary = new StringBuilder();
        summary.append(Messages.get("ai.context.description")).append(": ")
                .append(getDescription()).append('\n');
        if (range != null) {
            summary.append(Messages.get("ai.context.dateRange")).append(": ")
                    .append(DATE_FORMAT.format(range.start())).append(" - ")
                    .append(DATE_FORMAT.format(range.end())).append('\n');
        }
        summary.append(Messages.get("ai.context.storeScope")).append(": ")
                .append(storeIds.isEmpty()
                        ? Messages.get("ai.context.currentStore")
                        : String.join(", ", storeIds.stream().map(String::valueOf).toList()))
                .append('\n');
        String detail = reportContext();
        if (detail != null && !detail.isBlank()) {
            summary.append(detail);
        }
        return new AiPageContext(getTitle(), summary.toString(), AiTools.SALES_REPORT);
    }

    protected String reportContext() {
        return "";
    }

    /**
     * Returns the report title, used for logging.
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
     * reuses the currently applied filter.
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
     * Re-runs the report for the filter currently applied.
     */
    private void reload() {
        if (range != null) {
            load(range);
        }
    }

    /**
     * Starts a report request for the given range, guarding against overlapping
     * requests.
     *
     * @param range the date range to report on
     */
    private void load(DateRange range) {
        if (loading || range == null || !range.isValid()) {
            return;
        }
        loading = true;
        loadingBar.start();
        showLoading();
        loadReport(range);
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
