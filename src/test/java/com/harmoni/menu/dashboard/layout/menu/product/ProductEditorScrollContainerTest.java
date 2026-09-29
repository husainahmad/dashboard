package com.harmoni.menu.dashboard.layout.menu.product;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards that the product editor's action bar stays reachable at the bottom of a long form.
 *
 * <p>The bar is a {@code position: sticky; bottom: 0} child of the form, and a sticky
 * element only pins inside a scrolling <em>ancestor</em> while being constrained by its own
 * containing block. Those two came apart in the two-column editor: the form there is
 * {@code position: relative}, which makes the form itself the containing block, so the bar
 * pinned to the form and scrolled away with the content. Save and Update were then below the
 * bottom of the form with no way to reach them.</p>
 *
 * <p>The fix moves the scroll up one level to the column, so the form is ordinary content
 * that grows past it and the bar has a real bottom edge to stick against. The same reasoning
 * applies to the promotion editor, whose column used to be capped by a {@code calc(100vh -
 * 200px)} guess rather than an actual height, so both are asserted here.</p>
 */
class ProductEditorScrollContainerTest {

    private static final String PROMOTION_COLUMN = ".promotion-form-wrapper";
    private static final String PRODUCT_COLUMN = ".product-form-wrapper";
    private static final String PRODUCT_FORM = ".product-editor-layout .product-form";

    @Test
    void theProductColumn_isTheScrollContainer() throws Exception {
        String column = blockFor(PRODUCT_COLUMN);

        assertTrue(column.contains("overflow-y: auto"),
                "the column has to be the scroll container, not the form. A sticky action bar only pins "
                        + "against a scrolling ancestor outside its own containing block, and the form is "
                        + "position: relative in the editor, so it would otherwise be its own containing "
                        + "block and the bar would scroll out of reach with the content");
        assertTrue(column.contains("overscroll-behavior: contain"),
                "the column stops the scroll chaining to whatever is behind it, so reaching the end of the "
                        + "form does not drag the page underneath along with it");
    }

    @Test
    void theProductForm_canGrowPastTheColumn() throws Exception {
        String form = blockFor(PRODUCT_FORM);

        assertTrue(form.contains("min-height: 100%"),
                "the form needs a floor so a short one still fills the column rather than floating at the "
                        + "top of a tall window");
        assertTrue(Pattern.compile("\\bheight:\\s*auto").matcher(form).find(),
                "the form's height has to be auto so it can exceed the column; a form capped to the column's "
                        + "height has no overflow to scroll, which leaves the action bar nowhere to travel "
                        + "and the bottom of a long form unreachable");
        assertTrue(form.contains("width: 100%"),
                "the form's inline size was removed, so the stylesheet has to state the width too. Without "
                        + "it, flex: 0 0 auto would size the form to its content instead of filling the column");
    }

    @Test
    void theProductForm_isNotItsOwnScrollContainer() throws Exception {
        String form = blockFor(PRODUCT_FORM);

        assertTrue(form.contains("overflow: visible"),
                "the form's own overflow: auto from product-form.css has to be released here. Both it and the "
                        + "column scrolling at once is what left the action bar stranded between the two");
    }

    @Test
    void theProductActionBar_endsTheFormRatherThanPinningTheWindow() throws Exception {
        String bar = blockFor(".product-editor-layout .product-form .form-actions");

        assertTrue(bar.contains("position: static"),
                "the bar is position: sticky standalone, which pins it to the scrollport and so holds the "
                        + "bottom edge of the window for the whole scroll. Inside the two-column editor the "
                        + "column is what scrolls, so a sticky bar never leaves the viewport edge and the "
                        + "operator scrolls a long form with Save/Update hanging there the whole way. The "
                        + "promotion editor's footer is a sibling of its form and carries no position, so it "
                        + "turns up at the bottom of the form instead; this releases the sticky to match");
    }

    @Test
    void thePromotionFooter_isPositionedLikeTheProductBarNow() throws Exception {
        String footer = blockFor(".promotion-editor-footer");

        assertFalse(footer.contains("position:"),
                "the promotion footer is a sibling of the form, added to the wrapper rather than inside the "
                        + "form, so it travels with the content and lands at the bottom of the form. Giving it "
                        + "a position would pin it to the scrollport and reintroduce the same two editors "
                        + "disagreeing about where the buttons belong");
    }

    @Test
    void thePromotionColumn_noLongerGuessesItsHeight() throws Exception {
        String css = normalisedCss();

        assertFalse(css.contains("calc(100vh - 200px"),
                "that constant stood in for a real height by assuming 200px of chrome above and below the "
                        + "form. It silently clipped the form, or let it run under the tab strip, whenever "
                        + "the toolbar or the window chrome did not match the guess");

        String column = blockFor(PROMOTION_COLUMN);
        assertTrue(column.contains("overflow-y: auto"),
                "the column is what scrolls, so the form can exceed it and the action footer can stick "
                        + "against its bottom edge");
    }

    private static String blockFor(String selector) throws IOException {
        String block = rawBlockFor(selector, normalisedCss());
        if (block == null) {
            throw new AssertionError("forms.css no longer has a rule for '" + selector
                    + "' - the components still rely on these class names, so update this test too");
        }
        return block;
    }

    /**
     * The first rule for a selector that is not nested inside another block.
     *
     * <p>Several of these selectors are also overridden in the 900px media query, and a
     * naive search returns that one instead: a single {@code \{([^}]*)\}} match finds
     * whichever block comes first textually once comments and whitespace are collapsed,
     * so the assertion would quietly read a narrow-screen override that says nothing
     * about the default layout. Counting braces to the left of the match is what
     * distinguishes the two, since the file nests nothing else.</p>
     */
    private static String rawBlockFor(String selector, String css) {
        Matcher matcher = Pattern.compile(Pattern.quote(selector) + "\\s*\\{").matcher(css);
        while (matcher.find()) {
            if (braceDepth(css, matcher.start()) > 0) {
                continue;
            }
            Matcher body = Pattern.compile("([^}]*)\\}").matcher(css);
            body.region(matcher.end(), css.length());
            return body.find() ? body.group(1) : null;
        }
        return null;
    }

    /**
     * How many blocks enclose this offset, found by matching braces rather than by
     * searching for the nearest {@code @media}: a query cannot open before the text
     * already passed, and a rule nested in one is an override rather than the default.
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
     * Reads forms.css with comments dropped and whitespace collapsed, so declarations can be
     * matched without depending on how the file happens to wrap them.
     */
    private static String normalisedCss() throws IOException {
        return Files.readString(Path.of("frontend", "themes", "dashboard", "forms.css"))
                .replaceAll("(?s)/\\*.*?\\*/", " ")
                .replaceAll("\\s+", " ");
    }
}
