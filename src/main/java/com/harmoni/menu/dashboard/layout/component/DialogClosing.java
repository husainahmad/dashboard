package com.harmoni.menu.dashboard.layout.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

/**
 * Modal error dialog shown when an operation fails. Displays a warning icon,
 * the failure message and a Close button that dismisses the dialog.
 */
public class DialogClosing extends Dialog {

    private final String text;

    /**
     * Creates an error dialog with the given message.
     *
     * @param text the error description to show
     */
    public DialogClosing(String text) {
        this.text = text;

        addClassName("app-error-dialog");
        setHeaderTitle("Something went wrong");
        setAriaLabel("System failure notification");

        Icon icon = new Icon(VaadinIcon.WARNING);
        icon.addClassName("app-error-dialog-icon");
        icon.getStyle().set("--vaadin-icon-size", "28px");

        Paragraph message = new Paragraph(this.text);
        message.addClassName("app-error-dialog-message");

        HorizontalLayout body = new HorizontalLayout(icon, message);
        body.addClassName("app-error-dialog-body");
        body.setSpacing(true);
        body.setPadding(false);
        body.setAlignItems(FlexComponent.Alignment.CENTER);

        Button closeButton = new Button("Close", event -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.getElement().setAttribute("autofocus", true);

        HorizontalLayout footer = new HorizontalLayout(closeButton);
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        footer.setWidthFull();

        VerticalLayout dialogLayout = new VerticalLayout(body, footer);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        add(dialogLayout);
    }
}
