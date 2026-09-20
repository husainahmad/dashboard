package com.harmoni.menu.dashboard.layout.util;

import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

/**
 * Shared factories for the icon/primary buttons used consistently across the
 * list views (edit, delete, add new).
 */
public final class UiUtil {

    /** Default message shown in lists/grids when there is nothing to display. */
    public static final String NO_RECORDS = "No records found";

    private UiUtil() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Creates an inline edit button using the default "Edit" tooltip.
     *
     * @param listener the click listener wired to the button
     * @return the configured button
     */
    public static Button editButton(ComponentEventListener<ClickEvent<Button>> listener) {
        return editButton("Edit", listener);
    }

    /**
     * Creates an inline edit button with a custom tooltip.
     *
     * @param tooltip  tooltip shown on hover
     * @param listener the click listener wired to the button
     * @return the configured button
     */
    public static Button editButton(String tooltip,
                                    ComponentEventListener<ClickEvent<Button>> listener) {
        Button button = editButton(tooltip);
        button.addClickListener(listener);
        return button;
    }

    /**
     * Creates an inline edit button with a custom tooltip and no listener.
     *
     * @param tooltip tooltip shown on hover
     * @return the configured button
     */
    public static Button editButton(String tooltip) {
        return buildIconButton(VaadinIcon.EDIT, tooltip, false);
    }

    /**
     * Creates an inline update button using the default "Update" tooltip.
     *
     * @return the configured button
     */
    public static Button updateButton() {
        return buildIconButton(VaadinIcon.CHECK, "Update", false);
    }

    /**
     * Creates an inline delete button using the default "Delete" tooltip.
     *
     * @param listener the click listener wired to the button
     * @return the configured button
     */
    public static Button deleteButton(ComponentEventListener<ClickEvent<Button>> listener) {
        Button button = deleteButton("Delete");
        button.addClickListener(listener);
        return button;
    }

    /**
     * Creates an inline delete (error-styled) button with a custom tooltip.
     *
     * @param tooltip tooltip shown on hover
     * @return the configured button
     */
    public static Button deleteButton(String tooltip) {
        return buildIconButton(VaadinIcon.TRASH, tooltip, true);
    }

    private static Button buildIconButton(VaadinIcon vaadinIcon, String tooltip, boolean error) {
        Button button = new Button(new Icon(vaadinIcon));
        button.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        if (error) {
            button.addThemeVariants(ButtonVariant.LUMO_ERROR);
        }
        button.setTooltipText(tooltip);
        return button;
    }

    /**
     * Creates a primary "add" button with the given label.
     *
     * @param label    button caption
     * @param listener the click listener wired to the button
     * @return the configured button
     */
    public static Button addButton(String label, ComponentEventListener<ClickEvent<Button>> listener) {
        Button button = new Button(label, new Icon(VaadinIcon.PLUS));
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        button.addClickListener(listener);
        return button;
    }

    /**
     * Formats a money amount as Indonesian rupiah, e.g. {@code Rp5.000}, or
     * {@code -} when the input is {@code null}.
     */
    public static String rupiah(Number value) {
        if (value == null) {
            return "-";
        }
        DecimalFormat formatter = new DecimalFormat("#,##0",
                DecimalFormatSymbols.getInstance(new Locale("id", "ID")));
        return "Rp" + formatter.format(value.doubleValue());
    }

    /**
     * Toast shown at the bottom of the screen for successful operations.
     */
    public static void success(String message) {
        toast(message, NotificationVariant.LUMO_SUCCESS, 3000);
    }

    /**
     * Toast shown at the bottom of the screen for error / validation feedback.
     */
    public static void error(String message) {
        toast(message, NotificationVariant.LUMO_ERROR, 6000);
    }

    /**
     * Shows a notification toast with the given variant and duration.
     *
     * @param message  text shown in the toast
     * @param variant  styling variant of the notification
     * @param duration how long in milliseconds the toast stays visible
     */
    public static void show(String message, NotificationVariant variant, int duration) {
        toast(message, variant, duration);
    }

    private static void toast(String message, NotificationVariant variant, int duration) {
        Notification notification = new Notification(message, duration);
        notification.setPosition(Notification.Position.BOTTOM_CENTER);
        notification.addThemeVariants(variant);
        notification.getElement().getStyle()
                .set("--vaadin-notification-card-padding", "var(--lumo-space-s) var(--lumo-space-m)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set("font-weight", "500");
        notification.open();
    }
}