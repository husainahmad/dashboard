package com.harmoni.menu.dashboard.layout.component;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Summary tile used by the operational dashboard: a muted label, a large value
 * and a small caption line under it.
 *
 * <p>The value can be split into a small prefix (a currency such as "Rp") and
 * the amount beneath it, which is how the sales tile reads. The caption may
 * carry a trend, which colours it and prefixes it with an arrow.</p>
 */
public class KpiCard extends VerticalLayout {

    /** Direction of the caption's trend marker. */
    public enum Trend {
        UP, DOWN, NONE
    }

    private final Span prefixLabel = new Span();
    private final Span valueLabel = new Span("—");
    private final Span captionLabel = new Span();

    /**
     * @param label the tile label shown above the value
     */
    public KpiCard(String label) {
        Span labelSpan = new Span(label);
        labelSpan.addClassName("kpi-label");

        prefixLabel.addClassName("kpi-value-prefix");
        prefixLabel.setVisible(false);
        valueLabel.addClassName("kpi-value");
        captionLabel.addClassName("kpi-caption");

        add(labelSpan, prefixLabel, valueLabel, captionLabel);
        setPadding(false);
        setSpacing(false);
        addClassName("kpi-card");
    }

    /**
     * Sets the value as a single line and hides the currency prefix.
     *
     * @param value the value to show
     */
    public void setValue(String value) {
        prefixLabel.setVisible(false);
        valueLabel.setText(value);
    }

    /**
     * Sets a split value: a small prefix above a large amount.
     *
     * @param prefix the small prefix, e.g. "Rp"
     * @param value  the large amount
     */
    public void setValue(String prefix, String value) {
        prefixLabel.setText(prefix);
        prefixLabel.setVisible(prefix != null && !prefix.isBlank());
        valueLabel.setText(value);
    }

    /**
     * Sets the caption line and its trend marker.
     *
     * @param text  the caption text, or {@code null}/blank to hide it
     * @param trend the trend direction
     */
    public void setCaption(String text, Trend trend) {
        captionLabel.setText(text == null ? "" : text);
        captionLabel.setVisible(text != null && !text.isBlank());
        captionLabel.removeClassName("kpi-caption--up");
        captionLabel.removeClassName("kpi-caption--down");
        if (trend == Trend.UP) {
            captionLabel.addClassName("kpi-caption--up");
        } else if (trend == Trend.DOWN) {
            captionLabel.addClassName("kpi-caption--down");
        }
    }

    /**
     * Sets the caption with no trend marker.
     *
     * @param text the caption text
     */
    public void setCaption(String text) {
        setCaption(text, Trend.NONE);
    }
}
