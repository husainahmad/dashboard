package com.harmoni.menu.dashboard.layout.setting.service;

import com.harmoni.menu.dashboard.dto.ServiceDto;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Objects;

@RequiredArgsConstructor
@Slf4j
public class ServiceForm extends FormLayout {

    @Getter
    BeanValidationBinder<ServiceDto> binder = new BeanValidationBinder<>(ServiceDto.class);

    @Getter
    TextField serviceNameField = new TextField("Service name");

    Button saveButton = new Button("Save");
    Button deleteButton = UiUtil.deleteButton("Delete");
    Button closeButton = new Button("Cancel");
    Button updateButton = new Button("Update");

    @Getter
    UI ui;

    private final Dialog dialog;
    private final FormAction formAction;

    @Getter
    private final transient ServiceDto serviceDto;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        addValidation();
        add(serviceNameField);
        binder.bindInstanceFields(this);
        restructureButton(formAction);

        if (formAction == FormAction.EDIT && ObjectUtils.isNotEmpty(serviceDto)) {
            binder.readBean(serviceDto);
        }

        addFooterButtons();
    }

    public void showNotification(String text) {
        ui.access(() -> UiUtil.success(text));
    }

    public void close() {
        ui.access(() -> dialog.close());
    }

    private void addValidation() {
        binder.forField(serviceNameField)
                .withValidator(value -> value.length() > 2,
                        "Name must contain at least three characters")
                .bind(ServiceDto::getName, ServiceDto::setName);
    }

    private void addFooterButtons() {

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        saveButton.addClickShortcut(Key.ENTER);
        updateButton.addClickShortcut(Key.ENTER);

        closeButton.addClickShortcut(Key.ESCAPE);

        closeButton.addClickListener(event -> close());

        dialog.getFooter().add(saveButton, updateButton, deleteButton, closeButton);
    }

    public void restructureButton(FormAction formAction) {
        if (Objects.requireNonNull(formAction) == FormAction.CREATE) {
            saveButton.setVisible(true);
            updateButton.setVisible(false);
            deleteButton.setVisible(false);
            closeButton.setVisible(true);
        } else if (formAction == FormAction.EDIT) {
            saveButton.setVisible(false);
            updateButton.setVisible(true);
            deleteButton.setVisible(true);
            closeButton.setVisible(true);
        }
    }
}