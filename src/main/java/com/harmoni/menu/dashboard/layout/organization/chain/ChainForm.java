package com.harmoni.menu.dashboard.layout.organization.chain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.event.chain.ChainSaveEventListener;
import com.harmoni.menu.dashboard.event.chain.ChainUpdateEventListener;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

@Route("chain-form")
@Slf4j
public class ChainForm extends FormLayout  {
    Registration broadcasterRegistration;
    @Getter
    BeanValidationBinder<ChainDto> binder = new BeanValidationBinder<>(ChainDto.class);
    @Getter
    TextField chainNameField = new TextField("Chain name");
    private final Button saveButton = new Button("Save");
    private final Button  deleteButton = new Button("Delete");
    private final Button  closeButton = new Button("Cancel");
    private final Button  updateButton = new Button("Update");
    private final RestClientOrganizationService restClientOrganizationService;
    @Getter
    private UI ui;
    @Getter
    private ChainDto chainDto;

    public ChainForm(RestClientOrganizationService restClientOrganizationService) {
        this.restClientOrganizationService = restClientOrganizationService;
        addValidation();
        add(chainNameField);
        add(createButtonsLayout());
        binder.bindInstanceFields(this);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(this::receiptBroadcast);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    private void showNotification(String text) {
        ui.access(()->{
            Notification notification = new Notification(text, 3000, Notification.Position.MIDDLE);
            notification.open();
            hideForm();
        });
    }

    public void hideForm() {
        this.setVisible(false);
    }

    private void addValidation() {
        chainNameField.addValueChangeListener(
                (HasValue.ValueChangeListener<AbstractField.ComponentValueChangeEvent<TextField, String>>) changeEvent -> binder.validate());
        binder.forField(chainNameField)
                .withValidator(value -> value.length()>2,
                        "Name must contain at least three characters")
                .bind(ChainDto::getName, ChainDto::setName);

    }

    void setChainDto(ChainDto chainDto) {
        this.chainDto = chainDto;
        binder.readBean(chainDto);
    }

    private HorizontalLayout createButtonsLayout() {

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        saveButton.addClickShortcut(Key.ENTER);
        updateButton.addClickShortcut(Key.ENTER);

        closeButton.addClickShortcut(Key.ESCAPE);

        updateButton.addClickListener(new ChainUpdateEventListener(this, restClientOrganizationService));
        saveButton.addClickListener(new ChainSaveEventListener(this, restClientOrganizationService));
        closeButton.addClickListener(buttonClickEvent -> this.setVisible(false));

        return new HorizontalLayout(saveButton, updateButton, updateButton, deleteButton, closeButton);
    }

    public void restructureButton(FormAction formAction) {
        switch (formAction) {
            case CREATE -> {
                saveButton.setVisible(true);
                updateButton.setVisible(false);
                deleteButton.setVisible(false);
                closeButton.setVisible(true);
                break;
            }
            case EDIT -> {
                saveButton.setVisible(false);
                updateButton.setVisible(true);
                deleteButton.setVisible(true);
                closeButton.setVisible(true);
            }
        }
    }

    private void receiptBroadcast(String message) {
        try {
            BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
            if (ObjectUtils.isNotEmpty(broadcastMessage) && ObjectUtils.isNotEmpty(broadcastMessage.getType())
                    && broadcastMessage.getType().equals(BroadcastMessage.CHAIN_INSERT_SUCCESS)) {
                showNotification("Chain created..");
            }
        } catch (JsonProcessingException e) {
            log.error("Broadcast Handler Error", e);
        }

    }
}
