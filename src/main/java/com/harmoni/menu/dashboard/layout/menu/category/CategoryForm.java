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

@RequiredArgsConstructor
@Slf4j
public class CategoryForm extends FormLayout {

    Registration broadcasterRegistration;

    @Getter
    BeanValidationBinder<CategoryDto> binder = new BeanValidationBinder<>(CategoryDto.class);

    @Getter
    TextField categoryNameField = new TextField("Category name");

    @Getter
    TextArea categoryDescArea = new TextArea("Description");

    @Getter
    ComboBox<BrandDto> brandBox = new ComboBox<>("Brand");

    Button saveButton = new Button("Save");
    Button closeButton = new Button("Cancel");
    Button updateButton = new Button("Update");

    @Getter
    UI ui;

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientMenuService restClientMenuService;
    private final Dialog dialog;
    private final FormAction formAction;

    @Getter
    private final transient CategoryDto categoryDto;

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

        if (formAction == FormAction.EDIT && ObjectUtils.isNotEmpty(categoryDto)) {
            binder.readBean(categoryDto);
        }

        fetchBrands();
        addFooterButtons();
    }

    public void showNotification(String text) {
        ui.access(() -> UiUtil.success(text));
    }

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

    private void fetchBrands() {
        asyncRestClientOrganizationService.getAllBrandAsync(result ->
                ui.access(() -> {
                    brandBox.setItems(result);
                    restoreSelectedBrand(result);
                }));
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
                .ifPresentOrElse(brandBox::setValue,
                        () -> fetchDetailBrands(brandId.longValue()));
    }

    private Integer resolveBrandId() {
        if (categoryDto.getBrandId() != null) {
            return categoryDto.getBrandId();
        }
        return Optional.ofNullable(categoryDto.getBrandDto())
                .map(BrandDto::getId)
                .orElse(null);
    }

    private void fetchDetailBrands(Long id) {
        asyncRestClientOrganizationService.getDetailBrandAsync(result ->
                ui.access(() -> brandBox.setValue(result)), id);
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