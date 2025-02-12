package com.harmoni.menu.dashboard.layout.organization.user;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.dto.UserDto;
import com.harmoni.menu.dashboard.event.user.UserSaveEventListener;
import com.harmoni.menu.dashboard.event.user.UserUpdateEventListener;
import com.harmoni.menu.dashboard.layout.enums.RoleType;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.*;


@RequiredArgsConstructor
@Route("users-form")
@Slf4j
public class UserForm extends FormLayout  {
    Registration broadcasterRegistration;
    BeanValidationBinder<StoreDto> binder = new BeanValidationBinder<>(StoreDto.class);

    TextField userNameField = new TextField("User Name");
    EmailField userEmailField = new EmailField("Email");

    PasswordField userPassField = new PasswordField("Password");

    ComboBox<StoreDto> storeDtoComboBox = new ComboBox<>("Store");
    ComboBox<RoleType> authDtoComboBox = new ComboBox<>("Auth");

    Button saveButton = new Button("Save");
    Button closeButton = new Button("Cancel");
    Button updateButton = new Button("Update");

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final AccessService accessService;

    private final Tab userTab;
    private final FormAction formAction;
    private final transient UserDto userDto;

    UI ui;
    static final int TEMP_BRAND_ID = 1;
    private int totalRow = 1;

    private void renderLayout() {
        setSizeFull();
        storeDtoComboBox.setAllowCustomValue(false);
        storeDtoComboBox.setItemLabelGenerator(StoreDto::getName); // Display store name

        storeDtoComboBox.setDataProvider(
            DataProvider.fromFilteringCallbacks(
                    query -> fetchStores(query.getFilter().orElse(""), query.getOffset(), query.getLimit()).stream(),
                    _ -> countStores()
            ),
            filter -> filter
        );

        add(storeDtoComboBox);
        add(userNameField);
        add(userEmailField);
        add(userPassField);

        authDtoComboBox.setItems(getRoleTypes());

        add(authDtoComboBox);

        add(createButtonsLayout());
        restructureAddOrEdit();

        addValidation();
        binder.bindInstanceFields(this);
        setResponsiveSteps(new ResponsiveStep("0", 1, ResponsiveStep.LabelsPosition.ASIDE));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(message -> {
            try {
                BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
                if (ObjectUtils.isNotEmpty(broadcastMessage) && ObjectUtils.isNotEmpty(broadcastMessage.getType())
                        && (broadcastMessage.getType().equals(BroadcastMessage.STORE_INSERT_SUCCESS) ||
                        broadcastMessage.getType().equals(BroadcastMessage.STORE_UPDATED_SUCCESS))) {
                        removeFromSheet();
                    }
            } catch (JsonProcessingException e) {
                log.error("Broadcast Handler Error", e);
            }
        });
        renderLayout();
    }

    public void removeFromSheet() {
        this.ui.access(() -> {
            if (!(this.getParent().orElseThrow() instanceof TabSheet tabSheet)) {
                return;
            }
            tabSheet.remove(userTab);
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    private void addValidation() {
        storeDtoComboBox.addValueChangeListener(_ -> binder.validate());
        userNameField.addValueChangeListener(
                (HasValue.ValueChangeListener<AbstractField.ComponentValueChangeEvent<TextField, String>>)
                        _ -> binder.validate());
        binder.forField(userNameField)
                .withValidator(value -> value.length()>2,
                        "Name must contain at least three characters")
                .bind(StoreDto::getName, StoreDto::setName);
    }

    private HorizontalLayout createButtonsLayout() {

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        saveButton.addClickShortcut(Key.ENTER);
        updateButton.addClickShortcut(Key.ENTER);

        closeButton.addClickShortcut(Key.ESCAPE);
        closeButton.addClickListener(_ -> removeFromSheet());

        saveButton.addClickListener(new UserSaveEventListener(this, restClientOrganizationService));
        updateButton.addClickListener(new UserUpdateEventListener(this, restClientOrganizationService));
        HorizontalLayout horizontalLayout = new HorizontalLayout(saveButton, updateButton, updateButton, closeButton);
        horizontalLayout.setPadding(true);
        return horizontalLayout;
    }

    private void restructureAddOrEdit() {
        if (ObjectUtils.isNotEmpty(formAction) && formAction.equals(FormAction.EDIT) && ObjectUtils.isNotEmpty(userDto)) {

            userNameField.setValue(userDto.getUsername());
            userNameField.setEnabled(false);
            userEmailField.setEnabled(false);

            storeDtoComboBox.setEnabled(false);

            authDtoComboBox.getListDataView().getItems()
                    .filter(roleType -> Objects.equals(roleType.getId(), userDto.getAuthId()))
                    .findFirst()
                    .ifPresent(authDtoComboBox::setValue);

        }
        restructureButton();
    }

    public void restructureButton() {
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

    private List<StoreDto> fetchStores(String filter, int offset, int limit) {
        int page = (offset / limit) + 1;
        List<StoreDto> storeDtos = new ArrayList<>();

        try {
            restClientOrganizationService.getStore(accessService.getUserDetail().getStoreDto().getChainId(), page, limit, filter)
                .map(response -> {
                    if (ObjectUtils.isEmpty(response.getData())) {
                        throw new NullPointerException();
                    }
                    Map<String, Object> objectMap = ObjectUtil.convertObjectToObject(response.getData(), new TypeReference<>() {});
                    totalRow = Integer.parseInt(objectMap.get("page") == null ? "0" :objectMap.get("page").toString());

                    if (objectMap.get("data") instanceof List<?> dataList && !dataList.isEmpty()) {
                        dataList.forEach(object -> {
                            StoreDto storeDto = ObjectUtil.convertValueToObject(object, StoreDto.class);
                            storeDtos.add(storeDto);
                        });
                    }
                    return storeDtos;
                })
                .block();
        } catch (Exception e) {
            totalRow = 0;
            return Collections.emptyList();
        }
        return storeDtos;
    }

    private int countStores() {
        return totalRow;
    }

    private List<RoleType> getRoleTypes() {
        return List.of(RoleType.ADMIN, RoleType.MANAGER, RoleType.USER);
    }

    public UserDto getUserDto() {
        userDto.setUsername(userNameField.getValue());
        userDto.setEmail(userEmailField.getValue());
        userDto.setPassword(userPassField.getValue());

        if (storeDtoComboBox.getValue() != null) {
            userDto.setStoreId(storeDtoComboBox.getValue().getId());
        }

        if (authDtoComboBox.getValue() != null) {
            userDto.setAuthId(authDtoComboBox.getValue().getId());
        }
        return userDto;
    }

}
