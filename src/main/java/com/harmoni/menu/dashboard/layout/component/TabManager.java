package com.harmoni.menu.dashboard.layout.component;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.harmoni.menu.dashboard.util.Messages;
import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Centralises tab open / dedupe / close / select behaviour so that repeated
 * "Add" clicks never create duplicate tabs and closing always returns to the
 * tab that was active before it opened. Every tab created here except the fixed
 * "All <Entity>" list tab carries an inline close (X) button.
 *
 * <p>Dedupe is keyed by a stable key (normally {@code entity:id}) rather than by
 * the visible label, so two records that happen to share a display name still
 * get their own tab. Callers that only have a label can use the
 * {@link #addOrSelect(String, Function)} overload, which uses the label as key.</p>
 *
 * <p>Return targets and close guards are stored per tab, so a single manager may
 * host several tabs without one form's unsaved-changes guard leaking into
 * another.</p>
 */
@RequiredArgsConstructor
public final class TabManager {

    /** Visible label stored on the tab element, for {@link #findByLabel(String)}. */
    private static final String LABEL_PROPERTY = "data-tab-label";

    /** Stable dedupe key stored on the tab element, for {@link #findByKey(String)}. */
    private static final String KEY_PROPERTY = "data-tab-key";

    private final TabSheet tabSheet;

    /**
     * Tab that was selected when each form tab was opened, keyed by that form
     * tab. Closing the form returns here, so a page whose first tab is not the
     * list (for example the table page, where the floor plan comes first) still
     * lands back on the list the operator came from.
     */
    private final Map<Tab, Tab> returnTabs = new HashMap<>();

    /**
     * Optional guards consulted before a tab is closed, keyed by that tab.
     * Returning {@code true} allows the close; {@code false} keeps the tab open,
     * which lets a form intercept the close and ask the operator to confirm
     * discarding unsaved changes.
     */
    private final Map<Tab, Predicate<Tab>> closeGuards = new HashMap<>();

    /**
     * Finds the first tab whose stored label equals the given one.
     *
     * @param label the label to search for
     * @return the matching tab, or empty when no tab carries the label
     */
    public Optional<Tab> findByLabel(String label) {
        return findTab(LABEL_PROPERTY, label);
    }

    /**
     * Finds the first tab whose stored dedupe key equals the given one.
     *
     * @param key the key to search for
     * @return the matching tab, or empty when no tab carries the key
     */
    public Optional<Tab> findByKey(String key) {
        return findTab(KEY_PROPERTY, key);
    }

    private Optional<Tab> findTab(String property, String value) {
        for (int i = 0; i < tabSheet.getTabCount(); i++) {
            Tab tab = tabSheet.getTabAt(i);
            if (Objects.equals(value, tab.getElement().getProperty(property))) {
                return Optional.of(tab);
            }
        }
        return Optional.empty();
    }

    /**
     * Creates a tab with a label and a close (X) button wired to
     * {@link #closeAndSelectFirst(Tab)}. The label doubles as the dedupe key.
     */
    public Tab createClosableTab(String label) {
        return createClosableTab(label, label, null);
    }

    /**
     * Creates a tab with a visible label, a stable dedupe key and a close (X)
     * button wired to {@link #closeAndSelectFirst(Tab)}.
     *
     * @param key   stable key used to dedupe tabs (normally {@code entity:id})
     * @param label visible label shown on the tab
     */
    public Tab createClosableTab(String key, String label) {
        return createClosableTab(key, label, null);
    }

    /**
     * Creates a tab with an optional leading icon, a visible label, a stable
     * dedupe key and a close (X) button wired to
     * {@link #closeAndSelectFirst(Tab)}.
     *
     * @param key   stable key used to dedupe tabs (normally {@code entity:id})
     * @param label visible label shown on the tab
     * @param icon  decorative icon shown before the label, or {@code null}
     */
    public Tab createClosableTab(String key, String label, VaadinIcon icon) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(label, "label");

        Span text = new Span(label);

        Icon closeIcon = new Icon(VaadinIcon.CLOSE_SMALL);
        closeIcon.setSize("16px");
        Button closeButton = new Button(closeIcon);
        closeButton.addClassName("tab-close");
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
        closeButton.setTooltipText(Messages.get("action.closeTab"));

        HorizontalLayout header;
        if (icon == null) {
            header = new HorizontalLayout(text, closeButton);
        } else {
            Icon leading = new Icon(icon);
            leading.setSize("16px");
            leading.addClassName("tab-icon");
            header = new HorizontalLayout(leading, text, closeButton);
        }
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(false);
        header.getThemeList().add("spacing-xs");

        Tab tab = new Tab(header);
        tab.getElement().setProperty(LABEL_PROPERTY, label);
        tab.getElement().setProperty(KEY_PROPERTY, key);
        closeButton.addClickListener(event -> {
            Predicate<Tab> guard = closeGuards.get(tab);
            if (guard == null || guard.test(tab)) {
                closeAndSelectFirst(tab);
            }
        });
        return tab;
    }

    /**
     * Registers a guard consulted before the given tab is closed through its
     * inline close button. The guard is scoped to {@code tab} so it cannot
     * interfere with closing other tabs.
     *
     * @param tab        the tab the guard belongs to
     * @param closeGuard returns {@code true} when the tab may close
     */
    public void setCloseGuard(Tab tab, Predicate<Tab> closeGuard) {
        if (tab == null) {
            return;
        }
        if (closeGuard == null) {
            closeGuards.remove(tab);
        } else {
            closeGuards.put(tab, closeGuard);
        }
    }

    /**
     * Selects an existing tab whose label matches, or creates and selects a new
     * one whose content is built by the factory. The label is also the key.
     *
     * @param label          the tab label to find or create
     * @param contentFactory builds the content for a newly created tab
     * @return the selected existing or new tab
     */
    public Tab addOrSelect(String label, Function<Tab, Component> contentFactory) {
        return addOrSelect(label, label, null, contentFactory);
    }

    /**
     * Selects an existing tab whose dedupe key matches, or creates and selects a
     * new one whose content is built by the factory.
     *
     * @param key            stable key used to dedupe tabs
     * @param label          the visible tab label
     * @param contentFactory builds the content for a newly created tab
     * @return the selected existing or new tab
     */
    public Tab addOrSelect(String key, String label, Function<Tab, Component> contentFactory) {
        return addOrSelect(key, label, null, contentFactory);
    }

    /**
     * Selects an existing tab whose dedupe key matches, or creates and selects a
     * new one with a leading icon whose content is built by the factory.
     *
     * @param key            stable key used to dedupe tabs
     * @param label          the visible tab label
     * @param icon           decorative icon shown before the label, or {@code null}
     * @param contentFactory builds the content for a newly created tab
     * @return the selected existing or new tab
     */
    public Tab addOrSelect(String key, String label, VaadinIcon icon,
                           Function<Tab, Component> contentFactory) {
        return findByKey(key)
                .map(existing -> {
                    tabSheet.setSelectedTab(existing);
                    return existing;
                })
                .orElseGet(() -> {
                    Tab previous = tabSheet.getSelectedTab();
                    Tab tab = createClosableTab(key, label, icon);
                    tabSheet.add(tab, contentFactory.apply(tab));
                    if (previous != null) {
                        returnTabs.put(tab, previous);
                    }
                    tabSheet.setSelectedTab(tab);
                    return tab;
                });
    }

    /**
     * Closes the given tab. When it was the selected tab, selects the tab that
     * was active before it was opened, falling back to the first tab when that
     * tab is gone. Closing a background tab leaves the current selection alone.
     *
     * @param tab the tab to close; ignored when {@code null} or not present
     */
    public void closeAndSelectFirst(Tab tab) {
        if (tab == null || indexOf(tab) < 0) {
            return;
        }
        boolean wasSelected = tab.equals(tabSheet.getSelectedTab());
        Tab returnTab = returnTabs.remove(tab);
        closeGuards.remove(tab);

        tabSheet.remove(tab);

        if (!wasSelected) {
            return;
        }
        if (returnTab != null && returnTab != tab && indexOf(returnTab) >= 0) {
            tabSheet.setSelectedTab(returnTab);
        } else if (tabSheet.getTabCount() > 0) {
            tabSheet.setSelectedIndex(0);
        }
    }

    private int indexOf(Tab tab) {
        for (int i = 0; i < tabSheet.getTabCount(); i++) {
            if (Objects.equals(tab, tabSheet.getTabAt(i))) {
                return i;
            }
        }
        return -1;
    }
}
