package com.harmoni.menu.dashboard.layout.component;

import com.harmoni.menu.dashboard.layout.util.Css;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards what collapsing a page's chrome is allowed to touch.
 *
 * <p>The class toggle is the entire mechanism, and that is deliberate: the alternative -
 * moving the editor into a full screen overlay and back - detaches and re-attaches it,
 * and every editor here builds itself in {@code onAttach}, so the operator would return
 * from "full screen" to a second copy of the form. These assertions are the cheap half
 * of that argument: the component tree cannot change, only the class on its root.</p>
 */
class FocusModeToggleTest {

    @Test
    void itStartsWithItsTargetUntouched() {
        Div target = new Div();
        FocusModeToggle toggle = new FocusModeToggle(target);

        assertFalse(toggle.isFocusMode());
        assertFalse(target.getClassNames().contains(Css.FOCUS_MODE),
                "nothing is collapsed until it is asked for, and the page is built and shown with its "
                        + "header and toolbar in place");
    }

    @Test
    void clickingCollapsesTheTarget() {
        Div target = new Div();
        FocusModeToggle toggle = new FocusModeToggle(target);

        toggle.click();

        assertTrue(toggle.isFocusMode());
        assertTrue(target.getClassNames().contains(Css.FOCUS_MODE),
                "the class is the whole mechanism: list-view.css keys the hiding of the header and the "
                        + "toolbar off it, so without it the button would look pressed and change nothing");
    }

    @Test
    void clickingAgainRestoresTheTarget() {
        Div target = new Div();
        FocusModeToggle toggle = new FocusModeToggle(target);

        toggle.click();
        toggle.click();

        assertFalse(toggle.isFocusMode());
        assertFalse(target.getClassNames().contains(Css.FOCUS_MODE),
                "the header and toolbar are still there underneath, and a class left on the page would hide "
                        + "them for the rest of the session with no way back");
    }

    @Test
    void collapsingMovesNothing() {
        Component header = new Div();
        Component toolbar = new Div();
        Div target = new Div(header, toolbar);
        FocusModeToggle toggle = new FocusModeToggle(target);
        Component headerParentBefore = header.getParent().orElseThrow();
        Component toolbarParentBefore = toolbar.getParent().orElseThrow();

        toggle.click();

        assertTrue(header.getParent().isPresent(),
                "a collapsed page still has to be in the tree, or the tab it was opened on is showing nothing");
        assertSame(headerParentBefore, header.getParent().orElseThrow(),
                "the header has to keep the parent it had. Re-parenting the editor - into an overlay, say - "
                        + "is what re-runs onAttach, and onAttach is where this form renders itself");
        assertSame(toolbarParentBefore, toolbar.getParent().orElseThrow(),
                "the same goes for the toolbar: nothing is detached, it is only hidden, so its filter state "
                        + "is intact when it comes back");
    }

    @Test
    void theClassLandsOnTheTargetRatherThanTheButton() {
        Div target = new Div();
        FocusModeToggle toggle = new FocusModeToggle(target);

        toggle.click();

        assertFalse(toggle.getClassNames().contains(Css.FOCUS_MODE),
                "the state class describes the thing being collapsed; on the button it would describe the "
                        + "control instead, and the page's chrome would stay put");
    }

    @Test
    void itIsCarriedByTheClassTheStylesheetPins() {
        FocusModeToggle toggle = new FocusModeToggle(new Div());

        assertTrue(toggle.getClassNames().contains(Css.FOCUS_TOGGLE),
                "list-view.css positions the control in the top right of the shell through this class; "
                        + "without it the button joins the page's flow and pushes the tab sheet down");
    }

    @Test
    void anIconOnlyControl_isNamedForBothStates() {
        FocusModeToggle toggle = new FocusModeToggle(new Div());
        String collapsed = ariaLabel(toggle);

        toggle.click();
        String expanded = ariaLabel(toggle);
        toggle.click();

        assertFalse(collapsed.isBlank(),
                "an icon-only button has no text to read out, so the aria label is its accessible name");
        assertFalse(collapsed.equals(expanded),
                "the two icons differ by a few pixels, so a name that did not change with the state would "
                        + "announce the same action whichever way round the page was");
        assertFalse(ariaLabel(toggle).isBlank(),
                "bringing the chrome back has to be as reachable by name as taking it away was");
    }

    private static String ariaLabel(FocusModeToggle toggle) {
        return toggle.getAriaLabel().orElse("");
    }
}
