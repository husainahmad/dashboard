package com.harmoni.menu.dashboard.layout.menu.promotion;

import com.harmoni.menu.dashboard.layout.enums.PromotionType;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the promotion form's validation wiring, which the aggregate check in
 * {@link PromotionForm#isAggregateValid()} relies on.
 *
 * <p>The discount field is hidden for every promotion type that does not use it, so
 * a required-value validator bound to it fails the whole form for a reason the
 * operator can neither see nor correct: the generic "please correct the highlighted
 * fields" toast appears while nothing is highlighted, and the specific "enter a
 * discount greater than zero" message beneath it can never be reached.</p>
 */
class PromotionFormValidationTest {

    @Test
    void hiddenDiscountField_doesNotFailAFormThatIsOtherwiseComplete() throws Exception {
        PromotionForm form = formTypedAs(PromotionType.SPECIAL_PRICE);
        completeIdentityFields(form);

        assertFalse(binderOf(form).validate().hasErrors(),
                "a special price promotion never fills the discount, because the field is hidden for that "
                        + "type, so a validator on it can only ever raise a form error the operator cannot "
                        + "see or fix");
    }

    @Test
    void aBlankName_stillFailsTheForm() throws Exception {
        PromotionForm form = formTypedAs(PromotionType.SPECIAL_PRICE);
        completeIdentityFields(form);
        text(form, "nameField").setValue("");

        assertTrue(binderOf(form).validate().hasErrors(),
                "dropping the discount requirement must not have relaxed the name, which a special price "
                        + "promotion genuinely cannot omit");
    }

    @Test
    void aBlankCode_stillFailsTheForm() throws Exception {
        PromotionForm form = formTypedAs(PromotionType.SPECIAL_PRICE);
        completeIdentityFields(form);
        text(form, "codeField").setValue("");

        assertTrue(binderOf(form).validate().hasErrors(),
                "dropping the discount requirement must not have relaxed the code, which a special price "
                        + "promotion genuinely cannot omit");
    }

    /**
     * Builds a form the way {@code onAttach} does and types it, leaving the identity
     * fields empty for the test to fill in.
     *
     * <p>The collaborators are null because nothing on this path reaches for them: the
     * form only uses them when saving or when a target is picked.</p>
     */
    private static PromotionForm formTypedAs(PromotionType type) throws Exception {
        PromotionForm form = new PromotionForm(null, null, null, null,
                PromotionEditorContext.builder().formAction(FormAction.CREATE).build());

        invoke(form, "configureFields");
        invoke(form, "addValidation");

        select(form, "typeSelect").setValue(type);
        return form;
    }

    private static void completeIdentityFields(PromotionForm form) throws Exception {
        text(form, "nameField").setValue("Lunch special");
        text(form, "codeField").setValue("LUNCH");
    }

    @SuppressWarnings("unchecked")
    private static Binder<?> binderOf(PromotionForm form) throws Exception {
        return (Binder<?>) field(form, "binder");
    }

    private static void invoke(PromotionForm form, String name) throws Exception {
        Method method = PromotionForm.class.getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(form);
    }

    private static TextField text(PromotionForm form, String name) throws Exception {
        return (TextField) field(form, name);
    }

    @SuppressWarnings("unchecked")
    private static <T> Select<T> select(PromotionForm form, String name) throws Exception {
        return (Select<T>) field(form, name);
    }

    private static Object field(PromotionForm form, String name) throws Exception {
        Field field = PromotionForm.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(form);
    }
}
