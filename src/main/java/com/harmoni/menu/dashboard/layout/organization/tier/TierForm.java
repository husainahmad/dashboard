package com.harmoni.menu.dashboard.layout.organization.tier;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.event.tier.TierSaveEventListener;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;

@Route("tier-form")
public class TierForm extends FormLayout  {
    Registration broadcasterRegistration;
    @Getter
    BeanValidationBinder<TierDto> binder = new BeanValidationBinder<>(TierDto.class);
    @Getter
    TextField tierNameField = new TextField("Tier name");
    @Getter
    ComboBox<BrandDto> brandBox = new ComboBox<>("Brand");
    private final Button saveButton = new Button("Save");
    private final Button  deleteButton = new Button("Delete");
    private final Button  closeButton = new Button("Cancel");
    private final Button  updateButton = new Button("Update");
    private final RestClientOrganizationService restClientOrganizationService;
    @Getter
    private UI ui;
    @Getter
    private TierDto tierDto;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;

    public TierForm(@Autowired AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                    @Autowired RestClientOrganizationService restClientOrganizationService) {
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
        this.restClientOrganizationService = restClientOrganizationService;
        addValidation();

        brandBox.setItemLabelGenerator(BrandDto::getName);

        add(brandBox);

        add(tierNameField);

        add(createButtonsLayout());
        binder.bindInstanceFields(this);

        fetchBrands();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(message -> {
            if (message.equals(BroadcastMessage.TIER_INSERT_SUCCESS)) {
                showNotification("Tier created..");
                hideForm();
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

    }

    void setTierDto(TierDto tierDto) {
        this.tierDto = tierDto;

        if (!ObjectUtils.isEmpty(tierDto) && !ObjectUtils.isEmpty(tierDto.getBrandId())) {
            fetchDetailBrands(tierDto.getBrandId().longValue());
        }

        binder.readBean(this.tierDto);
    }

    private void fetchBrands() {
        asyncRestClientOrganizationService.getAllBrandAsync(result -> {
            ui.access(()->{
                brandBox.setItems(result);
            });
        });
    }

    private void fetchDetailBrands(Long id) {
        asyncRestClientOrganizationService.getDetailBrandAsync(result -> {
            ui.access(()->{
                brandBox.setValue(result);
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

//        updateButton.addClickListener(new BrandUpdateEventListener(this, restClientService));

        saveButton.addClickListener(
                new TierSaveEventListener(this, restClientOrganizationService));
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
