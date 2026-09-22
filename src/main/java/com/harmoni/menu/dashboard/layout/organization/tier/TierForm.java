package com.harmoni.menu.dashboard.layout.organization.tier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ServiceDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.event.tier.TierSaveEventListener;
import com.harmoni.menu.dashboard.event.tier.TierUpdateEventListener;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
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
 * selection, hosted in a {@link TabManager} tab. Shared by the menu, price and
 * service tier forms; binds the brand and name fields via a
 * {@link BeanValidationBinder}, wires the save/update buttons to
 * {@link TierSaveEventListener} / {@link TierUpdateEventListener}, and closes
 * the tab on a successful TIER BROADCAST message.
 */
@Slf4j
public class TierForm extends FormLayout {

    @Getter
    private RestClientOrganizationService restClientOrganizationService;
    @Getter
    private AsyncRestClientOrganizationService asyncRestClientOrganizationService;

    private final TabManager tabManager;
    private final Tab currentTab;
    private final FormAction formAction;

    private Registration broadcasterRegistration;
    @Getter
    private final BeanValidationBinder<TierDto> binder = new BeanValidationBinder<>(TierDto.class);
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

    private transient List<BrandDto> brands;

    /**
     * Creates a tier form hosted in a tab.
     *
     * @param restClientOrganizationService     the synchronous REST client
     * @param asyncRestClientOrganizationService the async REST client
     * @param tabManager                        the manager of the hosting tab sheet
     * @param currentTab                        the tab showing this form
     * @param formAction                        whether the form creates or edits
     * @param tierDto                           the tier to bind, or a new one to create
     * @param brands                            the brands to offer in the combo box
     */
    public TierForm(RestClientOrganizationService restClientOrganizationService, AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                    TabManager tabManager, Tab currentTab, FormAction formAction, TierDto tierDto, List<BrandDto> brands) {
        this.restClientOrganizationService = restClientOrganizationService;
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
        this.tabManager = tabManager;
        this.currentTab = currentTab;
        this.formAction = formAction;
        this.tierDto = tierDto;
        this.brands = brands;
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
                        close();
                    }

            } catch (JsonProcessingException e) {
                log.error("Broadcast Handler Error", e);
            }
        });
        restructureButton(formAction);
        populateBrands(brands);
    }

    /**
     * Shows a success notification with the given text on the UI thread.
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

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    /**
     * Registers the brand and tier-name validators on the binder. Both
     * validators are null-safe so opening a brand-new form never paints a
     * spurious error.
     */
    public void addValidation() {
        getBinder().forField(brandBox)
                .withValidator(value -> ObjectUtils.isNotEmpty(value) && value.getId() > 0,
                        "Brand not allow to be empty"
                ).bind(TierDto::getBrandDto, TierDto::setBrandDto);

        getBinder().forField(tierNameField)
                .withValidator(value -> ObjectUtils.isNotEmpty(value) && value.length() > 2,
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

        closeButton.addClickListener(event -> close());

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
     * Pre-fills the form from the current tier, populating the brand combo box
     * and selecting the tier's brand by id. Values are set directly instead of
     * via {@code binder.readBean} so that opening a form never runs validation
     * and can never paint a spurious "Brand not allow to be empty" error before
     * the user enters anything.
     *
     * @param brands the brands to offer in the combo box
     */
    private void populateBrands(List<BrandDto> brands) {
        brandBox.setItems(brands);
        if (ObjectUtils.isEmpty(tierDto)) {
            return;
        }
        if (ObjectUtils.isNotEmpty(tierDto.getName())) {
            tierNameField.setValue(tierDto.getName());
        }
        if (ObjectUtils.isNotEmpty(brands) && ObjectUtils.isNotEmpty(tierDto.getBrandId())) {
            brands.stream()
                    .filter(brand -> brand.getId().equals(tierDto.getBrandId()))
                    .findFirst()
                    .ifPresent(brand -> {
                        tierDto.setBrandDto(brand);
                        brandBox.setValue(brand);
                    });
        }
    }

}