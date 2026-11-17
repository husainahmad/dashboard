package com.harmoni.menu.dashboard.layout.menu.category;

import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import java.util.List;
import java.util.Objects;

/**
 * Modal dialog for creating and editing a menu category. The brand defaults to
 * the one selected in the page toolbar.
 */
public class CategoryDialog extends Dialog {

    private final RestClientMenuService restClientMenuService;
    private final List<BrandDto> brands;
    private final BrandDto defaultBrand;
    private final CategoryDto editingCategory;
    private final Runnable onSaved;

    private final TextField nameField = new TextField(Messages.get("label.field.categoryName"));
    private final TextArea descriptionField = new TextArea(Messages.get(Messages.Keys.LABEL_DESCRIPTION));
    private final ComboBox<BrandDto> brandBox = new ComboBox<>(Messages.get(Messages.Keys.LABEL_BRAND));

    public CategoryDialog(RestClientMenuService restClientMenuService, List<BrandDto> brands,
                          BrandDto defaultBrand, CategoryDto editingCategory, Runnable onSaved) {
        this.restClientMenuService = restClientMenuService;
        this.brands = brands;
        this.defaultBrand = defaultBrand;
        this.editingCategory = editingCategory;
        this.onSaved = onSaved;

        setHeaderTitle(Messages.get(editingCategory == null
                ? "category.dialog.title.create" : "category.dialog.title.edit"));
        setWidth("520px");

        configureFields();
        add(form());

        Button cancel = new Button(Messages.get(Messages.Keys.ACTION_CANCEL), event -> close());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button save = new Button(Messages.get(editingCategory == null
                ? "category.dialog.create" : "category.dialog.update"), event -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(cancel, save);

        if (editingCategory != null) {
            applyEdit(editingCategory);
        }
    }

    private void configureFields() {
        nameField.setRequired(true);
        nameField.setWidthFull();
        nameField.getElement().setAttribute("autocomplete", "off");

        descriptionField.setRequired(true);
        descriptionField.setWidthFull();
        descriptionField.setMinHeight("96px");

        brandBox.setItems(brands);
        brandBox.setItemLabelGenerator(BrandDto::getName);
        brandBox.setWidthFull();
        brandBox.setEnabled(brands != null && brands.size() > 1);
        brandBox.setValue(defaultBrand != null ? defaultBrand
                : brands == null || brands.isEmpty() ? null : brands.getFirst());
    }

    private FormLayout form() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.add(nameField);
        form.add(descriptionField);
        form.add(brandBox);
        return form;
    }

    private void applyEdit(CategoryDto category) {
        nameField.setValue(category.getName() == null ? "" : category.getName());
        descriptionField.setValue(category.getDescription() == null ? "" : category.getDescription());

        Integer brandId = category.getBrandId() != null ? category.getBrandId()
                : category.getBrandDto() == null ? null : category.getBrandDto().getId();
        brands.stream()
                .filter(brand -> Objects.equals(brand.getId(), brandId))
                .findFirst()
                .ifPresent(brandBox::setValue);
    }

    private void save() {
        if (!valid()) {
            return;
        }
        CategoryDto dto = editingCategory == null ? new CategoryDto() : copyOf(editingCategory);
        dto.setName(nameField.getValue().trim());
        dto.setDescription(descriptionField.getValue().trim());
        dto.setBrandId(brandBox.getValue() == null ? null : brandBox.getValue().getId());

        if (editingCategory == null) {
            restClientMenuService.createCategory(dto).subscribe(
                    response -> success("category.created"),
                    error -> failure("category.saveFailed"));
            return;
        }
        restClientMenuService.updateCategory(dto).subscribe(
                response -> success("category.updated"),
                error -> failure("category.saveFailed"));
    }

    private boolean valid() {
        boolean valid = true;
        if (nameField.isEmpty() || nameField.getValue().trim().length() < 3) {
            nameField.setInvalid(true);
            nameField.setErrorMessage(Messages.get(Messages.Keys.VALIDATION_NAME_MIN_LENGTH));
            valid = false;
        }
        if (descriptionField.isEmpty() || descriptionField.getValue().trim().length() < 3) {
            descriptionField.setInvalid(true);
            descriptionField.setErrorMessage(Messages.get("validation.description.minLength"));
            valid = false;
        }
        if (brandBox.getValue() == null) {
            brandBox.setInvalid(true);
            brandBox.setErrorMessage(Messages.get(Messages.Keys.VALIDATION_BRAND_REQUIRED));
            valid = false;
        }
        return valid;
    }

    private CategoryDto copyOf(CategoryDto source) {
        CategoryDto dto = new CategoryDto();
        dto.setId(source.getId());
        dto.setName(source.getName());
        dto.setDescription(source.getDescription());
        dto.setBrandId(source.getBrandId());
        return dto;
    }

    private void success(String messageKey) {
        UiUtil.safeAccess(getUI().orElse(null), () -> {
            UiUtil.success(Messages.get(messageKey));
            close();
            if (onSaved != null) {
                onSaved.run();
            }
        });
    }

    private void failure(String messageKey) {
        UiUtil.safeAccess(getUI().orElse(null),
                () -> UiUtil.error(Messages.get(messageKey)));
    }
}
