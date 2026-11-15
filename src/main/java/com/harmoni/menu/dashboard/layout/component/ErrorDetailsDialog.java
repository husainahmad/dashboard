package com.harmoni.menu.dashboard.layout.component;

import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import org.apache.commons.lang3.ObjectUtils;

/**
 * Friendly explanation shown from {@link ErrorBanner}. The operator gets a plain
 * description and a short support reference; the server payload stays behind a
 * collapsed "technical details" section for support staff.
 */
public class ErrorDetailsDialog extends Dialog {

    public ErrorDetailsDialog(ErrorDetail detail) {
        addClassName("app-error-details-dialog");
        setHeaderTitle(Messages.get("error.details.title"));
        setWidth("560px");

        Paragraph summary = new Paragraph(Messages.get("error.details.summary"));
        summary.addClassName("app-error-details-summary");

        Span reference = new Span(detail == null
                ? "-" : Messages.get("error.details.reference", detail.getReference()));
        reference.addClassName("app-error-details-reference");

        VerticalLayout content = new VerticalLayout(summary, reference);
        content.setPadding(false);
        content.setSpacing(true);

        TextArea technical = new TextArea();
        technical.addClassName("app-error-details-json");
        technical.setWidthFull();
        technical.setHeight("220px");
        technical.setReadOnly(true);
        technical.setValue(detail == null || ObjectUtils.isEmpty(detail.technicalText())
                ? Messages.get("error.details.none") : detail.technicalText());

        Details technicalDetails = new Details(Messages.get("error.details.technical"), technical);
        technicalDetails.setOpened(false);
        technicalDetails.addClassName("app-error-details-technical");
        content.add(technicalDetails);

        add(content);

        Button copy = new Button(Messages.get("error.details.copyReference"));
        copy.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        copy.addClickListener(event -> {
            String text = detail == null ? "" : detail.getReference();
            UI.getCurrent().getPage().executeJs("navigator.clipboard.writeText($0)", text);
            copy.setText(Messages.get("error.details.copied"));
        });

        Button close = new Button(Messages.get("error.details.close"), event -> close());
        close.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        getFooter().add(copy, close);
    }
}
