package com.harmoni.menu.dashboard.layout.organization.chain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.event.chain.ChainSaveEventListener;
import com.harmoni.menu.dashboard.event.chain.ChainUpdateEventListener;
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
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.shared.Registration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;
import java.util.Objects;

/**
 * Vaadin form for editing a {@link ChainDto} inside a {@link Dialog}. Renders a
 * brand {@link ComboBox} and a chain-name field bound with a
 * {@link BeanValidationBinder}, wires the save/update buttons to
 * {@link ChainSaveEventListener} / {@link ChainUpdateEventListener}, and
 * closes the dialog on a successful BROADCAST insert or update.
 */
@RequiredArgsConstructor
@Slf4j
public class ChainForm extends FormLayout {

    Registration broadcasterRegistration;

    @Getter
    BeanValidationBinder<ChainDto> binder = new BeanValidationBinder<>(ChainDto.class);

    @Getter
    ComboBox<BrandDto> brandComboBox = new ComboBox<>("Brand");

    @Getter
    TextField chainNameField = new TextField("Chain name");

    Button saveButton = new Button("Save");
    Button closeButton = new Button("Cancel");
    Button updateButton = UiUtil.updateButton();

    private final RestClientOrganizationService restClientOrganizationService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final Dialog dialog;
    private final FormAction formAction;

    @Getter
    UI ui;

    @Getter
    private final transient ChainDto chainDto;

    private final List<BrandDto> brands;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(this::receiptBroadcast);
        addValidation();
        brandComboBox.setItemLabelGenerator(BrandDto::getName);

        add(brandComboBox);
        add(chainNameField);
        binder.bindInstanceFields(this);
        restructureButton(formAction);

        if (ObjectUtils.isNotEmpty(brands)) {
            brandComboBox.setItems(brands);
        }

        if (formAction == FormAction.EDIT && ObjectUtils.isNotEmpty(chainDto)) {
            binder.readBean(chainDto);
            if (ObjectUtils.isNotEmpty(brands)) {
                restoreSelectedBrand(brands);
            }
        }

        addFooterButtons();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    private void showNotification() {
        ui.access(() -> UiUtil.success("Chain created.."));
    }

    /**
     * Closes the wrapped dialog on the UI thread.
     */
    public void close() {
        ui.access(() -> dialog.close());
    }

    private void addValidation() {
        binder.forField(brandComboBox)
                .withValidator(value -> value != null && value.getId() > 0,
                        "Brand must be not empty")
                .bind(ChainDto::getBrandDto, ChainDto::setBrandDto);
        binder.forField(chainNameField)
                .withValidator(value -> value.length() > 2,
                        "Name must contain at least three characters")
                .bind(ChainDto::getName, ChainDto::setName);
    }

    private void addFooterButtons() {
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        saveButton.addClickShortcut(Key.ENTER);
        updateButton.addClickShortcut(Key.ENTER);

        closeButton.addClickShortcut(Key.ESCAPE);

        updateButton.addClickListener(new ChainUpdateEventListener(this, restClientOrganizationService));
        saveButton.addClickListener(new ChainSaveEventListener(this, restClientOrganizationService));
        closeButton.addClickListener(event -> close());

        dialog.getFooter().add(saveButton, updateButton, closeButton);
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
            updateButton.setVisible(false);
            closeButton.setVisible(true);
        } else if (formAction == FormAction.EDIT) {
            saveButton.setVisible(false);
            updateButton.setVisible(true);
            closeButton.setVisible(true);
        }
    }

    private void restoreSelectedBrand(List<BrandDto> brands) {
        if (formAction != FormAction.EDIT || ObjectUtils.isEmpty(chainDto)) {
            return;
        }
        Integer brandId = resolveBrandId();
        if (ObjectUtils.isEmpty(brandId)) {
            return;
        }
        brands.stream()
                .filter(brand -> brandId.equals(brand.getId()))
                .findFirst()
                .ifPresent(brandComboBox::setValue);
    }

    private Integer resolveBrandId() {
        if (chainDto.getBrandId() != null && chainDto.getBrandId() > 0) {
            return chainDto.getBrandId();
        }
        return ObjectUtils.isNotEmpty(chainDto.getBrandDto()) && chainDto.getBrandDto().getId() != null
                ? chainDto.getBrandDto().getId() : null;
    }

    private void receiptBroadcast(String message) {
        try {
            BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
            if (ObjectUtils.isNotEmpty(broadcastMessage) && ObjectUtils.isNotEmpty(broadcastMessage.getType())
                    && (broadcastMessage.getType().equals(BroadcastMessage.CHAIN_INSERT_SUCCESS) ||
                    broadcastMessage.getType().equals(BroadcastMessage.CHAIN_SUCCESS_UPDATED))) {
                showNotification();
                close();
            }
        } catch (JsonProcessingException e) {
            log.error("Broadcast Handler Error", e);
        }
    }
}