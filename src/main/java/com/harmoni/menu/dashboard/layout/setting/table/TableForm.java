package com.harmoni.menu.dashboard.layout.setting.table;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.TableDto;
import com.harmoni.menu.dashboard.event.table.TableSaveEventListener;
import com.harmoni.menu.dashboard.event.table.TableUpdateEventListener;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.RestClientSettingService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.IntegerField;
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
public class TableForm extends FormLayout {

    Registration broadcasterRegistration;

    @Getter
    BeanValidationBinder<TableDto> binder = new BeanValidationBinder<>(TableDto.class);

    @Getter
    TextField nameField = new TextField("Table name");

    @Getter
    IntegerField capacityField = new IntegerField("Capacity");

    Button saveButton = new Button("Save");
    Button closeButton = new Button("Cancel");
    Button updateButton = UiUtil.updateButton();

    private final Dialog dialog;
    private final FormAction formAction;

    @Getter
    UI ui;

    @Getter
    private final transient TableDto tableDto;

    private final transient RestClientSettingService restClientSettingService;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(this::receiptBroadcast);
        addValidation();

        capacityField.setMin(1);
        capacityField.setMax(999);
        capacityField.setStepButtonsVisible(true);

        add(nameField);
        add(capacityField);
        binder.bindInstanceFields(this);
        restructureButton(formAction);

        if (formAction == FormAction.EDIT && ObjectUtils.isNotEmpty(tableDto)) {
            binder.readBean(tableDto);
        }

        addFooterButtons();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    public void close() {
        ui.access(() -> dialog.close());
    }

    private void addValidation() {
        binder.forField(nameField)
                .withValidator(value -> {
                    String trimmed = value.trim();
                    return trimmed.length() >= 2 && trimmed.length() <= 45;
                }, "Name must contain between two and forty-five characters")
                .bind(TableDto::getName, TableDto::setName);

        binder.forField(capacityField)
                .withValidator(Objects::nonNull, "Capacity is required")
                .withValidator(value -> value >= 1 && value <= 999,
                        "Capacity must be between one and nine hundred ninety-nine")
                .bind(TableDto::getCapacity, TableDto::setCapacity);
    }

    private void addFooterButtons() {
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        saveButton.addClickShortcut(Key.ENTER);
        updateButton.addClickShortcut(Key.ENTER);

        closeButton.addClickShortcut(Key.ESCAPE);

        updateButton.addClickListener(new TableUpdateEventListener(this, restClientSettingService));
        saveButton.addClickListener(new TableSaveEventListener(this, restClientSettingService));
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

    private void receiptBroadcast(String message) {
        try {
            BroadcastMessage broadcastMessage =
                    (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
            if (ObjectUtils.isNotEmpty(broadcastMessage) && ObjectUtils.isNotEmpty(broadcastMessage.getType())
                    && (broadcastMessage.getType().equals(BroadcastMessage.TABLE_INSERT_SUCCESS) ||
                        broadcastMessage.getType().equals(BroadcastMessage.TABLE_UPDATED_SUCCESS))) {

                boolean isInsert = broadcastMessage.getType().equals(BroadcastMessage.TABLE_INSERT_SUCCESS);
                ui.access(() -> {
                    UiUtil.success(isInsert ? "Table created.." : "Table updated..");
                    dialog.close();
                });
            }
        } catch (JsonProcessingException e) {
            log.error("Broadcast Handler Error", e);
        }
    }
}
