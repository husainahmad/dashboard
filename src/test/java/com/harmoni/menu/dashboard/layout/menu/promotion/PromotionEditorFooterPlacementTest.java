package com.harmoni.menu.dashboard.layout.menu.promotion;

import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards where the promotion editor's action buttons sit.
 *
 * <p>Save, Update, Apply Status and Cancel belong to the form, so they belong at the foot
 * of the form column. They used to be added to the editor itself, which made them a
 * sibling of both columns and put them in a strip running under the preview as well.
 * Nothing failed: the buttons were visible and worked either way. It was only that the
 * controls sat outside the panel they apply to.</p>
 *
 * <p>The layout is built by calling the attach-time methods reflectively, because there
 * is no session here for {@code onAttach} to fire.</p>
 */
class PromotionEditorFooterPlacementTest {

    private static PromotionFormWithPreview editor() {
        PromotionFormWithPreview editor = new PromotionFormWithPreview(
                null, null, null, PromotionEditorContext.builder().formAction(FormAction.CREATE).build(), null);
        invoke(editor, "buildLayout");
        invoke(editor, "addFooterButtons");
        return editor;
    }

    private static void invoke(PromotionFormWithPreview editor, String method) {
        try {
            Method target = PromotionFormWithPreview.class.getDeclaredMethod(method);
            target.setAccessible(true);
            target.invoke(editor);
        } catch (InvocationTargetException e) {
            throw new AssertionError(method + " failed", e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError(method + " is missing or not callable", e);
        }
    }

    @Test
    void theFooterIsInsideTheFormColumn() {
        VerticalLayout column = formColumn(editor());

        assertNotNull(column, "the form column is what the buttons are meant to sit in");
        assertTrue(containsClass(column, "promotion-editor-footer"),
                "the action footer has to be in the form column, not beside it");
    }

    @Test
    void theFooterIsNotASiblingOfTheTwoColumns() {
        PromotionFormWithPreview editor = editor();

        boolean atTopLevel = editor.getChildren().toList().stream()
                .anyMatch(child -> child.getClassNames().contains("promotion-editor-footer"));

        assertFalse(atTopLevel,
                "a footer on the editor itself spans the full width under the preview as "
                        + "well, which is the placement this replaced");
    }

    @Test
    void theFormItselfIsStillInThatColumn() {
        PromotionFormWithPreview editor = editor();
        VerticalLayout column = formColumn(editor);

        assertNotNull(column);
        assertTrue(contains(column, editor.getPromotionForm()),
                "moving the footer must not have displaced the form it sits under");
    }

    @Test
    void theFooterStillHoldsAllFourActionButtons() {
        VerticalLayout column = formColumn(editor());

        Component footer = column.getChildren().toList().stream()
                .filter(child -> child.getClassNames().contains("promotion-editor-footer"))
                .findFirst()
                .orElse(null);
        assertNotNull(footer, "expected a footer inside the form column");

        List<String> labels = footer.getChildren().toList().stream()
                .filter(Button.class::isInstance)
                .map(Button.class::cast)
                .map(Button::getText)
                .toList();

        assertTrue(labels.contains(Messages.get(Messages.Keys.ACTION_SAVE))
                        && labels.contains(Messages.get(Messages.Keys.ACTION_UPDATE))
                        && labels.contains(Messages.get(Messages.Keys.ACTION_CANCEL))
                        && labels.contains(Messages.get("action.applyStatus")),
                "moving the footer must not cost it any of its four buttons; found " + labels);
    }

    private static VerticalLayout formColumn(PromotionFormWithPreview editor) {
        for (Component child : editor.getChildren().toList()) {
            if (!child.getClassNames().contains("promotion-editor-layout")) {
                continue;
            }
            for (Component column : child.getChildren().toList()) {
                if (column.getClassNames().contains("promotion-form-wrapper")) {
                    return (VerticalLayout) column;
                }
            }
        }
        return null;
    }

    private static boolean containsClass(Component root, String className) {
        if (root.getClassNames().contains(className)) {
            return true;
        }
        return root.getChildren().toList().stream()
                .anyMatch(child -> containsClass(child, className));
    }

    private static boolean contains(Component root, Component wanted) {
        if (root == wanted) {
            return true;
        }
        return root.getChildren().anyMatch(child -> contains(child, wanted));
    }
}
