package com.harmoni.menu.dashboard.layout.util;

import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.Focusable;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Shortcuts;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Shared factories for the icon/primary buttons used consistently across the
 * list views (edit, delete, add new).
 */
public final class UiUtil {

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
        return editButton(Messages.get(Messages.Keys.ACTION_EDIT), listener);
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
        return buildIconButton(VaadinIcon.CHECK, Messages.get(Messages.Keys.ACTION_UPDATE), false);
    }

    /**
     * Creates an inline delete button using the default "Delete" tooltip.
     *
     * @param listener the click listener wired to the button
     * @return the configured button
     */
    public static Button deleteButton(ComponentEventListener<ClickEvent<Button>> listener) {
        Button button = deleteButton(Messages.get(Messages.Keys.ACTION_DELETE));
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
     * Error toast with a Retry button, for failed background loads where stale
     * or empty data stays on screen. Must be called on the UI thread (e.g.
     * inside {@link #safeAccess}).
     *
     * @param message text shown in the toast
     * @param retry   action run when the user clicks Retry
     */
    public static void errorWithRetry(String message, Runnable retry) {
        Notification notification = new Notification();
        notification.setPosition(Notification.Position.BOTTOM_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        notification.setDuration(8000);
        Span text = new Span(message);
        Button retryButton = new Button(Messages.get("action.retry"), event -> {
            notification.close();
            if (retry != null) {
                retry.run();
            }
        });
        retryButton.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout layout = new HorizontalLayout(text, retryButton);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        notification.add(layout);
        notification.open();
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

    /**
     * Shows a notification toast with the given variant and duration.
     *
     * @param message  text shown in the toast
     * @param variant  styling variant of the notification
     * @param duration how long in milliseconds the toast stays visible
     */
    private static void toast(String message, NotificationVariant variant, int duration) {
        Notification notification = new Notification(message, duration);
        notification.setPosition(Notification.Position.BOTTOM_CENTER);
        notification.addThemeVariants(variant);
        notification.getElement().getStyle()
                .set("--vaadin-notification-card-padding", "var(--lumo-space-s) var(--lumo-space-m)")
                .set("border-radius", "var(--lumo-border-radius-l)")
                .set(Css.FONT_WEIGHT, "500");
        notification.open();
    }

    /**
     * Registers the keyboard shortcuts shared by every list view: {@code /}
     * focuses the search filter and {@code n} triggers the "new" action. While
     * any of the fields tracked in {@code typingFields} is focused (they are
     * added here automatically plus whatever the caller tracks), the "new"
     * shortcut stays quiet so keys keep flowing to the field.
     *
     * @param host         the view owning the shortcuts
     * @param typingFields set of fields that currently have focus while the
     *                     user is typing; grows/shrinks automatically
     * @param filterText   the search field to focus with {@code /}
     * @param newAction    the "create new" action bound to {@code n}
     */
    public static void registerListShortcuts(Component host, Set<Component> typingFields,
                                             TextField filterText, Runnable newAction) {
        Shortcuts.addShortcutListener(host, filterText::focus, Key.SLASH);
        filterText.addFocusListener(event -> typingFields.add(filterText));
        filterText.addBlurListener(event -> typingFields.remove(filterText));
        Shortcuts.addShortcutListener(host, () -> {
            if (typingFields.isEmpty()) {
                newAction.run();
            }
        }, Key.KEY_N);
    }

    /**
     * Tracks a field for the {@code n} shortcut guard so Shortcuts registered
     * with {@link #registerListShortcuts} do not fire while it is focused.
     *
     * @param typingFields the shared set supplied to {@link #registerListShortcuts}
     * @param field        the input to track
     */
    public static void guardShortcutField(Set<Component> typingFields, Focusable<?> field) {
        field.addFocusListener(event -> typingFields.add((Component) field));
        field.addBlurListener(event -> typingFields.remove((Component) field));
    }

    private static final ScheduledExecutorService FLASH_SCHEDULER = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "ui-flash");
        thread.setDaemon(true);
        return thread;
    });

    /**
     * Adds a short-lived style class to a component, e.g. to flash a grid cell
     * green after a successful inline save or red after a failure. The class is
     * removed after {@code millis}, safely on the UI thread.
     *
     * @param component  the component to flash
     * @param styleClass the style class to add then remove
     * @param millis     how long the class stays applied
     */
    public static void flash(Component component, String styleClass, long millis) {
        if (component == null) {
            return;
        }
        component.addClassName(styleClass);
        component.getUI().ifPresent(ui -> FLASH_SCHEDULER.schedule(() -> {
            try {
                ui.access(() -> component.removeClassName(styleClass));
            } catch (RuntimeException ignored) {
                // UI was closed before the flash expired; nothing left to update.
            }
        }, millis, TimeUnit.MILLISECONDS));
    }

    /**
     * Dialog listing the keyboard shortcuts shared by the list views. Reusable
     * from any list toolbar (typically behind a question-mark button).
     *
     * @return a fresh, unopened dialog
     */
    public static Dialog shortcutsHelpDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(Messages.get(Messages.Keys.UI_KEYS_TITLE));
        dialog.setWidth("440px");
        VerticalLayout content = new VerticalLayout();
        content.setPadding(false);
        content.setSpacing(true);
        content.add(shortcutRow("/", Messages.get("ui.keys.focusSearch")));
        content.add(shortcutRow("n", Messages.get("ui.keys.createNew")));
        content.add(shortcutRow("Enter or Tab", Messages.get("ui.keys.savePrice")));
        dialog.add(content);
        dialog.getFooter().add(new Button(Messages.get(Messages.Keys.ACTION_CLOSE), event -> dialog.close()));
        return dialog;
    }

    private static Component shortcutRow(String key, String description) {
        Span kbd = new Span(key);
        kbd.addClassName("shortcut-kbd");
        Span text = new Span(description);
        text.addClassName("shortcut-desc");
        HorizontalLayout row = new HorizontalLayout(kbd, text);
        row.setSpacing(true);
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        return row;
    }

    private static final SimpleDateFormat SAVED_FORMAT = new SimpleDateFormat("HH:mm:ss");

    /**
     * Runs an action on the UI thread, tolerating a missing or already-detached
     * UI. Use this for any callback that can fire from a background thread
     * (REST responses, broadcasts) or after navigation, instead of calling
     * {@code ui.access()} directly.
     *
     * @param ui     the UI to lock, may be {@code null}
     * @param action the update to run on the UI thread
     */
    public static void safeAccess(UI ui, Runnable action) {
        if (ui == null || action == null || ui.getSession() == null) {
            return;
        }
        try {
            ui.access(action::run);
        } catch (RuntimeException e) {
            // UI was detached or closed before the action could run; nothing left to update.
        }
    }

    /**
     * Formats a timestamp for the small "Saved HH:mm:ss" caption shown under a
     * tier toggle checkbox, or {@code null} when there is nothing saved yet.
     *
     * @param savedAt the last successful save time
     * @return the caption text, or {@code null}
     */
    public static String tierSavedText(Date savedAt) {
        return savedAt == null ? null : Messages.get("grid.tier.saved", SAVED_FORMAT.format(savedAt));
    }
}