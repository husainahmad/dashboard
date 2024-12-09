package com.harmoni.menu.dashboard.layout.organization.tier.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ServiceDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.event.tier.*;
import com.harmoni.menu.dashboard.layout.component.DialogClosing;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Route("tier-service-form")
@Slf4j
public class TierServiceForm extends FormLayout  {
    private Registration broadcasterRegistration;
    @Getter
    private BeanValidationBinder<TierDto> binder = new BeanValidationBinder<>(TierDto.class);
    @Getter
    private TextField tierNameField = new TextField("Tier service name");
    @Getter
    @Setter
    private ComboBox<BrandDto> brandBox = new ComboBox<>("Brand");
    @Getter
    private MultiSelectComboBox<ServiceDto> serviceBox = new MultiSelectComboBox<>("Service");

    CheckboxGroup<String> checkboxGroup = new CheckboxGroup<>();
    private final Button saveButton = new Button("Save");
    private final Button  closeButton = new Button("Cancel");
    private final RestClientOrganizationService restClientOrganizationService;
    @Getter
    private UI ui;
    @Getter
    private transient TierDto tierDto;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    @Getter
    private List<String> subNames;
    @Setter
    @Getter
    private List<BrandDto> brandDtos;

    public TierServiceForm(@Autowired AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                           @Autowired RestClientOrganizationService restClientOrganizationService) {
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
        this.restClientOrganizationService = restClientOrganizationService;

        brandBox.setItemLabelGenerator(BrandDto::getName);
        add(brandBox);

        add(tierNameField);

        add(createButtonsLayout());

//        fetchBrands();
        fetchServices();

        setServiceBoxChangeListener();
        setCheckBoxSubServiceChangeListener();
        addValidation();
    }

    private void setServiceBoxChangeListener() {
        serviceBox.addValueChangeListener(event -> {
            event.getValue().stream().toList().forEach(serviceDto -> {
                checkboxGroup.setLabel("Sub Service");
                List<String> subServices = new ArrayList<>();
                serviceDto.getSubServices().forEach(subServiceDto -> subServices.add(subServiceDto.getName()));
                checkboxGroup.setItems(subServices.toArray(new String[0]));
            });

        });
    }

    private void setCheckBoxSubServiceChangeListener() {
        checkboxGroup.addValueChangeListener(changeEvent -> {
            log.debug("{}", changeEvent);
            subNames = changeEvent.getValue().stream().toList();
        });
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(message -> {
            try {
                BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
                if (ObjectUtils.isNotEmpty(broadcastMessage) && ObjectUtils.isNotEmpty(broadcastMessage.getType())) {
                    if (broadcastMessage.getType().equals(BroadcastMessage.TIER_INSERT_SUCCESS)) {
                        showNotification("Tier created..");
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
        ui.access(() -> brandBox.setItems(brandDtos));
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
                        ).bind(TierDto::getBrandDto, TierDto::setBrandDto);

        tierNameField.addValueChangeListener(
                (HasValue.ValueChangeListener<AbstractField
                        .ComponentValueChangeEvent<TextField, String>>) changeEvent -> binder.validate());

        binder.forField(tierNameField)
                .withValidator(value -> value.length() > 2,
                        "Name must contain at least three characters")
                .bind(TierDto::getName, TierDto::setName);

        binder.bindInstanceFields(this);
    }

    void setTierDto(TierDto tierDto) {
        this.tierDto = tierDto;

        if (!ObjectUtils.isEmpty(tierDto) && !ObjectUtils.isEmpty(tierDto.getBrandId())) {
            fetchDetailBrands(tierDto.getBrandId().longValue());
        }

        binder.readBean(this.tierDto);
    }

    private void fetchServices() {
        asyncRestClientOrganizationService.getAllServicesAsync(result -> ui.access(()-> serviceBox.setItems(result)));
    }

    private void fetchDetailBrands(Long id) {
        asyncRestClientOrganizationService.getDetailBrandAsync(result -> {
            ui.access(()-> brandBox.setValue(result));
        }, id);
    }

    private HorizontalLayout createButtonsLayout() {

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        saveButton.addClickShortcut(Key.ENTER);

        closeButton.addClickShortcut(Key.ESCAPE);
        saveButton.addClickListener(
                new TierServiceSaveEventListener(this, restClientOrganizationService));
//        updateButton.addClickListener(new TierServiceUpdateEventListener(this, restClientOrganizationService));

        closeButton.addClickListener(buttonClickEvent -> this.setVisible(false));

        return new HorizontalLayout(saveButton, closeButton);
    }

    public void restructureButton(FormAction formAction) {
        if (Objects.requireNonNull(formAction) == FormAction.CREATE) {
            saveButton.setVisible(true);
            closeButton.setVisible(true);
        } else if (formAction == FormAction.EDIT) {
            saveButton.setVisible(false);
            closeButton.setVisible(true);
        }
    }
}
