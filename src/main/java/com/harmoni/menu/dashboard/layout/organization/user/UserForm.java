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
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.harmoni.menu.dashboard.util.Messages;
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
import lombok.Getter;
import org.apache.commons.lang3.ObjectUtils;

import java.util.*;
import com.harmoni.menu.dashboard.layout.util.Css;


/**
 * Vaadin form for editing a {@link UserDto} inside a user tab. Renders the
 * user name, email, password, store and auth role fields; the store combo box
 * is loaded lazily through a callback data provider. Wires the save/update
 * buttons to {@link UserSaveEventListener} / {@link UserUpdateEventListener}
 * and removes the tab when a store BROADCAST message arrives.
 */
@RequiredArgsConstructor
@Route("users-form")
@Slf4j
public class UserForm extends FormLayout  {
    Registration broadcasterRegistration;
    @Getter
    BeanValidationBinder<UserDto> binder = new BeanValidationBinder<>(UserDto.class);

    TextField userNameField = new TextField(Messages.get("label.field.userName"));
    EmailField userEmailField = new EmailField(Messages.get("label.email"));

    PasswordField userPassField = new PasswordField(Messages.get(Messages.Keys.LABEL_PASSWORD));

    ComboBox<StoreDto> storeDtoComboBox = new ComboBox<>(Messages.get("label.store"));
    ComboBox<RoleType> authDtoComboBox = new ComboBox<>(Messages.get("label.auth"));

    Button saveButton = new Button(Messages.get(Messages.Keys.ACTION_SAVE));
    Button closeButton = new Button(Messages.get(Messages.Keys.ACTION_CANCEL));
    Button updateButton = new Button(Messages.get(Messages.Keys.ACTION_UPDATE));

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final AccessService accessService;

    private final Tab userTab;
    private final FormAction formAction;
    private final transient UserDto userDto;

    UI ui;
    private int totalRow = 1;

    private void renderLayout() {
        setSizeFull();
        storeDtoComboBox.setAllowCustomValue(false);
        storeDtoComboBox.setItemLabelGenerator(StoreDto::getName); // Display store name
        userNameField.getElement().setAttribute(Css.AUTOCOMPLETE, "off");
        userEmailField.getElement().setAttribute(Css.AUTOCOMPLETE, "off");
        userPassField.getElement().setAttribute(Css.AUTOCOMPLETE, "off");

        storeDtoComboBox.setDataProvider(
            DataProvider.fromFilteringCallbacks(
                    query -> fetchStores(query.getFilter().orElse(""), query.getOffset(), query.getLimit()).stream(),
                    event -> countStores()
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

    /**
     * Removes the user tab from the parent {@link TabSheet} on the UI thread.
     */
    public void removeFromSheet() {
        UiUtil.safeAccess(this.ui, () -> {
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
        binder.forField(userNameField)
                .withValidator(value -> value.length() > 2,
                        Messages.get(Messages.Keys.VALIDATION_NAME_MIN_LENGTH))
                .bind(UserDto::getUsername, UserDto::setUsername);
    }

    private HorizontalLayout createButtonsLayout() {

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        saveButton.addClickShortcut(Key.ENTER);
        updateButton.addClickShortcut(Key.ENTER);

        closeButton.addClickShortcut(Key.ESCAPE);
        closeButton.addClickListener(event -> removeFromSheet());

        saveButton.addClickListener(new UserSaveEventListener(this, restClientOrganizationService));
        updateButton.addClickListener(new UserUpdateEventListener(this, restClientOrganizationService));
        HorizontalLayout horizontalLayout = new HorizontalLayout(saveButton, updateButton, closeButton);
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

    /**
     * Shows or hides the save, update and cancel buttons depending on the form
     * action (only save for create, only update for edit; cancel always
     * visible).
     */
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

    /**
     * Copies the field values into the wrapped user DTO and returns it.
     *
     * @return the populated user DTO for saving or updating
     */
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
