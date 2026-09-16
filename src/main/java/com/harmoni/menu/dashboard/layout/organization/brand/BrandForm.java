package com.harmoni.menu.dashboard.layout.organization.brand;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.event.brand.BrandSaveEventListener;
import com.harmoni.menu.dashboard.event.brand.BrandUpdateEventListener;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.shared.Registration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Objects;

@RequiredArgsConstructor
@Slf4j
public class BrandForm extends FormLayout {

    Registration broadcasterRegistration;

    @Getter
    BeanValidationBinder<BrandDto> binder = new BeanValidationBinder<>(BrandDto.class);

    @Getter
    TextField brandNameField = new TextField("Brand name");

    Button saveButton = new Button("Save");
    Button closeButton = new Button("Cancel");
    Button updateButton = new Button("Update");

    @Getter
    UI ui;

    private final RestClientOrganizationService restClientOrganizationService;
    private final Dialog dialog;
    private final FormAction formAction;

    @Getter
    private final transient BrandDto brandDto;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(message -> {
            try {
                BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
                if (ObjectUtils.isNotEmpty(broadcastMessage) && ObjectUtils.isNotEmpty(broadcastMessage.getType())
                        && (broadcastMessage.getType().equals(BroadcastMessage.BRAND_INSERT_SUCCESS) ||
                        broadcastMessage.getType().equals(BroadcastMessage.BRAND_SUCCESS_UPDATED))) {
                        showNotification("Brand created..");
                        close();
                    }

            } catch (JsonProcessingException e) {
                log.error("Broadcast Handler Error", e);
            }
        });
        addValidation();

        add(brandNameField);
        binder.bindInstanceFields(this);
        restructureButton(formAction);

        if (formAction == FormAction.EDIT && ObjectUtils.isNotEmpty(brandDto)) {
            binder.readBean(brandDto);
        }

        addFooterButtons();
    }

    public void showNotification(String text) {
        ui.access(() -> UiUtil.success(text));
    }

    public void close() {
        ui.access(() -> dialog.close());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    private void addValidation() {
        binder.forField(brandNameField)
                .withValidator(value -> value.length() > 2,
                        "Name must contain at least three characters")
                .bind(BrandDto::getName, BrandDto::setName);
    }

    private void addFooterButtons() {
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        saveButton.addClickShortcut(Key.ENTER);
        updateButton.addClickShortcut(Key.ENTER);

        closeButton.addClickShortcut(Key.ESCAPE);

        updateButton.addClickListener(new BrandUpdateEventListener(this, restClientOrganizationService));
        saveButton.addClickListener(new BrandSaveEventListener(this, restClientOrganizationService));
        closeButton.addClickListener(event -> close());

        dialog.getFooter().add(saveButton, updateButton, closeButton);
    }

    public void restructureButton(FormAction formAction) {
        if (Objects.requireNonNull(formAction) == FormAction.CREATE) {
            saveButton.setVisible(true);
            updateButton.setVisible(false);
            closeButton.setVisible(true);
        } else if (formAction == FormAction.EDIT) {
            saveButton.setVisible(false);
            updateButton.setVisible(true);
            closeButton.setVisible(true);
        }
    }
}