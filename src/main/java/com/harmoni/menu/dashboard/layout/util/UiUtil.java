package com.harmoni.menu.dashboard.layout.util;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;

/**
 * Shared factories for the icon/primary buttons used consistently across the
 * list views (edit, delete, add new).
 */
public final class UiUtil {

    public static final String NO_RECORDS = "No records found";

    private UiUtil() {
        throw new IllegalStateException("Utility class");
    }

    public static Button editButton(ComponentEventListener<ClickEvent<Button>> listener) {
        return editButton("Edit", listener);
    }

    public static Button editButton(String tooltip,
                                    ComponentEventListener<ClickEvent<Button>> listener) {
        Button button = new Button(new Icon(VaadinIcon.EDIT));
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        button.setTooltipText(tooltip);
        button.addClickListener(listener);
        return button;
    }

    public static Button deleteButton(ComponentEventListener<ClickEvent<Button>> listener) {
        Button button = new Button(new Icon(VaadinIcon.TRASH));
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE,
                ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_ERROR);
        button.setTooltipText("Delete");
        button.addClickListener(listener);
        return button;
    }

    public static Button addButton(String label, ComponentEventListener<ClickEvent<Button>> listener) {
        Button button = new Button(label, new Icon(VaadinIcon.PLUS));
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button.addClickListener(listener);
        return button;
    }
}