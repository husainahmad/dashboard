package com.harmoni.menu.dashboard.layout.organization;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

/**
 * One settings row in the organization detail card: a label, an inheritance
 * badge, the source of the value and an optional action button.
 */
public class SettingRow extends HorizontalLayout {

    private final Span badge = new Span();
    private final Span source = new Span();
    private final Button action = new Button();

    public SettingRow(String label, String badgeText, boolean overridden,
                      String sourceText, String actionText, Runnable onAction) {
        addClassName("org-setting-row");
        setWidthFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setSpacing(false);

        Span labelSpan = new Span(label);
        labelSpan.addClassName("org-setting-label");

        badge.setText(badgeText);
        badge.addClassName("org-badge");
        badge.addClassName(overridden ? "org-badge-override" : "org-badge-inherit");

        source.setText(sourceText);
        source.addClassName("org-setting-source");

        HorizontalLayout value = new HorizontalLayout(badge, source);
        value.setAlignItems(FlexComponent.Alignment.CENTER);
        value.setSpacing(true);
        value.addClassName("org-setting-value");

        add(labelSpan, value);
        setFlexGrow(1, value);

        if (actionText != null && onAction != null) {
            action.setText(actionText);
            action.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            action.addClassName("org-setting-action");
            action.addClickListener(event -> onAction.run());
            add(action);
        }
    }
}
