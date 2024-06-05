package com.harmoni.menu.dashboard.layout.organization.brand;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.event.brand.BrandSaveEventListener;
import com.harmoni.menu.dashboard.event.brand.BrandUpdateEventListener;
import com.harmoni.menu.dashboard.layout.component.DialogClosing;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
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
import org.springframework.beans.factory.annotation.Autowired;

@Route("brand-form")
@Slf4j
public class BrandForm extends FormLayout  {
    Registration broadcasterRegistration;
    @Getter
    BeanValidationBinder<BrandDto> binder = new BeanValidationBinder<>(BrandDto.class);
    @Getter
    TextField brandNameField = new TextField("Brand name");
    @Getter
    ComboBox<ChainDto> chainBox = new ComboBox<>("Chain");
    private final Button saveButton = new Button("Save");
    private final Button  deleteButton = new Button("Delete");
    private final Button  closeButton = new Button("Cancel");
    private final Button  updateButton = new Button("Update");
    private final RestClientOrganizationService restClientOrganizationService;
    @Getter
    private UI ui;
    @Getter
    private BrandDto brandDto;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;

    public BrandForm(@Autowired AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                     RestClientOrganizationService restClientOrganizationService) {
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
        this.restClientOrganizationService = restClientOrganizationService;
        addValidation();

        chainBox.setItemLabelGenerator(ChainDto::getName);

        add(chainBox);
        add(brandNameField);

        add(createButtonsLayout());
        binder.bindInstanceFields(this);

        fetchChains();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(message -> {
            try {
                BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
                if (ObjectUtils.isNotEmpty(broadcastMessage) && ObjectUtils.isNotEmpty(broadcastMessage.getType())) {
                    if (broadcastMessage.getType().equals(BroadcastMessage.BRAND_INSERT_SUCCESS)) {
                        showNotification("Brand created..");
                        hideForm();
                    }
                    if (broadcastMessage.getType().equals(BroadcastMessage.BAD_REQUEST_FAILED)) {
                        showErrorDialog(message);
                    }
                    if (broadcastMessage.getType().equals(BroadcastMessage.PROCESS_FAILED)) {
                        showErrorDialog(message);
                    }
                }
            } catch (JsonProcessingException e) {
                log.error("Broadcast Handler Error", e);
            }
        });
    }


    public void showNotification(String text) {
        ui.access(()->{
            Notification notification = new Notification(text, 3000,
                    Notification.Position.MIDDLE);
            notification.open();
        });
    }

    private void showErrorDialog(String message) {
        DialogClosing dialog = new DialogClosing(message);
        ui.access(()-> {
            add(dialog);
            dialog.open();
        });
    }

    public void hideForm() {
        ui.access(()->{
            this.setVisible(false);
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    private void addValidation() {
        brandNameField.addValueChangeListener(
                (HasValue.ValueChangeListener<AbstractField.ComponentValueChangeEvent<TextField, String>>) changeEvent -> binder.validate());
        binder.forField(chainBox)
                .withValidator(value -> value.getId()>0,
                        "Chain must be not empty")
                .bind(BrandDto::getChainDto, BrandDto::setChainDto);
        binder.forField(brandNameField)
                .withValidator(value -> value.length()>2,
                        "Name must contain at least three characters")
                .bind(BrandDto::getName, BrandDto::setName);
    }

    void setBrandDto(BrandDto brandDto) {
        this.brandDto = brandDto;
        if (!ObjectUtils.isEmpty(this.brandDto) &&
                !ObjectUtils.isEmpty(this.brandDto.getChainId())) {
            fetchDetailChains(brandDto.getChainId().longValue());
        }
        binder.readBean(brandDto);
    }

    private void fetchChains() {
        asyncRestClientOrganizationService.getAllChainAsync(result -> {
            ui.access(()->{
                chainBox.setItems(result);
            });
        });
    }

    private void fetchDetailChains(Long id) {
        asyncRestClientOrganizationService.getDetailChainAsync(result -> {
            ui.access(()->{
                chainBox.setValue(result);
            });
        }, id);
    }

    private HorizontalLayout createButtonsLayout() {

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        saveButton.addClickShortcut(Key.ENTER);
        updateButton.addClickShortcut(Key.ENTER);

        closeButton.addClickShortcut(Key.ESCAPE);

        updateButton.addClickListener(new BrandUpdateEventListener(this, restClientOrganizationService));

        saveButton.addClickListener(
                new BrandSaveEventListener(this, restClientOrganizationService));
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
}
