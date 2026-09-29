package com.harmoni.menu.dashboard.layout.menu;

import com.harmoni.menu.dashboard.layout.component.DialogClosing;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import lombok.Getter;
import com.harmoni.menu.dashboard.layout.util.Css;

/**
 * Base {@link FormLayout} for the product editors.
 *
 * <p>
 * Applies the shared {@code product-form} styling, wraps a component into the
 * standard content / toolbar rows used by the editors and marshals success and
 * error feedback onto the UI thread. The owning {@link UI} is captured on attach.
 * </p>
 */
@Getter
public class ProductFormLayout extends FormLayout  {

    private UI ui;

    public ProductFormLayout() {
        addClassName(Css.PRODUCT_FORM);
        this.getElement().addEventListener("keydown", domEvent -> {}).stopPropagation();
    }

    /**
     * Wraps {@code component} in a full-size {@code content} row that stretches the component.
     *
     * @param component the component to lay out, given flex grow
     * @return the content row holding the component
     */
    public HorizontalLayout getContent(Component component) {
        HorizontalLayout content = new HorizontalLayout(component);
        // Width only. This used to be setSizeFull() plus flex-grow 1, and both worked
        // against the row growing to fit the grid: setSizeFull pinned the wrapper to
        // 100% of the form, and flex-grow 1 made the grid stretch to fill whatever
        // height that left. With the grid's max-height removed so it sizes to its rows,
        // a stretched grid scrolls internally again - the second scroller the removal
        // was meant to get rid of. So the row is full width and the grid is left to
        // take its natural height.
        content.setWidthFull();
        content.addClassNames("content");
        return content;
    }

    /**
     * Wraps {@code component} in a baseline-aligned {@code toolbar} row.
     *
     * @param component the component to place in the toolbar
     * @return the toolbar row holding the component
     */
    public HorizontalLayout getToolbar(Component component) {
        HorizontalLayout toolbar = new HorizontalLayout(component);
        toolbar.addClassName(Css.TOOLBAR);
        toolbar.setAlignItems(FlexComponent.Alignment.BASELINE);
        return toolbar;
    }

    /**
     * Shows a success notification on the UI thread.
     *
     * @param text the message to display
     */
    public void showNotification(String text) {
        UiUtil.safeAccess(ui, () -> UiUtil.success(text));
    }

    /**
     * Opens an error {@link DialogClosing} dialog on the UI thread.
     *
     * @param message the error text to show
     */
    public void showErrorDialog(String message) {
        DialogClosing dialog = new DialogClosing(message);
        UiUtil.safeAccess(ui, ()-> {
            add(dialog);
            dialog.open();
        });
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
    }
}
