package com.harmoni.menu.dashboard.layout.component;

import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * A floating control that takes a page's header and toolbar out of the way, and puts
 * them back.
 *
 * <p>It is a class toggle rather than a full screen overlay because the point is the
 * room, not the window: the tab sheet is the one flexible child of a page shell, so the
 * height the header and the toolbar were taking is simply handed to the editor below
 * them, which is already pinned to its panel and re-resolves against it. An editor
 * that is already open has a binder holding its dirty state, a grid of SKU rows and an
 * image uploaded but not yet saved; re-parenting it into an overlay and back would
 * detach and re-attach it, and every editor here builds itself in {@code onAttach}, so
 * the operator would come back from "full screen" to a second copy of the form stacked
 * on the first.</p>
 *
 * <p>The target has to be a positioned element and it must be the one carrying the
 * rule, so the shell is the target and the control is a sibling of the elements being
 * hidden rather than a child of one of them: hidden means unreachable, and a control
 * that hides itself leaves no way to undo it.</p>
 *
 * <p>Not a route and not a Spring bean: the page shell that owns the control is built
 * by the route itself.</p>
 */
public class FocusModeToggle extends Button {

    /** The component whose header and toolbar are hidden. */
    private final transient Component target;

    private final Icon enterIcon = new Icon(VaadinIcon.COMPRESS);

    private final Icon exitIcon = new Icon(VaadinIcon.EXPAND_FULL);

    /** Whether {@link Css#FOCUS_MODE} is currently on the target. */
    private boolean focusMode;

    /**
     * @param target the page shell to collapse, required; it must be the positioned
     *               ancestor the stylesheet pins the control against
     */
    public FocusModeToggle(Component target) {
        this.target = target;
        addClassName(Css.FOCUS_TOGGLE);
        // Icon and small only: no variant here claims the background, so Lumo fills the
        // control the same way it fills any other button and the stylesheet is left to
        // decide where it sits. Tertiary would declare the fill transparent behind an
        // !important.
        addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
        setIcon(enterIcon);
        describe(false);
        addClickListener(event -> toggle());
    }

    /**
     * Hides the page's header and toolbar, or brings them back.
     *
     * <p>Both states are the same call, so the button cannot end up offering to collapse
     * a page that is already collapsed: the icon and the name are re-read from the flag
     * this flips.</p>
     */
    public void toggle() {
        focusMode = !focusMode;
        if (focusMode) {
            target.addClassName(Css.FOCUS_MODE);
            setIcon(exitIcon);
        } else {
            target.removeClassName(Css.FOCUS_MODE);
            setIcon(enterIcon);
        }
        describe(focusMode);
    }

    /**
     * @return whether the header and toolbar are currently hidden
     */
    public boolean isFocusMode() {
        return focusMode;
    }

    /**
     * Names the control after what it will do next, since the icon alone leaves a screen
     * reader with two indistinguishable glyphs to learn. Both the tooltip and the aria
     * label are set: the tooltip is what a pointer user reads on hover, and the label is
     * what there is to read when there is no pointer.
     *
     * @param active whether the page is collapsed, i.e. what the click will undo
     */
    private void describe(boolean active) {
        String label = Messages.get(active ? "action.exitFocusMode" : "action.enterFocusMode");
        setTooltipText(label);
        setAriaLabel(label);
    }
}
