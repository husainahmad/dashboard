package com.harmoni.menu.dashboard.layout.menu.promotion;

import com.harmoni.menu.dashboard.dto.PromotionDto;
import com.harmoni.menu.dashboard.layout.enums.PromotionStatus;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Guards that the Active checkbox and the lifecycle status never disagree on a new
 * promotion.
 *
 * <p>They used to: the form hard-coded {@code DRAFT} while ticking the checkbox, and
 * since the status is what the menu service stores, a promotion the operator had
 * switched on was previewed as "Active" and "Draft" at the same time, then saved as a
 * draft it was never actually live in.</p>
 */
class PromotionFormStatusTest {

    @Test
    void aNewPromotionWithActiveTicked_isAuthoredAsActive() throws Exception {
        PromotionForm form = newForm(FormAction.CREATE, null);

        assertEquals(PromotionStatus.ACTIVE, form.getStatusSelect().getValue(),
                "the Active checkbox is the only lifecycle control the create form shows, so ticking it has to "
                        + "produce an active promotion rather than a draft that merely looks switched on");
    }

    @Test
    void aNewPromotionWithActiveUnticked_isAuthoredAsDraft() throws Exception {
        PromotionForm form = newForm(FormAction.CREATE, null);
        form.getActiveCheckbox().setValue(false);

        assertEquals(PromotionStatus.DRAFT, form.getStatusSelect().getValue(),
                "an unticked checkbox must not author a live promotion");
    }

    @Test
    void untickingAndRetickingActive_onANewPromotion_tracksTheCheckbox() throws Exception {
        PromotionForm form = newForm(FormAction.CREATE, null);

        form.getActiveCheckbox().setValue(false);
        assertEquals(PromotionStatus.DRAFT, form.getStatusSelect().getValue());
        form.getActiveCheckbox().setValue(true);
        assertEquals(PromotionStatus.ACTIVE, form.getStatusSelect().getValue());
    }

    @Test
    void theStatusSubmittedForANewPromotion_matchesTheCheckbox() throws Exception {
        PromotionForm form = newForm(FormAction.CREATE, null);

        assertEquals(PromotionStatus.ACTIVE, form.buildAggregate().getStatus(),
                "the status that reaches the menu service has to be the one the checkbox asks for, or the "
                        + "saved promotion is not the promotion the operator configured");
    }

    @Test
    void theStatusSubmittedAfterUntickingActive_isDraft() throws Exception {
        PromotionForm form = newForm(FormAction.CREATE, null);
        form.getActiveCheckbox().setValue(false);

        assertEquals(PromotionStatus.DRAFT, form.buildAggregate().getStatus());
    }

    @Test
    void restoringTwice_doesNotDriftTheStatus() throws Exception {
        PromotionForm form = newForm(FormAction.CREATE, null);
        invoke(form, "restoreBean");

        assertEquals(PromotionStatus.ACTIVE, form.getStatusSelect().getValue(),
                "onAttach runs again on every re-attach, and a second pass that flipped the status would leave "
                        + "the checkbox and the status disagreeing for no reason");
    }

    @Test
    void editingAPausedPromotion_leavesItsStatusAlone() throws Exception {
        PromotionDto paused = PromotionDto.builder()
                .id(7L)
                .name("Lunch special")
                .code("LUNCH")
                .status(PromotionStatus.PAUSED)
                .active(true)
                .build();
        PromotionForm form = newForm(FormAction.EDIT, paused);

        assertEquals(PromotionStatus.PAUSED, form.buildAggregate().getStatus(),
                "the status belongs to the status endpoint, so editing an unrelated field must not quietly "
                        + "reactivate a promotion an operator deliberately paused");
    }

    @Test
    void aDraftPromotionBeingEdited_isNotActivatedByTheTickedCheckbox() throws Exception {
        PromotionDto draft = PromotionDto.builder()
                .id(8L)
                .name("Lunch special")
                .code("LUNCH")
                .status(PromotionStatus.DRAFT)
                .active(true)
                .build();
        PromotionForm form = newForm(FormAction.EDIT, draft);

        assertFalse(form.getStatusSelect().getValue() == PromotionStatus.ACTIVE,
                "ticking Active while editing a draft must not be read as a lifecycle transition");
        assertEquals(PromotionStatus.DRAFT, form.buildAggregate().getStatus());
    }

    @Test
    void editingWithoutASavedPromotion_stillAuthorsTheStatus() throws Exception {
        PromotionForm form = newForm(FormAction.EDIT, null);

        assertEquals(PromotionStatus.ACTIVE, form.getStatusSelect().getValue(),
                "the create branch also covers a missing dto, so it has to keep the two fields in step");
    }

    /**
     * Builds a form the way {@code onAttach} does.
     *
     * <p>The collaborators are null because nothing on this path reaches for them: the
     * form only uses them when saving or when a target is picked.</p>
     */
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

    private static void invoke(PromotionForm form, String name) throws Exception {
        Method method = PromotionForm.class.getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(form);
    }
}
