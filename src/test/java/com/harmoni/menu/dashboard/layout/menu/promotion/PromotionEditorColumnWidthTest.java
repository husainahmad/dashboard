package com.harmoni.menu.dashboard.layout.menu.promotion;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.dom.Style;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards that both editor columns are described by one stylesheet, and only one.
 *
 * <p>They used to be described in both places, and the Java always won. An inline
 * style outranks every stylesheet rule regardless of specificity, so
 * {@code .promotion-form-wrapper} in forms.css could say {@code min-width: 0} while the
 * component said {@code min-width: 560px} and be silently overridden - and every rule in
 * the {@code max-width: 900px} media query that relied on a width being unset was dead,
 * so neither column ever collapsed to a single column on a narrow screen.</p>
 */
class PromotionEditorColumnWidthTest {

    /**
     * The properties that decide how wide a column is. None may be set on the
     * component, or the stylesheet cannot describe it.
     */
    private static final List<String> SIZING_PROPERTIES = List.of(
            "width", "min-width", "max-width", "flex", "flex-grow", "flex-basis", "flex-shrink");

    private static final String FORM_COLUMN = ".promotion-form-wrapper";
    private static final String PREVIEW_COLUMN = ".promotion-preview-wrapper";

    @Test
    void theFormColumn_carriesNoInlineSizing() throws Exception {
        assertNoInlineSizing(formColumn(), FORM_COLUMN);
    }

    @Test
    void thePreviewColumn_carriesNoInlineSizing() throws Exception {
        assertNoInlineSizing(previewColumn(), PREVIEW_COLUMN);
    }

    @Test
    void bothColumns_areStyledByTheStylesheetRule() throws Exception {
        assertTrue(formColumn().getClassNames().contains("promotion-form-wrapper"),
                "with no inline sizing the form column is only as wide as forms.css says, so it has to carry "
                        + "the class the rule is written against");
        assertTrue(previewColumn().getClassNames().contains("promotion-preview-wrapper"),
                "with no inline sizing the preview column is only as wide as forms.css says, so it has to carry "
                        + "the class the rule is written against");
    }

    @Test
    void theStylesheet_describesTheFormColumnWidth() throws Exception {
        String rule = ruleFor(FORM_COLUMN);

        assertTrue(rule.contains("flex: 1 1 0"),
                "the form column takes the remaining space, which flex-basis is what expresses - a width is "
                        + "never read on a flex item");
        assertTrue(rule.contains("min-width: 560px"),
                "the form stops being readable below 560px, and that floor is what the rule has to state");
        assertTrue(rule.contains("max-width: 1000px"),
                "the form column is capped so a wide monitor does not stretch the fields to unusable line "
                        + "lengths");
        assertTrue(rule.contains("overflow-x: hidden"),
                "the section cards clip their own overflow, so the column does the same");
    }

    @Test
    void theStylesheet_doesNotContradictTheFormColumnWidth() throws Exception {
        assertTrue(!ruleFor(FORM_COLUMN).contains("min-width: 0"),
                "min-width: 0 only grants the flex algorithm permission to crush the form to nothing, which is "
                        + "the opposite of the floor this column needs");
    }

    @Test
    void theStylesheet_describesThePreviewColumnWidth() throws Exception {
        String rule = ruleFor(PREVIEW_COLUMN);

        assertTrue(rule.contains("width: 420px"),
                "the preview column is a fixed band, and the stylesheet is the only thing that can say so now "
                        + "that the component no longer does");
        assertTrue(rule.contains("min-width: 380px"),
                "the band has a floor below which the label/value rows stop fitting");
        assertTrue(rule.contains("max-width: 480px"),
                "the band has a ceiling so the preview does not drift away from the form on a wide monitor");
        assertTrue(rule.contains("flex: 0 0 auto"),
                "the column is pinned rather than flexible, so it must not grow or shrink with the form; "
                        + "flex-basis auto is what lets the width above set the size");
    }

