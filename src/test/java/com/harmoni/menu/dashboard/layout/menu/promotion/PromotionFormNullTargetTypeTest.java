package com.harmoni.menu.dashboard.layout.menu.promotion;

import com.harmoni.menu.dashboard.dto.PromotionDto;
import com.harmoni.menu.dashboard.dto.PromotionTargetDto;
import com.harmoni.menu.dashboard.layout.enums.PromotionTargetType;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Guards that restoring a promotion with a target row that names no type does not throw.
 *
 * <p>{@code PromotionTargetDto.targetType} is nullable, and a row can genuinely arrive
 * nameless: the field is only set by the picker, and a row the backend has not finished
 * classifying comes back null. Two switches on it were reached from that state and threw,
 * which is worse than the empty target it was restoring - the operator opens a saved
 * promotion and gets a stack trace instead of a form.</p>
 *
 * <p>{@code updateTargetButtonText} was the one that actually crashed. It is handed
 * {@code firstTarget.getTargetType()} during a restore, and it switched on the value with
 * no null branch, so a nameless first row threw an NPE and abandoned the rest of the
 * restore - the scope, the names and the targets that followed were all left unapplied.
 * {@code selectTarget} had a {@code case null} but not a null check on the row itself.</p>
 */
class PromotionFormNullTargetTypeTest {

    @Test
    void restoringAPromotionWhoseFirstTargetHasNoType_doesNotThrow() throws Exception {
        PromotionForm form = newForm(PromotionDto.builder()
                .id(1L)
                .targets(List.of(PromotionTargetDto.builder().id(10L).build()))
                .build());

        assertDoesNotThrow(() -> invoke(form, "restoreBean"),
                "a saved row with no target type names nothing, so there is nothing to restore - but it must not "
                        + "abort the restore and leave the operator with a stack trace instead of the form");
    }

    @Test
    void restoringAPromotionWithANullTargetRow_doesNotThrow() throws Exception {
        PromotionForm form = newForm(PromotionDto.builder()
                .id(1L)
                .targets(Arrays.asList(PromotionTargetDto.builder().id(10L).build(), null))
                .build());

        assertDoesNotThrow(() -> invoke(form, "restoreBean"),
                "a null row in the target list is skipped by the other loops over this same list, so this one has "
                        + "to skip it too rather than throw on the row before the type");
    }

    @Test
    void aRestoredTargetWithNoType_leavesTheApplyToFieldEmpty() throws Exception {
        PromotionForm form = newForm(PromotionDto.builder()
                .id(1L)
                .targets(List.of(PromotionTargetDto.builder().id(10L).build()))
                .build());
        invoke(form, "restoreBean");

        assertNull(getApplyToValue(form),
                "there is no type to restore, so the field is cleared rather than defaulted to something the "
                        + "operator never chose - defaulting here would make a nameless row look like a decision");
    }

    @Test
    void aTargetRowWithNoType_doesNotCostThePromotionItsSkuOption() throws Exception {
        PromotionForm form = newForm(PromotionDto.builder()
                .id(1L)
                .targets(List.of(
                        PromotionTargetDto.builder().id(10L).build(),
                        PromotionTargetDto.builder().id(11L)
                                .targetType(PromotionTargetType.SKU).skuId(99L).build()))
                .build());

        assertDoesNotThrow(() -> invoke(form, "restoreBean"),
                "a nameless row ahead of a SKU row must not stop the SKU from being seen, since the SKU type is put "
                        + "back into the dropdown specifically so the saved promotion stays editable");
    }

    private static Object getApplyToValue(PromotionForm form) throws Exception {
        var field = PromotionForm.class.getDeclaredField("applyToSelect");
        field.setAccessible(true);
        return ((com.vaadin.flow.component.select.Select<?>) field.get(form)).getValue();
    }

    /** Builds a form the way {@code onAttach} does; the collaborators are unused here. */
    private static PromotionForm newForm(PromotionDto promotion) throws Exception {
        PromotionForm form = new PromotionForm(null, null, null, null,
                PromotionEditorContext.builder().formAction(FormAction.EDIT).promotionDto(promotion).build());
        invoke(form, "configureFields");
        invoke(form, "configureSpecialPriceGrid");
        invoke(form, "addValidation");
        invoke(form, "addFields");
        return form;
    }

    private static void invoke(PromotionForm form, String name) throws Exception {
        Method method = PromotionForm.class.getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(form);
    }
}
