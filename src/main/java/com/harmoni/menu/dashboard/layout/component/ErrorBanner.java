package com.harmoni.menu.dashboard.layout.component;

import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import java.util.function.Consumer;

/**
 * Non-blocking banner for unexpected server failures. It stays visible until the
 * operator dismisses it and opens the full response detail on demand, so a 500
 * no longer takes the whole page hostage.
 */
public class ErrorBanner extends HorizontalLayout {

    private final Span message = new Span();
    private final Span reference = new Span();
    private final Button details;
    private final Consumer<ErrorDetail> detailsListener;

    private ErrorDetail detail;

    public ErrorBanner(Consumer<ErrorDetail> detailsListener) {
        this.detailsListener = detailsListener;

        addClassName("app-error-banner");
        setWidthFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setSpacing(true);
        setVisible(false);
        getElement().setAttribute("role", "alert");

        Icon icon = VaadinIcon.WARNING.create();
        icon.addClassName("app-error-banner-icon");

        message.addClassName("app-error-banner-message");
        reference.addClassName("app-error-banner-reference");
        reference.setVisible(false);

        details = new Button(Messages.get("error.banner.details"), event -> {
            if (detail != null && detailsListener != null) {
                detailsListener.accept(detail);
            }
        });
        details.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE);
        details.addClassName("app-error-banner-action");

        Button dismiss = new Button(VaadinIcon.CLOSE_SMALL.create(), event -> setVisible(false));
        dismiss.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY_INLINE,
                ButtonVariant.LUMO_ICON);
        dismiss.getElement().setAttribute("title", Messages.get("error.banner.dismiss"));
        dismiss.addClassName("app-error-banner-action");

        add(icon, message, reference, details, dismiss);
        setFlexGrow(1, message);
    }

    /**
     * Shows the banner with the given failure.
     *
     * @param detail the failure to show
     */
    public void show(ErrorDetail detail) {
        this.detail = detail;
        message.setText(detail == null ? "" : detail.getSummary());
        reference.setText(detail == null ? "" : Messages.get("error.banner.reference", detail.getReference()));
        reference.setVisible(detail != null);
        setVisible(true);
    }
}
