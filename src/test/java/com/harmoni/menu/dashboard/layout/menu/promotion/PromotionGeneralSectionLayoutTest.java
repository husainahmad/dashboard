package com.harmoni.menu.dashboard.layout.menu.promotion;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards how the general section arranges its fields.
 *
 * <p>Name and code each held a full row of their own, which spent two rows on the two
 * shortest single-line fields in the form. They share a row now. The description stays
 * full width, because it is the one field here with a length worth the space.</p>
 */
class PromotionGeneralSectionLayoutTest {

    @Test
    void nameAndCode_shareARow() throws Exception {
        PromotionForm form = newForm();
        FormLayout general = generalSection(form);
        List<Component> fields = general.getChildren().toList();

        int name = fields.indexOf(field(form, "nameField"));
        int code = fields.indexOf(field(form, "codeField"));

        assertTrue(name >= 0 && code >= 0,
                "both fields belong to the general section - found " + describeOrder(fields));
        assertEquals(1, code - name,
                "name and code have to be adjacent for them to land on the same row, and nothing may come "
                        + "between them - found " + describeOrder(fields));
    }

    @Test
    void neitherNameNorCode_takesTheWholeRow() throws Exception {
        PromotionForm form = newForm();
        FormLayout general = generalSection(form);

        // A colspan of 2 is exactly what put each one on a row of its own before.
        assertEquals(1, general.getColspan(field(form, "nameField")),
                "name spans the section's full width, so it cannot share a row with the code beside it");
        assertEquals(1, general.getColspan(field(form, "codeField")),
                "code spans the section's full width, so it cannot share a row with the name beside it");
    }

    @Test
    void theDescription_stillTakesTheWholeRow() throws Exception {
        PromotionForm form = newForm();
        assertEquals(2, generalSection(form).getColspan(field(form, "descriptionField")),
                "the description is the one field in this section with a length that earns a row of its own, "
                        + "and sharing a row would squeeze it to half the section");
    }

    @Test
    void theSection_stillCollapsesToASingleColumn() throws Exception {
        List<FormLayout.ResponsiveStep> steps = generalSection(newForm()).getResponsiveSteps();

        assertNotNull(steps, "the section declares responsive steps");
        // ResponsiveStep exposes no getters - it only serialises - so the step is read back
        // the way Vaadin itself reads it, rather than by reaching into its fields.
        boolean stacks = steps.stream().map(FormLayout.ResponsiveStep::toJson).anyMatch(json ->
                json.hasKey("minWidth") && json.hasKey("columns")
                        && "0".equals(json.getString("minWidth"))
                        && json.getNumber("columns") == 1);

        assertTrue(stacks,
                "name and code only share a row once the form is wide enough for two columns; the "
                        + "single-column step is what stacks them again on a narrow form - found " + steps.stream()
                        .map(FormLayout.ResponsiveStep::toJson).toList());
    }

    @Test
    void theGeneralSection_isFoundRatherThanAssumed() throws Exception {
        // The layout is a local inside addFields, so this is the only handle on it. If it ever
        // stops being reachable the assertions above are checking nothing, hence this test.
        assertNotNull(generalSection(newForm()),
                "the general section's layout is reachable in the component tree");
    }

    /** The general section's layout, located by the field that only it holds. */
    private static FormLayout generalSection(PromotionForm form) throws Exception {
        Component name = field(form, "nameField");

        List<FormLayout> layouts = new ArrayList<>();
        collectLayouts(form, layouts);
        return layouts.stream()
                .filter(layout -> layout.getChildren().anyMatch(child -> child == name))
                .findFirst()
                .orElseThrow(() -> new AssertionError(
                        "no form layout holds the name field, so the general section's layout has moved"));
    }

    private static void collectLayouts(Component node, List<FormLayout> found) {
        if (node instanceof FormLayout layout) {
            found.add(layout);
        }
        node.getChildren().forEach(child -> collectLayouts(child, found));
    }

    /**
     * Read off the very form being inspected. Building a second form for the lookup would
     * compare a field from one instance against a layout from another, and find neither.
     */
    private static Component field(PromotionForm form, String name) throws Exception {
        Field declared = PromotionForm.class.getDeclaredField(name);
        declared.setAccessible(true);
        return (Component) declared.get(form);
    }

    private static PromotionForm newForm() throws Exception {
        PromotionForm form = new PromotionForm(null, null, null, null, FormAction.CREATE, null, null);
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

    private static String describeOrder(List<Component> fields) {
        return fields.stream()
                .map(child -> child.getClass().getSimpleName())
                .reduce((left, right) -> left + " then " + right)
                .orElse("no fields");
    }
}
