package com.harmoni.menu.dashboard.layout.component;

import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.harmoni.menu.dashboard.layout.util.Css;

/**
 * Statistic card shown in the dashboard's summary row: an icon, a large value
 * and a caption. The value switches between the "ok" and "warn" styles via
 * {@link #setWarn(boolean)}.
 */
public class StatCard extends HorizontalLayout {

    private final Span valueLabel;

    public StatCard(VaadinIcon icon, String label) {
        Icon iconComponent = icon.create();
        iconComponent.getStyle().set("--vaadin-icon-size", "24px");
        iconComponent.addClassName("stat-icon");

        valueLabel = new Span("…");
        valueLabel.addClassName("stat-value");
        Paragraph caption = new Paragraph(label);
        caption.addClassName("stat-label");

        VerticalLayout texts = new VerticalLayout(valueLabel, caption);
        texts.setPadding(false);
        texts.setSpacing(false);

        add(iconComponent, texts);
        setAlignItems(FlexComponent.Alignment.CENTER);
        setSpacing(true);
        addClassName("stat-card");
    }

    public void setValue(int value) {
        valueLabel.setText(String.valueOf(value));
    }

    /**
     * Sets a pre-formatted value, for reports whose totals are money rather than
     * plain counts.
     *
     * @param value the already formatted value to show
     */
    public void setValue(String value) {
        valueLabel.setText(value);
    }

    public void setWarn(boolean warn) {
        valueLabel.removeClassName(Css.STAT_VALUE_OK);
        valueLabel.removeClassName(Css.STAT_VALUE_WARN);
        valueLabel.addClassName(warn ? Css.STAT_VALUE_WARN : Css.STAT_VALUE_OK);
    }
}