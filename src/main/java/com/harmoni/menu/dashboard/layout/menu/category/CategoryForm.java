package com.harmoni.menu.dashboard.layout.menu.category;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.event.category.CategorySaveEventListener;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
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
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.shared.Registration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Dialog form for creating or editing a {@link CategoryDto}.
 *
 * <p>
 * Backed by a {@link BeanValidationBinder} over the name and description fields
 * with an optional {@link BrandDto} selection; persistence is handled by
 * {@link CategorySaveEventListener} against the injected {@code restClientMenuService}.
 * The dialog closes itself when the category-insert or category-updated broadcast
 * arrives via {@link Broadcaster}.
 * </p>
 */
@RequiredArgsConstructor
@Slf4j
public class CategoryForm extends FormLayout {

    Registration broadcasterRegistration;

    /** Binder driving validation of the category fields. */
    @Getter
    BeanValidationBinder<CategoryDto> binder = new BeanValidationBinder<>(CategoryDto.class);

    /** Category name input. */
    @Getter
    TextField categoryNameField = new TextField("Category name");

    /** Free-text category description input. */
    @Getter
    TextArea categoryDescArea = new TextArea("Description");

    /** Optional brand selection, populated from the injected brand list. */
    @Getter
    ComboBox<BrandDto> brandBox = new ComboBox<>("Brand");

    Button saveButton = new Button("Save");
    Button closeButton = new Button("Cancel");
    Button updateButton = new Button("Update");

    /** Captured on attach; used to marshal notifications and dialog close onto the UI thread. */
    @Getter
    UI ui;

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientMenuService restClientMenuService;
    private final Dialog dialog;
    private final FormAction formAction;

    /** The category being edited, or {@code null} when creating a new one. */
    @Getter
    private final transient CategoryDto categoryDto;

    private final List<BrandDto> brands;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(message -> {
            try {
                BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
                if (ObjectUtils.isNotEmpty(broadcastMessage) && ObjectUtils.isNotEmpty(broadcastMessage.getType())
                        && (broadcastMessage.getType().equals(BroadcastMessage.CATEGORY_INSERT_SUCCESS) ||
                        broadcastMessage.getType().equals(BroadcastMessage.CATEGORY_UPDATED_SUCCESS))) {
                        showNotification("Category created..");
                        close();
                    }

            } catch (JsonProcessingException e) {
                log.error("Broadcast Handler Error", e);
            }
        });
        addValidation();

        brandBox.setItemLabelGenerator(BrandDto::getName);

        add(brandBox);
        add(categoryNameField);
        add(categoryDescArea);

        binder.bindInstanceFields(this);
        restructureButton(formAction);

        if (ObjectUtils.isNotEmpty(brands)) {
            brandBox.setItems(brands);
        }

        if (formAction == FormAction.EDIT && ObjectUtils.isNotEmpty(categoryDto)) {
            binder.readBean(categoryDto);
            if (ObjectUtils.isNotEmpty(brands)) {
                restoreSelectedBrand(brands);
            }
        }

        addFooterButtons();
    }

    /**
     * Shows a success notification on the UI thread.
     *
     * @param text the message to display
     */
    public void showNotification(String text) {
        ui.access(() -> UiUtil.success(text));
    }

    /**
     * Closes the hosting dialog on the UI thread.
     */
    public void close() {
        ui.access(() -> dialog.close());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    private void addValidation() {

        binder.forField(categoryNameField)
                .withValidator(value -> value != null && value.length() > 2,
                        "Name must contain at least three characters")
                .bind(CategoryDto::getName, CategoryDto::setName);

        binder.forField(categoryDescArea)
                .withValidator(value -> value != null && value.length() > 2,
                        "Description must contain at least three characters")
                .bind(CategoryDto::getDescription, CategoryDto::setDescription);
    }

    private void restoreSelectedBrand(List<BrandDto> brands) {
        if (formAction != FormAction.EDIT || ObjectUtils.isEmpty(categoryDto)) {
            return;
        }
        Integer brandId = resolveBrandId();
        if (ObjectUtils.isEmpty(brandId)) {
            return;
        }
        brands.stream()
                .filter(brand -> brandId.equals(brand.getId()))
                .findFirst()
                .ifPresent(brandBox::setValue);
    }

    private Integer resolveBrandId() {
        if (categoryDto.getBrandId() != null && categoryDto.getBrandId() > 0) {
            return categoryDto.getBrandId();
        }
        return Optional.ofNullable(categoryDto.getBrandDto())
                .map(BrandDto::getId)
                .orElse(null);
    }

    private void addFooterButtons() {

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        saveButton.addClickShortcut(Key.ENTER);
        updateButton.addClickShortcut(Key.ENTER);

        closeButton.addClickShortcut(Key.ESCAPE);

        saveButton.addClickListener(
                new CategorySaveEventListener(this, restClientMenuService));

        closeButton.addClickListener(event -> close());

        dialog.getFooter().add(saveButton, updateButton, closeButton);
    }

    /**
     * Toggles the Save / Update buttons for the given action; Cancel is always visible.
     *
     * @param formAction the action driving which buttons are shown
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
}