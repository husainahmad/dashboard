package com.harmoni.menu.dashboard.layout.organization.brand;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.event.brand.BrandSaveEventListener;
import com.harmoni.menu.dashboard.event.brand.BrandUpdateEventListener;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Objects;

@RequiredArgsConstructor
@Route("brand-form")
@Slf4j
public class BrandForm extends FormLayout  {
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
    @Getter
    transient BrandDto brandDto;
    private final RestClientOrganizationService restClientOrganizationService;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(message -> {
            try {
                BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
                if (ObjectUtils.isNotEmpty(broadcastMessage) && ObjectUtils.isNotEmpty(broadcastMessage.getType())
                        && broadcastMessage.getType().equals(BroadcastMessage.BRAND_INSERT_SUCCESS)) {
                        showNotification("Brand created..");
                        hideForm();
                    }

            } catch (JsonProcessingException e) {
                log.error("Broadcast Handler Error", e);
            }
        });
        addValidation();

        add(brandNameField);

        add(createButtonsLayout());
        binder.bindInstanceFields(this);
    }


    public void showNotification(String text) {
        ui.access(()->{
            Notification notification = new Notification(text, 3000,
                    Notification.Position.MIDDLE);
            notification.open();
        });
    }

    public void hideForm() {
        ui.access(()-> this.setVisible(false));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    private void addValidation() {
        brandNameField.addValueChangeListener(
                (HasValue.ValueChangeListener<AbstractField.ComponentValueChangeEvent<TextField, String>>) _ -> binder.validate());

        binder.forField(brandNameField)
                .withValidator(value -> value.length()>2,
                        "Name must contain at least three characters")
                .bind(BrandDto::getName, BrandDto::setName);
    }

    void setBrandDto(BrandDto brandDto) {
        this.brandDto = brandDto;
        binder.readBean(brandDto);
    }

    private HorizontalLayout createButtonsLayout() {

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        saveButton.addClickShortcut(Key.ENTER);
        updateButton.addClickShortcut(Key.ENTER);

        closeButton.addClickShortcut(Key.ESCAPE);

        updateButton.addClickListener(new BrandUpdateEventListener(this, restClientOrganizationService));

        saveButton.addClickListener(
                new BrandSaveEventListener(this, restClientOrganizationService));
        closeButton.addClickListener(_ -> this.setVisible(false));

        HorizontalLayout horizontalLayout = new HorizontalLayout(saveButton, updateButton, updateButton, closeButton);
        horizontalLayout.setPadding(true);

        return horizontalLayout;
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