    @Test
    void theSingleColumnFallback_isReachableForBothColumns() throws Exception {
        String mediaQuery = mediaQueryFor(900);

        String formFallback = blockFor(FORM_COLUMN, mediaQuery);
        assertTrue(formFallback != null && formFallback.contains("max-width: none"),
                "the form column has to be allowed to exceed its 1000px cap once the editor stacks, and the "
                        + "stylesheet can only say that while the column's own max-width is not inline");

        String previewFallback = blockFor(PREVIEW_COLUMN, mediaQuery);
        assertTrue(previewFallback != null
                        && previewFallback.contains("width: 100%")
                        && previewFallback.contains("min-width: 0")
                        && previewFallback.contains("max-width: none"),
                "the preview column has to reach full width on a narrow screen, and it cannot while the "
                        + "component sets its own width - all three of these rules were dead before");
    }

    private static void assertNoInlineSizing(Component column, String selector) throws Exception {
        Style style = column.getElement().getStyle();

        for (String property : SIZING_PROPERTIES) {
            // Style.get returns null, not "", for a property that was never set.
            String declared = style.get(property);
            assertTrue(declared == null || declared.isEmpty(),
                    selector + " sets an inline '" + property + "' (" + declared + "), which outranks forms.css "
                            + "however specific the rule is - the column's width belongs in the stylesheet, where "
                            + "the media query can still reach it");
        }
    }

    /** The editor's first column, i.e. the form wrapper. */
    private static Component formColumn() throws Exception {
        return column(0);
    }

    /** The editor's second column, i.e. the preview wrapper. */
    private static Component previewColumn() throws Exception {
        return column(1);
    }

    private static Component column(int index) throws Exception {
        PromotionFormWithPreview editor = new PromotionFormWithPreview(
                null, null, null, PromotionEditorContext.builder().formAction(FormAction.CREATE).build(), null);
        invoke(editor, "buildLayout");

        Component mainLayout = editor.getChildren().findFirst()
                .orElseThrow(() -> new AssertionError("the editor built no main layout"));
        return mainLayout.getChildren().skip(index).findFirst()
                .orElseThrow(() -> new AssertionError("the main layout holds no column at index " + index));
    }

    private static void invoke(PromotionFormWithPreview editor, String name) throws Exception {
        Method method = PromotionFormWithPreview.class.getDeclaredMethod(name);
        method.setAccessible(true);
        method.invoke(editor);
    }

    /**
     * Reads one rule out of the promotion stylesheet, with comments dropped and
     * whitespace collapsed, so the declarations can be matched without depending on how
     * the file happens to wrap them.
     */
    private static String ruleFor(String selector) throws IOException {
        String block = blockFor(selector, normalisedCss());
        if (block == null) {
            throw new AssertionError("forms.css no longer has a rule for " + selector
                    + " - if it moved, the component still relies on the class name, so update this test too");
        }
        return block;
    }

    /**
     * The body of one media query, by its max-width in pixels. Braces are matched rather
     * than sliced to the next one, so the dark-mode block that follows it in the file
     * cannot be mistaken for part of the query.
     */
    private static String mediaQueryFor(int maxWidth) throws IOException {
        String css = normalisedCss();
        Matcher start = Pattern
                .compile("@media\\s*\\(max-width:\\s*" + maxWidth + "px\\)\\s*\\{")
                .matcher(css);
        if (!start.find()) {
            throw new AssertionError("forms.css no longer has a " + maxWidth + "px media query");
        }

        int depth = 1;
        int cursor = start.end();
        while (cursor < css.length() && depth > 0) {
            char character = css.charAt(cursor++);
            if (character == '{') {
                depth++;
            } else if (character == '}') {
                depth--;
            }
        }
        return css.substring(start.end(), cursor - 1);
    }

    private static String normalisedCss() throws IOException {
        return Files.readString(Path.of("frontend", "themes", "dashboard", "forms.css"))
                .replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("\\s+", " ");
    }

    private static String blockFor(String selector, String css) {
        Matcher matcher = Pattern.compile(Pattern.quote(selector) + "\\s*\\{([^}]*)\\}").matcher(css);
        return matcher.find() ? matcher.group(1) : null;
    }
}
