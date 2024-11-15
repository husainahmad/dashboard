package com.harmoni.menu.dashboard.layout.organization.chain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.event.chain.ChainDeleteEventListener;
import com.harmoni.menu.dashboard.event.chain.ChainSaveEventListener;
import com.harmoni.menu.dashboard.event.chain.ChainUpdateEventListener;
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

import java.util.Objects;

@Route("chain-form")
@Slf4j
public class ChainForm extends FormLayout  {
    Registration broadcasterRegistration;
    @Getter
    BeanValidationBinder<ChainDto> binder = new BeanValidationBinder<>(ChainDto.class);
    @Getter
    ComboBox<BrandDto> brandComboBox = new ComboBox<>("Brand");
    @Getter
    TextField chainNameField = new TextField("Chain name");
    private final Button saveButton = new Button("Save");
    private final Button  deleteButton = new Button("Delete");
    private final Button  closeButton = new Button("Cancel");
    private final Button  updateButton = new Button("Update");
    private final RestClientOrganizationService restClientOrganizationService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;

    @Getter
    private UI ui;
    @Getter
    private transient ChainDto chainDto;

    public ChainForm(RestClientOrganizationService restClientOrganizationService,
                     AsyncRestClientOrganizationService asyncRestClientOrganizationService) {
        this.restClientOrganizationService = restClientOrganizationService;
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;

        addValidation();
        brandComboBox.setItemLabelGenerator(BrandDto::getName);

        add(brandComboBox);
        add(chainNameField);
        add(createButtonsLayout());
        binder.bindInstanceFields(this);

        fetchBrands();
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

    private void showNotification() {
        ui.access(()->{
            Notification notification = new Notification("Chain created..", 3000, Notification.Position.MIDDLE);
            notification.open();
            hideForm();
        });
    }

    public void hideForm() {
        this.setVisible(false);
    }

    private void addValidation() {
        chainNameField.addValueChangeListener(
                (HasValue.ValueChangeListener<AbstractField.ComponentValueChangeEvent<TextField, String>>) changeEvent ->
                        binder.validate());
        binder.forField(brandComboBox)
                .withValidator(value -> value.getId()>0,
                        "Brand must be not empty")
                .bind(ChainDto::getBrandDto, ChainDto::setBrandDto);
        binder.forField(chainNameField)
                .withValidator(value -> value.length()>2,
                        "Name must contain at least three characters")
                .bind(ChainDto::getName, ChainDto::setName);

    }

    void setChainDto(ChainDto chainDto) {
        this.chainDto = chainDto;
        if (!ObjectUtils.isEmpty(this.chainDto) && !ObjectUtils.isEmpty(this.chainDto.getBrandId())) {
            fetchDetailBrands(this.chainDto.getBrandId().longValue());
        }
        binder.readBean(chainDto);
    }

    private void fetchDetailBrands(Long id) {
        asyncRestClientOrganizationService.getDetailBrandAsync(result ->
                ui.access(()-> brandComboBox.setValue(result)), id);
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
        deleteButton.addClickListener(new ChainDeleteEventListener(this, restClientOrganizationService));
        closeButton.addClickListener(buttonClickEvent -> this.setVisible(false));

        return new HorizontalLayout(saveButton, updateButton, updateButton, deleteButton, closeButton);
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

    private void fetchBrands() {
        asyncRestClientOrganizationService.getAllBrandAsync(result -> ui.access(()-> brandComboBox.setItems(result)));
    }

    private void receiptBroadcast(String message) {
        try {
            BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
            if (ObjectUtils.isNotEmpty(broadcastMessage) && ObjectUtils.isNotEmpty(broadcastMessage.getType())
                    && broadcastMessage.getType().equals(BroadcastMessage.CHAIN_INSERT_SUCCESS)) {
                showNotification();
            }
        } catch (JsonProcessingException e) {
            log.error("Broadcast Handler Error", e);
        }
    }
}
