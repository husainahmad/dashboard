package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.layout.menu.ProductFormLayout;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards that the product form has one scroll, not three.
 *
 * <p>The SKU and customization grids were capped at 332px and 240px so they would scroll
 * inside themselves. That put a second and a third scroller in one panel, and the inner
 * ones were under the operator's cursor: scrolling the form moved the page while the grid
 * stayed put, and rows past the cap were only reachable by finding the grid's own
 * scrollbar. The form already scrolls, so the caps bought nothing but a second scroll
 * target.</p>
 *
 * <p>Removing a {@code max-height} is not enough on its own, because two inline sizes were
 * holding the grid open against its content: {@code setSizeFull()} on the grid's row and
 * {@code flex-grow: 1} on the grid itself. An inline value outranks the stylesheet, so the
 * grid stretched to fill the row and scrolled internally again - the same second scroller,
 * by a different route. Both are asserted here.</p>
 */
class ProductFormGridScrollTest {

    private static final String FORM_STYLESHEET = "frontend/themes/dashboard/product-form.css";

    @Test
    void theSkuGrid_isNotCapped() throws Exception {
        assertNoHeightCap(".content vaadin-grid.sku-grid");
    }

    @Test
    void theCustomizationGrid_isNotCapped() throws Exception {
        assertNoHeightCap("vaadin-grid.customization-grid");
    }

    @Test
    void theGridRow_isNotStretchedByAnInlineHeight() throws Exception {
        var content = new ContentRow();
        var style = content.getElement().getStyle();

        for (String property : new String[]{"height", "min-height", "max-height"}) {
            String declared = style.get(property);
            assertTrue(declared == null || declared.isEmpty(),
                    "the grid's row sets an inline '" + property + "' (" + declared + "). It used to call "
                            + "setSizeFull(), which pinned the row to the form's height so the grid stretched "
                            + "to fill it and scrolled internally - the second scroller the cap removal was "
                            + "meant to get rid of, reintroduced through the inline value");
        }
    }

    @Test
    void theGrid_isNotStretchedByFlexGrow() {
        var grid = new ContentRow().getChild();

        String flexGrow = grid.getElement().getStyle().get("flex-grow");
        assertTrue(flexGrow == null || flexGrow.isEmpty(),
                "the grid is given flex-grow 1 (" + flexGrow + "), so it stretches to fill whatever height "
                        + "the row has instead of taking the height of its own rows. A stretched Vaadin grid "
                        + "scrolls internally, which is the scroll this form is meant not to have");
    }

    /** Asserts no rule for this selector declares a max-height. */
    private static void assertNoHeightCap(String selector) throws IOException {
        String css = normalisedCss();
        Matcher matcher = Pattern.compile(Pattern.quote(selector) + "\\s*\\{([^}]*)\\}").matcher(css);

        while (matcher.find()) {
            String body = matcher.group(1);
            assertFalse(body.contains("max-height"),
                    selector + " is capped again (" + body.strip() + "). Two scrollbars in one panel means the "
                            + "one under the cursor is not the one that moves");
        }
    }

    private static String normalisedCss() throws IOException {
        return Files.readString(Path.of(FORM_STYLESHEET))
                .replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("\\s+", " ");
    }

    /**
     * The row {@code ProductFormLayout.getContent} builds, so the assertions above read the
     * real component rather than a reimplementation of it.
     */
    private static final class ContentRow {
        private final com.vaadin.flow.component.orderedlayout.HorizontalLayout row =
                new ProductFormLayout().getContent(new com.vaadin.flow.component.grid.Grid<>());

        com.vaadin.flow.dom.Element getElement() {
            return row.getElement();
        }

        com.vaadin.flow.component.Component getChild() {
            return row.getChildren().findFirst()
                    .orElseThrow(() -> new AssertionError("the row holds no grid"));
        }
    }
}
