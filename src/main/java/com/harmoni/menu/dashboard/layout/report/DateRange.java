package com.harmoni.menu.dashboard.layout.report;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * An inclusive date range selected on a report view.
 *
 * <p>The order service expects ISO local date-times, so the range is widened to
 * cover the whole of each selected day: the start becomes the first instant of
 * the start day and the end the last second of the end day. This keeps a
 * same-day report from silently excluding orders placed late in the evening.</p>
 *
 * @param start first day of the range, inclusive
 * @param end   last day of the range, inclusive
 */
public record DateRange(LocalDate start, LocalDate end) {

    /**
     * Whether the range is usable, meaning both days are set and the start does
     * not fall after the end.
     *
     * @return whether the range can be requested
     */
    public boolean isValid() {
        return start != null && end != null && !start.isAfter(end);
    }

    /**
     * Returns the start of the range as the first instant of the start day.
     *
     * @return the inclusive range start
     */
    public LocalDateTime startDateTime() {
        return start.atStartOfDay();
    }

    /**
     * Returns the end of the range as the last second of the end day, so orders
     * placed at the end of the day are included.
     *
     * @return the inclusive range end
     */
    public LocalDateTime endDateTime() {
        return end.atTime(LocalTime.of(23, 59, 59));
    }
}
