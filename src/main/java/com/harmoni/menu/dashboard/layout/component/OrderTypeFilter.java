package com.harmoni.menu.dashboard.layout.component;

import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * Segmented control for the dashboard's order-type scope: one "All" chip plus a
 * chip per channel (dine-in, takeaway, delivery).
 *
 * <p>Exactly one chip is active at a time; the active chip is styled as a
 * primary button and the rest as tertiary. Selecting a chip notifies the
 * listener with its key.</p>
 */
public class OrderTypeFilter extends HorizontalLayout {

    /** Order channels the dashboard can be scoped to. */
    public enum Scope {
        ALL("dashboard.scope.all"),
        DINE_IN("dashboard.scope.dineIn"),
        TAKEAWAY("dashboard.scope.takeaway"),
        DELIVERY("dashboard.scope.delivery");

        private final String messageKey;

        Scope(String messageKey) {
            this.messageKey = messageKey;
        }

        /**
         * @return the message key for this scope's chip label
         */
        public String messageKey() {
            return messageKey;
        }
    }

    private final List<Button> chips = new ArrayList<>();
    private final List<Consumer<Scope>> listeners = new ArrayList<>();
    private Scope selected = Scope.ALL;

    /**
     * Builds the chip row, selecting {@link Scope#ALL} by default.
     */
    public OrderTypeFilter() {
        addClassName("order-type-filter");
        setSpacing(true);
        setPadding(false);
        setAlignItems(Alignment.CENTER);

        for (Scope scope : Scope.values()) {
            Button chip = new Button(Messages.get(scope.messageKey()), event -> select(scope));
            chip.addClassName("order-type-chip");
            chip.getElement().setAttribute("aria-pressed", String.valueOf(scope == selected));
            chips.add(chip);
            add(chip);
        }
        restyle();
    }

    /**
     * Registers a listener notified whenever the active scope changes.
     *
     * @param listener the listener to notify with the new scope
     */
    public void addSelectionListener(Consumer<Scope> listener) {
        listeners.add(listener);
    }

    /**
     * Returns the active scope.
     *
     * @return the active scope
     */
    public Scope getSelected() {
        return selected;
    }

    private void select(Scope scope) {
        if (scope == selected) {
            return;
        }
        selected = scope;
        restyle();
        listeners.forEach(listener -> listener.accept(scope));
    }

    private void restyle() {
        for (int i = 0; i < chips.size(); i++) {
            Button chip = chips.get(i);
            boolean active = Scope.values()[i] == selected;
            chip.removeThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_TERTIARY);
            chip.addThemeVariants(active ? ButtonVariant.LUMO_PRIMARY : ButtonVariant.LUMO_TERTIARY);
            chip.getElement().setAttribute("aria-pressed", String.valueOf(active));
        }
    }
}
