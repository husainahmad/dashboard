package com.harmoni.menu.dashboard.layout.report;

import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Toolbar shared by every report view: a start and end date picker plus an apply
 * button, since all five order service reports are date-ranged.
 *
 * <p>Defaults to the last seven days, which covers a typical week of trading
 * without flooding the tables. The range is only applied when the user confirms
 * it, so a half-typed date does not trigger a request on every keystroke.</p>
 */
public class ReportRangeBar extends HorizontalLayout {

    private static final int DEFAULT_RANGE_DAYS = 7;

    private final DatePicker startPicker = new DatePicker(Messages.get("report.range.start"));

    private final DatePicker endPicker = new DatePicker(Messages.get("report.range.end"));

    private final List<Consumer<DateRange>> listeners = new ArrayList<>();

    /**
     * Builds the range bar, pre-filled with the last seven days and wired to
     * {@code onApply}.
     */
    public ReportRangeBar(Consumer<DateRange> onApply) {
        LocalDate today = LocalDate.now();
        startPicker.setValue(today.minusDays(DEFAULT_RANGE_DAYS - 1L));
        endPicker.setValue(today);
        startPicker.setRequired(true);
        endPicker.setRequired(true);

        Button apply = new Button(Messages.get("report.range.apply"), VaadinIcon.REFRESH.create(),
                event -> apply());
        apply.addClassName("small-button");

        add(startPicker, endPicker, apply);
        addClassName(Css.TOOLBAR);
        setWidthFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setPadding(false);
        listeners.add(onApply);
    }

    /**
     * Registers an extra listener notified with the range once it is applied.
     *
     * @param listener the listener to notify
     */
    public void addRangeListener(Consumer<DateRange> listener) {
        listeners.add(listener);
    }

    /**
     * Returns the currently selected range.
     *
     * @return the selected range
     */
    public DateRange getRange() {
        return new DateRange(startPicker.getValue(), endPicker.getValue());
    }

    /**
     * Validates the selected range and, when it is usable, notifies every
     * listener. An inverted or incomplete range is reported instead of being
     * silently sent to the service, which would answer with an empty report.
     */
    private void apply() {
        DateRange range = getRange();
        if (!range.isValid()) {
            UiUtil.error(Messages.get("report.range.invalid"));
            return;
        }
        listeners.forEach(listener -> listener.accept(range));
    }
}
