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
        addClassName("product-form");
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
        content.setFlexGrow(1, component);
        content.addClassNames("content");
        content.setSizeFull();
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
        toolbar.addClassName("toolbar");
        toolbar.setAlignItems(FlexComponent.Alignment.BASELINE);
        return toolbar;
    }

    /**
     * Shows a success notification on the UI thread.
     *
     * @param text the message to display
     */
    public void showNotification(String text) {
        ui.access(() -> UiUtil.success(text));
    }

    /**
     * Opens an error {@link DialogClosing} dialog on the UI thread.
     *
     * @param message the error text to show
     */
    public void showErrorDialog(String message) {
        DialogClosing dialog = new DialogClosing(message);
        ui.access(()-> {
            add(dialog);
            dialog.open();
        });
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
    }
}
