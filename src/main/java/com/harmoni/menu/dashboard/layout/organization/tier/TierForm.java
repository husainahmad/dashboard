package com.harmoni.menu.dashboard.layout.organization.tier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ServiceDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.event.tier.TierSaveEventListener;
import com.harmoni.menu.dashboard.event.tier.TierUpdateEventListener;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.shared.Registration;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;
import java.util.Objects;

/**
 * Base Vaadin form for editing a {@link TierDto} with brand and service
 * selection. Shared by the menu, price and service tier forms; binds the brand
 * and name fields via a {@link BeanValidationBinder}, wires the save/update
 * buttons to {@link TierSaveEventListener} / {@link TierUpdateEventListener},
 * and hides itself on a successful TIER BROADCAST message.
 */
@Slf4j
public class TierForm extends FormLayout {

    @Getter
    private RestClientOrganizationService restClientOrganizationService;
    @Getter
    private AsyncRestClientOrganizationService asyncRestClientOrganizationService;

    private Registration broadcasterRegistration;
    @Getter
    private BeanValidationBinder<TierDto> binder = new BeanValidationBinder<>(TierDto.class);
    /**
     * Input for the tier's display name.
     */
    @Getter
    public TextField tierNameField = new TextField("Tier name");
    /**
     * Selects the brand the tier belongs to.
     */
    @Getter
    @Setter
    public ComboBox<BrandDto> brandBox = new ComboBox<>("Brand");
    @Getter
    MultiSelectComboBox<ServiceDto> serviceBox = new MultiSelectComboBox<>("Service");

    protected final Button saveButton = new Button("Save");
    protected final Button closeButton = new Button("Cancel");
    protected final Button updateButton = new Button("Update");

    @Getter
    private UI ui;
    @Getter
    private transient TierDto tierDto;
    @Setter
    @Getter
    private transient List<BrandDto> brandDtos;

    /**
     * Creates a tier form with the given REST clients.
     *
     * @param restClientOrganizationService     the synchronous REST client
     * @param asyncRestClientOrganizationService the async REST client
     */
    public TierForm(RestClientOrganizationService restClientOrganizationService, AsyncRestClientOrganizationService asyncRestClientOrganizationService) {
        this.restClientOrganizationService = restClientOrganizationService;
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(message -> {
            try {
                BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
                if (ObjectUtils.isNotEmpty(broadcastMessage) && ObjectUtils.isNotEmpty(broadcastMessage.getType())
                        && (broadcastMessage.getType().equals(BroadcastMessage.TIER_INSERT_SUCCESS) ||
                            broadcastMessage.getType().equals(BroadcastMessage.TIER_UPDATED_SUCCESS) ||
                        broadcastMessage.getType().equals(BroadcastMessage.TIER_DELETED_SUCCESS))) {
                        showNotification("Tier updated..");
                        hideForm();
                    }

            } catch (JsonProcessingException e) {
                log.error("Broadcast Handler Error", e);
            }
        });
        if (ObjectUtils.isNotEmpty(brandDtos)) {
            ui.access(() -> brandBox.setItems(brandDtos));
        }
    }

    /**
     * Shows a success notification with the given text on the UI thread.
     *
     * @param text the message to display
     */
    public void showNotification(String text) {
        ui.access(() -> UiUtil.success(text));
    }

    /**
     * Hides the form on the UI thread.
     */
    public void hideForm() {
        ui.access(()-> this.setVisible(false));
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    protected void setTierDtoAndBind(TierDto tierDto) {
        this.tierDto = (tierDto);
        getBinder().readBean(this.getTierDto());
    }

    /**
     * Registers the brand and tier-name validators on the binder.
     */
    public void addValidation() {
        getBinder().forField(brandBox)
                .withValidator(value -> value.getId() > 0, "Brand not allow to be empty"
                ).bind(TierDto::getBrandDto, TierDto::setBrandDto);

        getBinder().forField(tierNameField)
                .withValidator(value -> value.length() > 2,
                        "Name must contain at least three characters")
                .bind(TierDto::getName, TierDto::setName);

    }

    /**
     * Shows or hides the save, update and cancel buttons depending on the form
     * action (only save for create, only update for edit; cancel always
     * visible).
     *
     * @param formAction the mode the form was opened in
     */
    public void restructureButton(FormAction formAction) {
        if (Objects.requireNonNull(formAction) == FormAction.CREATE) {
            saveButton.setVisible(true);
            closeButton.setVisible(true);
            updateButton.setVisible(false);
        } else if (formAction == FormAction.EDIT) {
            saveButton.setVisible(false);
            closeButton.setVisible(true);
            updateButton.setVisible(true);
        }
    }

    protected HorizontalLayout createButtonsLayout(boolean enableUpdate, boolean enableSave) {

        HorizontalLayout horizontalLayout = new HorizontalLayout();

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        saveButton.addClickShortcut(Key.ENTER);
        updateButton.addClickShortcut(Key.ENTER);

        closeButton.addClickShortcut(Key.ESCAPE);
        saveButton.addClickListener(
                new TierSaveEventListener(this, this.getRestClientOrganizationService()));
        updateButton.addClickListener(new TierUpdateEventListener(this, this.getRestClientOrganizationService()));

        closeButton.addClickListener(event -> this.setVisible(false));

        if (enableSave) {
            horizontalLayout.add(saveButton);
        }
        if (enableUpdate) {
            horizontalLayout.add(updateButton);
        }

        horizontalLayout.add(closeButton);
        horizontalLayout.setPadding(true);

        return horizontalLayout;
    }

    /**
     * Loads all brands into the brand combo box asynchronously.
     */
    public void fetchBrands() {
        this.getAsyncRestClientOrganizationService().getAllBrandAsync(result ->
                ui.access(()-> brandBox.setItems(result)));
    }

    /**
     * Loads a single brand by id and selects it in the brand combo box.
     *
     * @param id the brand id to select
     */
    public void fetchDetailBrands(Long id) {
        this.getAsyncRestClientOrganizationService().getDetailBrandAsync(result ->
                getUi().access(()-> brandBox.setValue(result)), id);
    }

    /**
     * Binds the given tier into the form and restores the selected brand.
     *
     * @param tierDto the tier to display for editing
     */
    public void changeTierDto(TierDto tierDto) {
        this.setTierDtoAndBind(tierDto);

        if (!ObjectUtils.isEmpty(tierDto) && !ObjectUtils.isEmpty(tierDto.getBrandId())) {
            fetchDetailBrands(tierDto.getBrandId().longValue());
        }
    }

}
