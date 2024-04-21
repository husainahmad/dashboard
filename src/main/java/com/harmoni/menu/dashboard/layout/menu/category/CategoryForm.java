package com.harmoni.menu.dashboard.layout.menu.category;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.event.category.CategorySaveEventListener;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.rest.data.RestClientMenuService;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;

@Route("category-form")
public class CategoryForm extends FormLayout  {
    Registration broadcasterRegistration;
    @Getter
    BeanValidationBinder<CategoryDto> binder = new BeanValidationBinder<>(CategoryDto.class);
    @Getter
    TextField categoryNameField = new TextField("Category name");
    @Getter
    TextArea categoryDescArea = new TextArea("Description");
    @Getter
    ComboBox<BrandDto> brandBox = new ComboBox<>("Brand");
    private final Button saveButton = new Button("Save");
    private final Button  deleteButton = new Button("Delete");
    private final Button  closeButton = new Button("Cancel");
    private final Button  updateButton = new Button("Update");

    @Getter
    private UI ui;
    @Getter
    private CategoryDto categoryDto;
    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientMenuService restClientMenuService;

    public CategoryForm(@Autowired AsyncRestClientMenuService asyncRestClientMenuService,
                        @Autowired AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                        @Autowired RestClientMenuService restClientMenuService) {
        this.asyncRestClientMenuService = asyncRestClientMenuService;
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
        this.restClientMenuService = restClientMenuService;
        addValidation();

        brandBox.setItemLabelGenerator(BrandDto::getName);

        add(brandBox);
        add(categoryNameField);
        add(categoryDescArea);

        add(createButtonsLayout());
        binder.bindInstanceFields(this);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(message -> {
            if (message.equals(BroadcastMessage.CATEGORY_INSERT_SUCCESS)) {
                showNotification("Category created..");
                hideForm();
            }
        });
        fetchBrands();
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
                        ).bind(CategoryDto::getBrandDto, CategoryDto::setBrandDto);
        categoryNameField.addValueChangeListener(
                (HasValue.ValueChangeListener<AbstractField
                        .ComponentValueChangeEvent<TextField, String>>) changeEvent -> binder.validate());

        binder.forField(categoryNameField)
                .withValidator(value -> value.length() > 2,
                        "Name must contain at least three characters")
                .bind(CategoryDto::getName, CategoryDto::setName);

        categoryDescArea.addValueChangeListener(
                (HasValue.ValueChangeListener<AbstractField
                        .ComponentValueChangeEvent<TextArea, String>>) changeEvent -> binder.validate());
        binder.forField(categoryDescArea)
                .withValidator(value -> value.length() > 2,
                        "Name must contain at least three characters")
                .bind(CategoryDto::getDescription, CategoryDto::setDescription);

    }

    void setCategoryDto(CategoryDto categoryDto) {
        this.categoryDto = categoryDto;

        if (!ObjectUtils.isEmpty(categoryDto) && !ObjectUtils.isEmpty(categoryDto.getBrandId())) {
            fetchDetailBrands(categoryDto.getBrandId().longValue());
        }

        binder.readBean(this.categoryDto);
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
                new CategorySaveEventListener(this, restClientMenuService));
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
