package com.harmoni.menu.dashboard.layout.setting.service;

import com.harmoni.menu.dashboard.dto.ServiceDto;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Objects;

/**
 * Tab form for creating or editing a service, binding the single service-name
 * field through a {@link BeanValidationBinder}. The footer buttons are laid out
 * from the {@link FormAction} (save for CREATE, update/delete for EDIT) and
 * success/close feedback is marshalled back onto the UI thread.
 */
@RequiredArgsConstructor
@Slf4j
public class ServiceForm extends FormLayout {

    /** Binder that drives field validation and reads/writes the {@link ServiceDto}. */
    @Getter
    BeanValidationBinder<ServiceDto> binder = new BeanValidationBinder<>(ServiceDto.class);

    /** Text field holding the service name. */
    @Getter
    TextField serviceNameField = new TextField(Messages.get(Messages.Keys.LABEL_FIELD_SERVICE_NAME));

    Button saveButton = new Button(Messages.get(Messages.Keys.ACTION_SAVE));
    Button deleteButton = UiUtil.deleteButton(Messages.get(Messages.Keys.ACTION_DELETE));
    Button closeButton = new Button(Messages.get(Messages.Keys.ACTION_CANCEL));
    Button updateButton = new Button(Messages.get(Messages.Keys.ACTION_UPDATE));

    /** UI this form was attached to, used to marshal callbacks onto the UI thread. */
    @Getter
    UI ui;

    private final TabManager tabManager;
    private final Tab currentTab;
    private final FormAction formAction;

    /** The service being edited, empty when creating a new one. */
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

    /**
     * Shows a success notification on the UI thread.
     *
     * @param text the message to display
     */
    public void showNotification(String text) {
        UiUtil.safeAccess(ui, () -> UiUtil.success(text));
    }

    /**
     * Closes the hosting tab on the UI thread.
     */
    public void close() {
        UiUtil.safeAccess(ui, () -> tabManager.closeAndSelectFirst(currentTab));
    }

    /**
     * Adds validation to the service name field, ensuring it has a minimum length
     * of 3 characters. Binds the field to the {@link ServiceDto} name property.
     */
    private void addValidation() {
        binder.forField(serviceNameField)
                .withValidator(value -> value.length() > 2,
                        Messages.get(Messages.Keys.VALIDATION_NAME_MIN_LENGTH))
                .bind(ServiceDto::getName, ServiceDto::setName);
    }

    /**
     * Adds the footer buttons to the form, configuring their theme variants,
     * click shortcuts, and click listeners.
     */
    private void addFooterButtons() {

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        saveButton.addClickShortcut(Key.ENTER);
        updateButton.addClickShortcut(Key.ENTER);

        closeButton.addClickShortcut(Key.ESCAPE);

        closeButton.addClickListener(event -> close());

        HorizontalLayout footer = new HorizontalLayout(saveButton, updateButton, deleteButton, closeButton);
        footer.setWidthFull();
        add(footer);
    }

    /**
     * Toggles the footer button visibility for the given action: the save
     * button for CREATE, update/delete for EDIT.
     *
     * @param formAction whether the form creates or edits
     */
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