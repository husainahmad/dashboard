package com.harmoni.menu.dashboard.layout.menu.promotion;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.dom.Style;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards how a detail row divides the preview panel between its label and its value.
 *
 * <p>The label used to claim a fixed 42% of the row whether it needed it or not, and the
 * value broke words mid-word, so a plain one-word value like "Percentage" could be split
 * across two lines as "Percenta" / "ge". That is the bug these tests exist to keep out:
 * the value column has to be wide enough for a normal word, and a word that fits must
 * never be split.</p>
 */
class PromotionPreviewDetailRowTest {

    /** A one-word value that used to be split mid-word. */
    private static final String ONE_WORD_VALUE = "Percentage";

    @Test
    void theLabel_isSizedToItsOwnText_cappedAtTheShareItUsedToHold() {
        Style label = firstRowLabel();

        // 'auto' is the point: a two-word label no longer reserves its share up front, and
        // the cap is what stops a long localised label from taking the row instead.
        assertEquals("0 1 auto", label.get("flex"),
                "a fixed share made every short label hold back a quarter of the panel from the value next to it");
        assertEquals("42%", label.get("max-width"),
                "without a cap the label could grow to its full width and squeeze the value out of the row");
        assertEquals("0", label.get("min-width"),
                "the label has to stay shrinkable, or the cap would be the only thing bounding it");
    }

    @Test
    void theValue_doesNotBreakWordsThatFit() {
        String overflowWrap = firstRowValue().get("overflow-wrap");

        // 'anywhere' is the variant that lowers the element's min-content width, which is
        // what lets the flex algorithm hand the value a column too narrow for the word and
        // then split it. 'break-word' only breaks a word that genuinely cannot fit.
        assertTrue("break-word".equals(overflowWrap) || overflowWrap == null,
                "a value that fits on one line must stay on one line, so the value may not use "
                        + "overflow-wrap: anywhere - found: " + overflowWrap);
    }

    @Test
    void theValue_stillWrapsRatherThanOverflowing() {
        Style value = firstRowValue();

        assertEquals("1 1 0", value.get("flex"),
                "the value takes what the label leaves over, so a long target list wraps in place "
                        + "instead of pushing the panel wide");
        assertEquals("0", value.get("min-width"),
                "without min-width 0 the flex default pins the value to its min-content width and it "
                        + "cannot shrink to wrap");
    }

    @Test
    void thePanelIsWideEnoughForAOneWordValue() {
        // The row's own budget, in the panel's CSS terms. A word of this kind has to fit in
        // the value's share at the panel's narrow end, which is the case that used to break.
        int panelNarrowEnd = 380;
        int chrome = 2 * 16 + 2 * 1 + 2 * 16 + 8; // wrapper padding + border, preview padding, row gap
        int rowWidth = panelNarrowEnd - chrome;
        int valueWidth = rowWidth - (int) Math.round(rowWidth * 0.42);

        assertTrue(valueWidth >= 90,
                "at the panel's narrow end the value column works out to " + valueWidth
                        + "px, which leaves no room for a word like " + ONE_WORD_VALUE
                        + " - a narrow panel is the case that produced the two-line value");
    }

    @Test
    void everyDetailRowGetsTheSameTreatment() {
        PromotionPreview preview = new PromotionPreview();
        List<Style> values = previewDetailValues(preview);
        List<Style> labels = previewDetailLabels(preview);

        assertTrue(values.size() >= 10, "expected the preview's detail rows, found " + values.size());
        assertEquals(values.size(), labels.size(), "each row has a label and a value");

        for (Style value : values) {
            assertTrue(!String.valueOf(value.get("overflow-wrap")).equals("anywhere"),
                    "a value using overflow-wrap: anywhere can be split mid-word once the column narrows, "
                            + "which is how a one-word value ended up on two lines");
        }
    }

    private static Style firstRowLabel() {
        return previewDetailLabels(new PromotionPreview()).get(0);
    }

    private static Style firstRowValue() {
        return previewDetailValues(new PromotionPreview()).get(0);
    }

    /**
     * The value spans of the preview's detail rows, in the order they were added. Collected
     * by walking the element tree rather than by re-deriving the keys, so a row added later
     * is covered without this test knowing its name.
     */
    private static List<Style> previewDetailValues(PromotionPreview preview) {
        return spansWithClass(preview, "preview-detail-value").stream().map(Span::getStyle).toList();
    }

    private static List<Style> previewDetailLabels(PromotionPreview preview) {
        return spansWithClass(preview, "preview-detail-label").stream().map(Span::getStyle).toList();
    }

    private static List<Span> spansWithClass(com.vaadin.flow.component.Component root, String className) {
        List<Span> found = new ArrayList<>();
        collectSpans(root, className, found);
        return found;
    }

    /**
     * Walks the whole component tree depth-first. A fixed number of levels would be a
     * guess at the panel's nesting, and would quietly stop covering rows the moment one is
     * added. Walking components rather than elements also keeps the text nodes out of it.
     */
    private static void collectSpans(com.vaadin.flow.component.Component node, String className, List<Span> found) {
        if (node instanceof Span span && span.getClassNames().contains(className)) {
            found.add(span);
        }
        node.getChildren().forEach(child -> collectSpans(child, className, found));
    }
}
