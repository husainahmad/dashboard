package com.harmoni.menu.dashboard.layout.menu.product;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.dto.ProductDto;
import com.harmoni.menu.dashboard.layout.component.DialogClosing;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.rest.data.RestAPIResponse;
import com.harmoni.menu.dashboard.util.ObjectUtil;
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
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Objects;

@Route("product-form")
@Slf4j
public class ProductForm extends FormLayout  {
    Registration broadcasterRegistration;
    @Getter
    BeanValidationBinder<ProductDto> binder = new BeanValidationBinder<>(ProductDto.class);
    @Getter
    TextField productNameField = new TextField("Product name");

    @Getter
    ComboBox<CategoryDto> categoryBox = new ComboBox<>("Category");
    private final Button saveButton = new Button("Save");
    private final Button  deleteButton = new Button("Delete");
    private final Button  closeButton = new Button("Cancel");
    private final Button  updateButton = new Button("Update");

    @Getter
    private UI ui;
    @Getter
    private transient ProductDto productDto;
    private final AsyncRestClientMenuService asyncRestClientMenuService;

    public ProductForm(@Autowired AsyncRestClientMenuService asyncRestClientMenuService) {
        this.asyncRestClientMenuService = asyncRestClientMenuService;

        addValidation();

        categoryBox.setItemLabelGenerator(CategoryDto::getName);

        add(categoryBox);
        add(productNameField);

        add(createButtonsLayout());
        binder.bindInstanceFields(this);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(this::acceptNotification);

        fetchCategories();
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

        categoryBox.addValueChangeListener(changeEvent -> binder.validate());
        binder.forField(categoryBox)
                        .withValidator(value -> value.getId() > 0, "Category not allow to be empty"
                        ).bind(ProductDto::getCategoryDto, ProductDto::setCategoryDto);
        productNameField.addValueChangeListener(
                (HasValue.ValueChangeListener<AbstractField
                        .ComponentValueChangeEvent<TextField, String>>) changeEvent -> binder.validate());

        binder.forField(productNameField)
                .withValidator(value -> value.length() > 2,
                        "Name must contain at least three characters")
                .bind(ProductDto::getName, ProductDto::setName);

    }

    void setProductDto(ProductDto productDto) {
        this.productDto = productDto;

        if (!ObjectUtils.isEmpty(productDto) && !ObjectUtils.isEmpty(productDto.getCategoryId())) {
            fetchDetailCategory(productDto.getCategoryId().longValue());
        }

        binder.readBean(this.productDto);
    }

    private void fetchCategories() {
        asyncRestClientMenuService.getAllCategoryAsync(result ->
                ui.access(()-> categoryBox.setItems(result)), 1);
    }

    private void fetchDetailCategory(Long id) {
        asyncRestClientMenuService.getDetailCategoryAsync(result ->
                ui.access(()-> categoryBox.setValue(result)), id);
    }

    private HorizontalLayout createButtonsLayout() {

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        saveButton.addClickShortcut(Key.ENTER);
        updateButton.addClickShortcut(Key.ENTER);

        closeButton.addClickShortcut(Key.ESCAPE);

        closeButton.addClickListener(buttonClickEvent -> this.setVisible(false));

        return new HorizontalLayout(saveButton, updateButton, updateButton, deleteButton, closeButton);
    }

    public void restructureButton(FormAction formAction) {
        if (Objects.requireNonNull(formAction) == FormAction.CREATE) {
            saveButton.setVisible(true);
            updateButton.setVisible(false);
            deleteButton.setVisible(false);
            closeButton.setVisible(true);
        } else if (formAction == FormAction.EDIT) {
            saveButton.setVisible(false);
            updateButton.setVisible(true);
            deleteButton.setVisible(true);
            closeButton.setVisible(true);
        }
    }

    private void acceptNotification(String message) {
        try {
            BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
            if (ObjectUtils.isNotEmpty(broadcastMessage) && ObjectUtils.isNotEmpty(broadcastMessage.getType())) {
                showBroadcastMessage(broadcastMessage);
            }
        } catch (JsonProcessingException e) {
            log.error("Broadcast Handler Error", e);
        }
    }

    private void showBroadcastMessage(BroadcastMessage broadcastMessage) {
        RestAPIResponse restAPIResponse = (RestAPIResponse) broadcastMessage.getData();
        if (broadcastMessage.getType().equals(BroadcastMessage.PRODUCT_INSERT_SUCCESS)) {
            showNotification("Category created..");
            hideForm();
        }
        if (broadcastMessage.getType().equals(BroadcastMessage.BAD_REQUEST_FAILED)) {
            showErrorDialog(restAPIResponse.getData().toString());
        }
        if (broadcastMessage.getType().equals(BroadcastMessage.PROCESS_FAILED)) {
            showErrorDialog(restAPIResponse.getData().toString());
        }

    }
}
