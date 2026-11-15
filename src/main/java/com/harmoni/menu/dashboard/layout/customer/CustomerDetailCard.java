package com.harmoni.menu.dashboard.layout.customer;

import com.harmoni.menu.dashboard.dto.CustomerDto;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.apache.commons.lang3.ObjectUtils;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Right-hand card of the Customer page: the selected customer's profile with
 * phone and email masked until the operator reveals them.
 */
public class CustomerDetailCard extends VerticalLayout {

    private static final String DATE_TIME_PATTERN = "dd MMM yyyy HH:mm";

    private final VerticalLayout empty = new VerticalLayout();
    private final VerticalLayout content = new VerticalLayout();

    private CustomerDto customer;
    private boolean revealed;

    public CustomerDetailCard() {
        addClassName("customer-detail-card");
        setPadding(false);
        setSpacing(false);
        setSizeFull();

        buildEmptyState();
        content.setPadding(false);
        content.setSpacing(false);
        content.setSizeFull();
        content.setVisible(false);
        add(empty);
        setFlexGrow(1, empty);
    }

    /**
     * Renders the given customer, or the empty state when {@code customer} is null.
     *
     * @param customer the selected customer
     */
    public void show(CustomerDto customer) {
        this.customer = customer;
        this.revealed = false;
        if (customer == null) {
            removeAll();
            buildEmptyState();
            content.setVisible(false);
            add(empty);
            setFlexGrow(1, empty);
            return;
        }
        render();
    }

    private void render() {
        content.removeAll();
        content.add(header(), info(), footerNote());
        content.setVisible(true);
        removeAll();
        add(content);
        setFlexGrow(1, content);
    }

    private void buildEmptyState() {
        empty.removeAll();
        empty.addClassName("customer-detail-empty");
        empty.setSizeFull();
        empty.setPadding(false);
        empty.setSpacing(false);
        empty.setAlignItems(Alignment.CENTER);
        empty.setJustifyContentMode(JustifyContentMode.CENTER);

        Icon icon = VaadinIcon.USER.create();
        icon.addClassName("customer-detail-empty-icon");

        H3 title = new H3(Messages.get("customer.detail.empty.title"));
        title.addClassName("customer-detail-empty-title");

        Paragraph hint = new Paragraph(Messages.get("customer.detail.empty.hint"));
        hint.addClassName("customer-detail-empty-hint");

        empty.add(icon, title, hint);
    }

    private HorizontalLayout header() {
        Div avatar = new Div();
        avatar.addClassName("customer-detail-avatar");
        avatar.setText(CustomerMask.initials(customer.getName()));

        H2 name = new H2(ObjectUtils.isNotEmpty(customer.getName()) ? customer.getName() : "-");
        name.addClassName("customer-detail-name");

        Span phone = new Span(phone());
        phone.addClassName("customer-detail-subtitle");

        VerticalLayout titleBlock = new VerticalLayout(name, phone);
        titleBlock.setPadding(false);
        titleBlock.setSpacing(false);

        Button reveal = new Button(revealed
                ? Messages.get("customer.detail.hide")
                : Messages.get("customer.detail.show"),
                event -> {
                    revealed = !revealed;
                    render();
                });
        reveal.setIcon(revealed ? VaadinIcon.EYE_SLASH.create() : VaadinIcon.EYE.create());
        reveal.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        reveal.addClassName("customer-reveal-button");

        HorizontalLayout header = new HorizontalLayout(avatar, titleBlock, reveal);
        header.addClassName("customer-detail-header");
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setFlexGrow(1, titleBlock);
        return header;
    }

    private VerticalLayout info() {
        VerticalLayout info = new VerticalLayout();
        info.addClassName("customer-info-list");
        info.setPadding(false);
        info.setSpacing(false);

        info.add(infoRow(Messages.get(Messages.Keys.LABEL_CUSTOMER_EMAIL), email()));
        info.add(infoRow(Messages.get("label.customer.createdAt"),
                formatDate(customer.getCreatedAt())));
        info.add(infoRow(Messages.get("label.customer.updatedAt"),
                formatDate(customer.getUpdatedAt())));
        return info;
    }

    private Span footerNote() {
        Span note = new Span(Messages.get("customer.detail.maskedHint"));
        note.addClassName("customer-detail-note");
        note.setVisible(!revealed);
        return note;
    }

    private Div infoRow(String label, String value) {
        Span name = new Span(label);
        name.addClassName("customer-info-label");

        Span content = new Span(ObjectUtils.isNotEmpty(value) ? value : "-");
        content.addClassName("customer-info-value");

        Div row = new Div(name, content);
        row.addClassName("customer-info-row");
        return row;
    }

    private String phone() {
        if (ObjectUtils.isEmpty(customer.getPhone())) {
            return Messages.get("customer.detail.notSet");
        }
        return revealed ? customer.getPhone() : CustomerMask.phone(customer.getPhone());
    }

    private String email() {
        if (ObjectUtils.isEmpty(customer.getEmail())) {
            return Messages.get("customer.detail.notSet");
        }
        return revealed ? customer.getEmail() : CustomerMask.email(customer.getEmail());
    }

    private static String formatDate(Date date) {
        return date == null ? "-" : new SimpleDateFormat(DATE_TIME_PATTERN).format(date);
    }
}
