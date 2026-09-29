package com.harmoni.menu.dashboard.layout.menu.promotion;

import com.harmoni.menu.dashboard.dto.PromotionDto;
import com.harmoni.menu.dashboard.dto.PromotionScheduleDto;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.vaadin.flow.component.checkbox.Checkbox;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards that the day checkboxes and the submitted day list cannot disagree.
 *
 * <p>They used to: {@code Checkbox.setValue} does not fire a value change event when
 * the new value equals the current one, and the create-time default already leaves
 * Monday to Friday ticked. Restoring a promotion scheduled on those days therefore
 * left the backing list empty while every checkbox still looked correctly ticked, and
 * submitting the form was rejected with "select at least one day of the week".</p>
 */
class PromotionFormDaysTest {

    @Test
    void editingAPromotionScheduledOnTheFormDefaults_restoresEveryDay() throws Exception {
        PromotionForm form = formWithSchedules(schedule(DayOfWeek.MONDAY), schedule(DayOfWeek.TUESDAY),
                schedule(DayOfWeek.WEDNESDAY), schedule(DayOfWeek.THURSDAY), schedule(DayOfWeek.FRIDAY));

        assertEquals(List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY), daysOf(form),
                "a promotion scheduled Monday to Friday submits the same five days, because those are the "
                        + "days the form had already ticked before the restore overwrote the list");
    }

    @Test
    void editingAPromotionScheduledOnASubsetOfTheFormDefaults_restoresThatSubset() throws Exception {
        PromotionForm form = formWithSchedules(schedule(DayOfWeek.MONDAY), schedule(DayOfWeek.WEDNESDAY));

        assertEquals(List.of(DayOfWeek.MONDAY, DayOfWeek.WEDNESDAY), daysOf(form),
                "the days the operator deselected on a weekday must be dropped, and the rest kept");
    }

    @Test
    void editingAPromotionScheduledEveryDay_keepsAllSeven() throws Exception {
        PromotionForm form = formWithSchedules(Arrays.stream(DayOfWeek.values())
                .map(PromotionFormDaysTest::schedule)
                .toArray(PromotionScheduleDto[]::new));

        assertEquals(List.of(DayOfWeek.values()), daysOf(form));
    }

    @Test
    void editingAPromotionScheduledOnTheWeekend_clearsTheWeekdayDefaults() throws Exception {
        PromotionForm form = formWithSchedules(schedule(DayOfWeek.SATURDAY), schedule(DayOfWeek.SUNDAY));

        assertEquals(List.of(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY), daysOf(form),
                "a weekend-only promotion must not inherit the Monday to Friday default");
    }

    @Test
    void aScheduleRowWithoutTheEnabledFlag_isTreatedAsLive() throws Exception {
        PromotionScheduleDto schedule = schedule(DayOfWeek.MONDAY);
        schedule.setEnabled(null);
        PromotionForm form = formWithSchedules(schedule);

        assertEquals(List.of(DayOfWeek.MONDAY), daysOf(form),
                "the flag documents a default of true and this form only ever writes true, so a row that "
                        + "arrives without it is a live window rather than a disabled one");
    }

    @Test
    void aScheduleRowExplicitlyDisabled_isNotSelected() throws Exception {
        PromotionScheduleDto schedule = schedule(DayOfWeek.MONDAY);
        schedule.setEnabled(false);
        PromotionForm form = formWithSchedules(schedule);

        assertTrue(daysOf(form).isEmpty(), "an explicitly disabled window must not come back as a selected day");
    }

    @Test
    void aScheduleRowWithoutTheEnabledFlag_stillRestoresTheTimeWindow() throws Exception {
        PromotionScheduleDto schedule = schedule(DayOfWeek.MONDAY);
        schedule.setEnabled(null);
        PromotionForm form = formWithSchedules(schedule);

        assertEquals(LocalTime.of(9, 0), timePicker(form, "startTimePicker").getValue());
        assertEquals(LocalTime.of(12, 0), timePicker(form, "endTimePicker").getValue(),
                "a row without the flag still carries the window the promotion runs in");
    }

    @Test
    void aNewPromotion_startsOnTheWeekdayDefaults() throws Exception {
        PromotionForm form = newForm(FormAction.CREATE, null);

        assertEquals(List.of(DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                        DayOfWeek.FRIDAY), daysOf(form),
                "a new promotion keeps the Monday to Friday default");
    }

    @Test
    void everyTickedCheckbox_isSubmitted() throws Exception {
        PromotionForm form = formWithSchedules(schedule(DayOfWeek.MONDAY), schedule(DayOfWeek.SATURDAY));

        for (Map.Entry<DayOfWeek, Checkbox> entry : checkboxes(form).entrySet()) {
            assertEquals(entry.getValue().getValue(), daysOf(form).contains(entry.getKey()),
                    entry.getKey() + " is ticked in the form, so the submitted day list has to contain it "
                            + "too - a mismatch is what makes the submit fail on a form that looks complete");
        }
    }

    @Test
    void submittingAnEditedPromotion_keepsTheSavedDays() throws Exception {
        PromotionForm form = formWithSchedules(schedule(DayOfWeek.MONDAY), schedule(DayOfWeek.FRIDAY));

        assertEquals(List.of(DayOfWeek.MONDAY, DayOfWeek.FRIDAY),
                form.buildAggregate().getSchedules().stream().map(PromotionScheduleDto::getDayOfWeek).toList(),
                "the days written to the payload are the days the operator sees ticked");
    }

    @Test
    void aScheduleRowForAnUnknownDay_doesNotBreakTheRestore() throws Exception {
        PromotionScheduleDto schedule = schedule(DayOfWeek.MONDAY);
        schedule.setDayOfWeek(null);
        PromotionForm form = formWithSchedules(schedule);

        assertTrue(daysOf(form).isEmpty(),
                "a row with no day contributes nothing rather than failing the whole restore");
    }

    /**
     * Builds a form the way {@code onAttach} does, carrying the given schedule rows.
     *
     * <p>The collaborators are null because nothing on this path reaches for them: the
     * form only uses them when saving or when a target is picked.</p>
     */
    private static PromotionForm formWithSchedules(PromotionScheduleDto... schedules) throws Exception {
        PromotionDto promotion = PromotionDto.builder()
                .id(1L)
                .name("Lunch special")
                .code("LUNCH")
                .schedules(List.of(schedules))
                .build();
        return newForm(FormAction.EDIT, promotion);
    }

    private static PromotionForm newForm(FormAction formAction, PromotionDto promotion) throws Exception {
        PromotionForm form = new PromotionForm(null, null, null, null,
                PromotionEditorContext.builder().formAction(formAction).promotionDto(promotion).build());
        invoke(form, "configureFields");
        invoke(form, "configureSpecialPriceGrid");
        invoke(form, "addValidation");
        invoke(form, "addFields");
        invoke(form, "restoreBean");
        return form;
    }

    private static PromotionScheduleDto schedule(DayOfWeek day) {
        return PromotionScheduleDto.builder()
                .dayOfWeek(day)
                .startTime(LocalTime.of(9, 0))
                .endTime(LocalTime.of(12, 0))
                .enabled(true)
                .build();
    }

    /** The submitted day list, in the order the checkboxes are laid out. */
    private static List<DayOfWeek> daysOf(PromotionForm form) throws Exception {
        @SuppressWarnings("unchecked")
        List<DayOfWeek> selectedDays = (List<DayOfWeek>) field(form, "selectedDays");
        return List.copyOf(selectedDays);
    }

    @SuppressWarnings("unchecked")
    private static Map<DayOfWeek, Checkbox> checkboxes(PromotionForm form) throws Exception {
        return (Map<DayOfWeek, Checkbox>) field(form, "dayCheckboxes");
    }

    private static com.vaadin.flow.component.timepicker.TimePicker timePicker(PromotionForm form, String name)
            throws Exception {
        return (com.vaadin.flow.component.timepicker.TimePicker) field(form, name);
    }

    private static void invoke(PromotionForm form, String name) throws Exception {
        Method method = PromotionForm.class.getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(form);
    }

    private static Object field(PromotionForm form, String name) throws Exception {
        Field field = PromotionForm.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(form);
    }
}
