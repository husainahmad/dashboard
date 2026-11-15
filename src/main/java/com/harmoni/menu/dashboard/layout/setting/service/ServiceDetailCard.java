package com.harmoni.menu.dashboard.layout.setting.service;

import com.harmoni.menu.dashboard.dto.ServiceDto;
import com.harmoni.menu.dashboard.dto.SubServiceDto;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.apache.commons.lang3.ObjectUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

/**
 * Right-hand card of the Service page: the selected service, its sub-services
 * and the edit / delete actions.
 */
public class ServiceDetailCard extends VerticalLayout {

    private static final String DATE_TIME_PATTERN = "dd MMM yyyy HH:mm";

    private final VerticalLayout empty = new VerticalLayout();
    private final VerticalLayout content = new VerticalLayout();

    private Consumer<ServiceDto> editListener;
    private Consumer<ServiceDto> deleteListener;

    public ServiceDetailCard() {
        addClassName("service-detail-card");
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
     * Renders the given service, or the empty state when {@code service} is null.
     *
     * @param service the selected service
     */
    public void show(ServiceDto service) {
        removeAll();
        if (service == null) {
            buildEmptyState();
            content.setVisible(false);
            add(empty);
            setFlexGrow(1, empty);
            return;
        }
        content.removeAll();
        content.add(header(service), info(service), actions(service));
        content.setVisible(true);
        add(content);
        setFlexGrow(1, content);
    }

    public void setEditListener(Consumer<ServiceDto> listener) {
        this.editListener = listener;
    }

    public void setDeleteListener(Consumer<ServiceDto> listener) {
        this.deleteListener = listener;
    }

    private void buildEmptyState() {
        empty.removeAll();
        empty.addClassName("service-detail-empty");
        empty.setSizeFull();
        empty.setPadding(false);
        empty.setSpacing(false);
        empty.setAlignItems(Alignment.CENTER);
        empty.setJustifyContentMode(JustifyContentMode.CENTER);

        Icon icon = VaadinIcon.COG.create();
        icon.addClassName("service-detail-empty-icon");

        H3 title = new H3(Messages.get("service.detail.empty.title"));
        title.addClassName("service-detail-empty-title");

        Paragraph hint = new Paragraph(Messages.get("service.detail.empty.hint"));
        hint.addClassName("service-detail-empty-hint");

        empty.add(icon, title, hint);
    }

    private HorizontalLayout header(ServiceDto service) {
        Div avatar = new Div(new Icon(VaadinIcon.COG));
        avatar.addClassName("service-detail-avatar");

        H2 name = new H2(ObjectUtils.isNotEmpty(service.getName()) ? service.getName() : "-");
        name.addClassName("service-detail-name");

        Span subtitle = new Span(Messages.get("service.detail.subServiceCount", subServiceCount(service)));
        subtitle.addClassName("service-detail-subtitle");

        VerticalLayout titleBlock = new VerticalLayout(name, subtitle);
        titleBlock.setPadding(false);
        titleBlock.setSpacing(false);

        HorizontalLayout header = new HorizontalLayout(avatar, titleBlock);
        header.addClassName("service-detail-header");
        header.setWidthFull();
        header.setAlignItems(Alignment.CENTER);
        header.setFlexGrow(1, titleBlock);
        return header;
    }

    private VerticalLayout info(ServiceDto service) {
        VerticalLayout info = new VerticalLayout();
        info.addClassName("service-info-list");
        info.setPadding(false);
        info.setSpacing(false);

        info.add(infoRow(Messages.get("service.detail.subServices"), subServiceChips(service)));
        info.add(infoRow(Messages.get("service.detail.created"), text(formatDate(service.getCreatedAt()))));
        info.add(infoRow(Messages.get("service.detail.updated"), text(formatDate(service.getUpdatedAt()))));
        return info;
    }

    private Component subServiceChips(ServiceDto service) {
        if (service.getSubServices() == null || service.getSubServices().isEmpty()) {
            return text(Messages.get("service.detail.noSubServices"));
        }
        Div chips = new Div();
        chips.addClassName("service-chip-list");
        for (SubServiceDto subService : service.getSubServices()) {
            Span chip = new Span(ObjectUtils.isNotEmpty(subService.getName())
                    ? subService.getName() : "-");
            chip.addClassName("service-chip");
            chips.add(chip);
        }
        return chips;
    }

    private Div infoRow(String label, Component value) {
        Span name = new Span(label);
        name.addClassName("service-info-label");

        Div row = new Div(name, value);
        row.addClassName("service-info-row");
        return row;
    }

    private Span text(String value) {
        Span text = new Span(ObjectUtils.isNotEmpty(value) ? value : "-");
        text.addClassName("service-info-value");
        return text;
    }

    private HorizontalLayout actions(ServiceDto service) {
        Button edit = new Button(Messages.get(Messages.Keys.ACTION_EDIT),
                event -> editListener.accept(service));
        edit.setIcon(VaadinIcon.PENCIL.create());
        edit.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button delete = new Button(Messages.get(Messages.Keys.ACTION_DELETE),
                event -> deleteListener.accept(service));
        delete.setIcon(VaadinIcon.TRASH.create());
        delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        delete.addClassName("service-delete-button");

        HorizontalLayout actions = new HorizontalLayout(delete, edit);
        actions.addClassName("service-action-bar");
        actions.setWidthFull();
        actions.setAlignItems(FlexComponent.Alignment.CENTER);
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        return actions;
    }

    private int subServiceCount(ServiceDto service) {
        return service.getSubServices() == null ? 0 : service.getSubServices().size();
    }

    private static String formatDate(Date date) {
        return date == null ? "-" : new SimpleDateFormat(DATE_TIME_PATTERN).format(date);
    }
}
