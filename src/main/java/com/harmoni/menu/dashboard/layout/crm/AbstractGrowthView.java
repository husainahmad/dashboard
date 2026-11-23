package com.harmoni.menu.dashboard.layout.crm;

import com.harmoni.menu.dashboard.layout.component.PageHeader;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Shared page shell for the Customer Growth screens: the same list-view
 * container, header, toolbar and content slot the other admin pages use.
 */
public abstract class AbstractGrowthView extends VerticalLayout {

    protected final VerticalLayout content = new VerticalLayout();

    protected AbstractGrowthView(String viewClass) {
        addClassName(Css.LIST_VIEW);
        addClassName(viewClass);
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        content.setPadding(false);
        content.setSpacing(true);
        content.setWidthFull();
        add(content);
        setFlexGrow(1, content);
    }

    protected void render(PageHeader header, HorizontalLayout toolbar, Component... body) {
        content.removeAll();
        content.add(header);
        if (toolbar != null) {
            content.add(toolbar);
        }
        content.add(body);
    }

    protected static HorizontalLayout toolbar(Component... components) {
        HorizontalLayout bar = new HorizontalLayout(components);
        bar.addClassName(Css.TOOLBAR);
        bar.setWidthFull();
        bar.setAlignItems(FlexComponent.Alignment.CENTER);
        bar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        return bar;
    }

    protected static HorizontalLayout cardRow(Component... cards) {
        HorizontalLayout row = new HorizontalLayout(cards);
        row.addClassName(Css.STAT_CARDS);
        row.setWidthFull();
        row.setPadding(false);
        row.setSpacing(true);
        return row;
    }

    protected static Button button(String labelKey, VaadinIcon icon,
                                   com.vaadin.flow.component.ComponentEventListener<
                                           com.vaadin.flow.component.ClickEvent<Button>> listener) {
        Button button = new Button(Messages.get(labelKey), icon.create(), listener);
        button.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        return button;
    }

    protected static Component gridSlot(Grid<?> grid) {
        VerticalLayout slot = new VerticalLayout(grid);
        slot.addClassName("grid-slot");
        slot.setPadding(false);
        slot.setSpacing(false);
        slot.setSizeFull();
        return slot;
    }

    protected void loadError(Runnable retry) {
        UiUtil.errorWithRetry(Messages.get("growth.error.load"), retry);
    }

    protected void actionError(Runnable retry) {
        UiUtil.errorWithRetry(Messages.get("growth.error.action"), retry);
    }
}
