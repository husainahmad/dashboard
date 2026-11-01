package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.layout.component.PageHeader;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.vaadin.flow.component.Component;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards focus mode on the product page.
 *
 * <p>Two halves that only fail in different places. The component half checks that the
 * shell carries the control, that the control can reach the page it collapses, and that
 * the state class lands on the shell rather than on the button. The stylesheet half
 * checks that the rule acting on that class still hides the two elements it is supposed
 * to - and, more to the point, only those two: {@code .toolbar} is also the class on the
 * product form's SKU action bar and its Save/Update bar, so a selector that reached
 * further than the shell's own children would take the operator's price controls and
 * Save button out of a form still sitting on screen. None of that is visible from a Java
 * object, which is why the rule is read as text.</p>
 */
class ProductPageFocusModeTest {

    private static final String SHELL = ".page";
    private static final String STATE = ".page.focus-mode";
    private static final String TOGGLE = ".page .focus-toggle";

    @Test
    void theShell_carriesTheClassTheRuleIsWrittenAgainst() {
        assertTrue(shell().getClassNames().contains(Css.PAGE),
                "the shell is the positioned element the control's absolute box is measured against, and the "
                        + "element the focus-mode rule is scoped to");
    }

    @Test
    void theHeader_isNamedForTheRuleToActOn() {
        PageHeader header = PageHeader.of("page.product.title", "page.product.description");

        assertTrue(header.getClassNames().contains(Css.PAGE_HEADER),
                "one of the two things focus mode hides had no class at all until now, so the rule had "
                        + "nothing to match and the header would have stayed on screen");
    }

    @Test
    void theControl_isASiblingOfWhatItHides() {
        ProductTabs page = shell();
        Component toggle = page.getFocusModeToggle();
        Component header = childCarrying(page, Css.PAGE_HEADER);
        Component toolbar = childCarrying(page, Css.TOOLBAR);

        assertNotNull(toggle, "without a control there is no way to collapse the page, which is the feature");
        assertNotNull(header, "the shell no longer has the header the rule is written against");
        assertNotNull(toolbar, "the shell no longer has the toolbar the rule is written against");
        assertTrue(page.getChildren().anyMatch(child -> child == toggle),
                "the control is a direct child of the shell, so it keeps its place whether the header is on "
                        + "screen or not");
        assertFalse(contains(header, toggle),
                "nested inside the header the control would go out of sight at the same moment it hides the "
                        + "header, and the operator would be left with no way to bring it back");
        assertFalse(contains(toolbar, toggle),
                "same again for the toolbar, which is the larger of the two");
    }

    @Test
    void collapsingTheShell_marksTheShellItself() {
        ProductTabs page = shell();
        page.getFocusModeToggle().click();

        assertTrue(page.getClassNames().contains(Css.FOCUS_MODE),
                "the shell is what list-view.css keys the hiding off; the class on the button instead would "
                        + "leave the page exactly as it was");
    }

    @Test
    void theStylesheet_hidesTheHeaderAndTheToolbar() throws Exception {
        SelectorBlock rule = ruleFor(STATE);

        assertTrue(rule.selector().contains(Css.PAGE_HEADER),
                "the header is half the point: the operator reading a form wants the form, not the page title "
                        + "above it");
        assertTrue(rule.selector().contains(Css.TOOLBAR),
                "the toolbar is the other half. It is where the brand, category and tier filters and the "
                        + "search box live, none of which mean anything while a product is being edited");
        assertTrue(rule.body().contains("display: none"),
                "the elements have to leave the flow rather than be made transparent, or the tab sheet keeps "
                        + "the height it was already giving them and nothing is gained");
    }

    @Test
    void theStylesheet_hidesOnlyTheShellsOwnChildren() throws Exception {
        SelectorBlock rule = ruleFor(STATE);

        for (String hidden : List.of(Css.PAGE_HEADER, Css.TOOLBAR)) {
            assertTrue(rule.selector().contains("> ." + hidden),
                    "only the shell's own header and toolbar may go. " + hidden + " is also the class on the "
                            + "product form's own bars, and without the direct child combinator a collapsed "
                            + "page would take the SKU price controls and the Save button with it");
        }
    }

    @Test
    void theStylesheet_keepsTheControlInTheCornersEmptySpace() throws Exception {
        SelectorBlock rule = ruleFor(TOGGLE);

        assertTrue(rule.body().contains("position: absolute"),
                "in the flow the control would sit under the title and take a row of the page for itself in "
                        + "both states, which is the space the operator asked to get back");
        assertTrue(rule.body().contains("top: 0") && rule.body().contains("right: 0"),
                "one corner serves both states because the shell does not move when its header is hidden: the "
                        + "tab sheet grows upward into the space, so the control stays over the empty right "
                        + "half of the title, and over the empty end of the tab strip once that is gone");
    }

    // ------------------------------------------------------------- helpers

    /**
     * The product page shell, built without a UI.
     *
     * <p>{@code renderTabSheet} is run by hand because {@code onAttach} never fires with
     * no session, the same limitation {@code ProductFormWithPreviewTest} works around.</p>
     */
    private static ProductTabs shell() {
        ProductTabs page = new ProductTabs(null, null, null);
        page.addClassName(Css.PAGE);
        renderTabSheet(page);
        return page;
    }

    private static void renderTabSheet(ProductTabs page) {
        try {
            java.lang.reflect.Method render = ProductTabs.class.getDeclaredMethod("renderTabSheet");
            render.setAccessible(true);
            render.invoke(page);
        } catch (java.lang.reflect.InvocationTargetException e) {
            throw new AssertionError("renderTabSheet failed", e.getCause());
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("renderTabSheet is not reachable", e);
        }
    }

    private static Component childCarrying(Component parent, String className) {
        return parent.getChildren()
                .filter(child -> child.getClassNames().contains(className))
                .findFirst()
                .orElse(null);
    }

    private static boolean contains(Component root, Component wanted) {
        if (root == wanted) {
            return true;
        }
        return root.getChildren().anyMatch(child -> contains(child, wanted));
    }

    private record SelectorBlock(String selector, String body) {
    }

    private static SelectorBlock ruleFor(String marker) throws IOException {
        String css = normalisedCss();
        Matcher matcher = Pattern.compile("([^{}]+)\\{").matcher(css);
        while (matcher.find()) {
            String selector = matcher.group(1).trim();
            if (!selector.contains(marker) || braceDepth(css, matcher.start()) > 0) {
                continue;
            }
            Matcher body = Pattern.compile("([^}]*)\\}").matcher(css);
            body.region(matcher.end(), css.length());
            return new SelectorBlock(selector, body.find() ? body.group(1) : "");
        }
        throw new AssertionError("list-view.css no longer has a rule for '" + marker
                + "' - the component still relies on this class, so update this test too");
    }

    /**
     * How many blocks enclose this offset, so a rule nested in a media query is not
     * mistaken for the default one.
     */
    private static int braceDepth(String css, int offset) {
        int depth = 0;
        for (int i = 0; i < offset; i++) {
            char character = css.charAt(i);
            if (character == '{') {
                depth++;
            } else if (character == '}') {
                depth--;
            }
        }
        return depth;
    }

    /**
     * Reads list-view.css with comments dropped and whitespace collapsed, so declarations
     * can be matched without depending on how the file happens to wrap them.
     */
    private static String normalisedCss() throws IOException {
        return Files.readString(Path.of("frontend", "themes", "dashboard", "list-view.css"))
                .replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("\\s+", " ");
    }
}
