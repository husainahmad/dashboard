package com.harmoni.menu.dashboard.layout.organization.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.event.store.StoreSaveEventListener;
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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;

@Route("store-form")
@Slf4j
public class StoreForm extends FormLayout  {
    Registration broadcasterRegistration;
    @Getter
    BeanValidationBinder<StoreDto> binder = new BeanValidationBinder<>(StoreDto.class);
    @Getter
    TextField storeNameField = new TextField("Store name");
    @Getter
    TextArea addressArea = new TextArea("Address");
    @Getter
    ComboBox<BrandDto> brandBox = new ComboBox<>("Brand");
    @Getter
    ComboBox<TierDto> tierBox = new ComboBox<>("Tier");
    private final Button saveButton = new Button("Save");
    private final Button  deleteButton = new Button("Delete");
    private final Button  closeButton = new Button("Cancel");
    private final Button  updateButton = new Button("Update");
    private final RestClientOrganizationService restClientOrganizationService;
    @Getter
    private UI ui;
    @Getter
    private StoreDto storeDto;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;

    public StoreForm(@Autowired AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                     @Autowired RestClientOrganizationService restClientOrganizationService) {
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
        this.restClientOrganizationService = restClientOrganizationService;
        addValidation();

        brandBox.setItemLabelGenerator(BrandDto::getName);
        brandBox.addValueChangeListener(changeEvent -> {
            if (changeEvent.isFromClient()) {
                tierBox.setItems(new ArrayList<TierDto>());
                fetchDetailTierByBrand(changeEvent.getValue().getId());
            }
        });
        tierBox.setItemLabelGenerator(TierDto::getName);
        add(brandBox);
        add(tierBox);

        add(storeNameField);
        add(addressArea);

        add(createButtonsLayout());
        binder.bindInstanceFields(this);

        fetchBrands();
        fetchTiers();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(message -> {
            try {
                BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
                if (ObjectUtils.isNotEmpty(broadcastMessage) && ObjectUtils.isNotEmpty(broadcastMessage.getType())) {
                    if (broadcastMessage.getType().equals(BroadcastMessage.STORE_INSERT_SUCCESS)) {
                        fetchBrands();
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

    public void hideForm() {
        ui.access(()-> this.setVisible(false));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    private void addValidation() {
        brandBox.addValueChangeListener(changeEvent -> binder.validate());
        binder.forField(brandBox)
                .withValidator(value -> value.getId() > 0, "Brand not allow to be empty"
                ).bind(StoreDto::getBrandDto, StoreDto::setBrandDto);

        tierBox.addValueChangeListener(changeEvent -> binder.validate());

        binder.forField(addressArea)
                .withValidator(value -> !value.isEmpty(), "Address not allow to be empty"
                ).bind(StoreDto::getAddress, StoreDto::setAddress);

        tierBox.addValueChangeListener(changeEvent -> binder.validate());

        binder.forField(tierBox)
                .withValidator(value -> value.getId() > 0, "Tier not allow to be empty"
                ).bind(StoreDto::getTierDto, StoreDto::setTierDto);

        storeNameField.addValueChangeListener(
                (HasValue.ValueChangeListener<AbstractField.ComponentValueChangeEvent<TextField, String>>) changeEvent -> binder.validate());
        binder.forField(storeNameField)
                .withValidator(value -> value.length()>2,
                        "Name must contain at least three characters")
                .bind(StoreDto::getName, StoreDto::setName);
    }

    void setStoreDto(StoreDto storeDto) {
        this.storeDto = storeDto;

        if (!ObjectUtils.isEmpty(this.storeDto) &&
                !ObjectUtils.isEmpty(this.storeDto.getBrandId())) {
            fetchDetailBrands(storeDto.getBrandId().longValue());
            fetchDetailTierByBrand(storeDto.getBrandId());
        }

        binder.readBean(storeDto);
    }

    private void fetchBrands() {
        asyncRestClientOrganizationService.getAllBrandAsync(result -> {
            ui.access(()-> brandBox.setItems(result));
        });
    }

    private void fetchTiers() {
        asyncRestClientOrganizationService.getAllTierAsync(result -> {
            ui.access(()-> tierBox.setItems(result));
        });
    }

    private void fetchDetailBrands(Long id) {
        asyncRestClientOrganizationService.getDetailBrandAsync(result -> {
            ui.access(()-> brandBox.setValue(result));
        }, id);
    }

    private void fetchDetailTierByBrand(Integer id) {
        asyncRestClientOrganizationService.getAllTierByBrandAsync(result -> {
            ui.access(()-> tierBox.setItems(result));

            for (TierDto tierDto : result) {
                if (tierDto.getId().longValue() == this.storeDto.getTierId().longValue()) {
                    ui.access(()-> tierBox.setValue(tierDto));
                    break;
                }
            }
        }, id);
    }

    private HorizontalLayout createButtonsLayout() {

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        saveButton.addClickShortcut(Key.ENTER);
        updateButton.addClickShortcut(Key.ENTER);

        closeButton.addClickShortcut(Key.ESCAPE);

//        updateButton.addClickListener(new BrandUpdateEventListener(this, restClientService));

        saveButton.addClickListener(
                new StoreSaveEventListener(this, restClientOrganizationService));
        closeButton.addClickListener(buttonClickEvent -> this.setVisible(false));

        return new HorizontalLayout(saveButton, updateButton, updateButton, deleteButton, closeButton);
    }

    private void showErrorDialog(String message) {
        DialogClosing dialog = new DialogClosing(message);
        ui.access(()-> {
            add(dialog);
            dialog.open();
        });
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
