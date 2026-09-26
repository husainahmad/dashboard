package com.harmoni.menu.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * A recurring weekly window during which a promotion may be redeemed. A promotion
 * is live when the current weekday and time fall inside at least one enabled
 * window; a window whose end time is earlier than its start time wraps past
 * midnight.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PromotionScheduleDto {

    /** The database id, or {@code null} for a row that has not been saved yet. */

    private Long id;

    /** The day this window applies to, where {@code MONDAY} is 1 and {@code SUNDAY} is 7. */

    private DayOfWeek dayOfWeek;

    /** Inclusive window start. */

    private LocalTime startTime;

    /** Exclusive window end; may be earlier than {@link #startTime} to wrap midnight. */

    private LocalTime endTime;

    /** Whether the window is currently taken into account; defaults to {@code true}. */

    private Boolean enabled;

}
