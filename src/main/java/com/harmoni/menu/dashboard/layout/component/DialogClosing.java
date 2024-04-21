package com.harmoni.menu.dashboard.layout.component;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

public class DialogClosing extends Dialog {

    private String text;
    public DialogClosing(String text) {
        this.text = text;
        getElement().setAttribute("arial-label", "System failed");
        VerticalLayout dialogLayout = createDialogLayout(this);
        add(dialogLayout);
    }

    private VerticalLayout createDialogLayout(Dialog dialog) {
        H2 headline = new H2("Failed to process");
        headline.getStyle().set("margin", "var(--lumo-space-m) 0")
                .set("font-size", "1.5em").set("font-weight", "bold");

        Paragraph paragraph = new Paragraph(this.text);

        Button closeButton = new Button("Close");
        closeButton.addClickListener(e -> dialog.close());

        VerticalLayout dialogLayout = new VerticalLayout(headline, paragraph,
                closeButton);
        dialogLayout.setPadding(false);
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("width", "300px").set("max-width", "100%");
        dialogLayout.setAlignSelf(FlexComponent.Alignment.END, closeButton);

        return dialogLayout;
    }
}
