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

import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * Centralises tab open / dedupe / close / select behaviour so that repeated
 * "Add" clicks never create duplicate tabs and closing always returns to the
 * first (list) tab. Every tab created here except the fixed "All <Entity>" list tab
 * carries an inline close (X) button.
 */
public final class TabManager {

    /** Label stored on the tab element so any TabManager view can resolve it. */
    private static final String LABEL_PROPERTY = "data-tab-label";

    private final TabSheet tabSheet;

    public TabManager(TabSheet tabSheet) {
        this.tabSheet = tabSheet;
    }

    public Optional<Tab> findByLabel(String label) {
        for (int i = 0; i < tabSheet.getTabCount(); i++) {
            Tab tab = tabSheet.getTabAt(i);
            if (Objects.equals(label, tab.getElement().getProperty(LABEL_PROPERTY))) {
                return Optional.of(tab);
            }
        }
        return Optional.empty();
    }

    /**
     * Creates a tab with a label and a close (X) button wired to
     * {@link #closeAndSelectFirst(Tab)}.
     */
    public Tab createClosableTab(String label) {
        Span text = new Span(label);

        Icon closeIcon = new Icon(VaadinIcon.CLOSE_SMALL);
        closeIcon.setSize("16px");
        Button closeButton = new Button(closeIcon);
        closeButton.addClassName("tab-close");
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_SMALL);
        closeButton.setTooltipText("Close tab");

        HorizontalLayout header = new HorizontalLayout(text, closeButton);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setSpacing(false);
        header.getThemeList().add("spacing-xs");

        Tab tab = new Tab(header);
        tab.getElement().setProperty(LABEL_PROPERTY, label);
        closeButton.addClickListener(event -> closeAndSelectFirst(tab));
        return tab;
    }

    public Tab addOrSelect(String label, Function<Tab, Component> contentFactory) {
        return findByLabel(label)
                .map(existing -> {
                    tabSheet.setSelectedTab(existing);
                    return existing;
                })
                .orElseGet(() -> {
                    Tab tab = createClosableTab(label);
                    tabSheet.add(tab, contentFactory.apply(tab));
                    tabSheet.setSelectedTab(tab);
                    return tab;
                });
    }

    public void closeAndSelectFirst(Tab tab) {
        if (tab == null || indexOf(tab) < 0) {
            return;
        }
        tabSheet.remove(tab);
        if (tabSheet.getTabCount() > 0) {
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
