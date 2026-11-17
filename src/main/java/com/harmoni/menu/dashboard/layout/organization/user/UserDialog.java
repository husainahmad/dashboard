package com.harmoni.menu.dashboard.layout.organization.user;

import com.harmoni.menu.dashboard.dto.RoleDto;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.dto.UserDto;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Modal dialog for creating and editing a dashboard user. Create collects the
 * identity, password, store and role; edit keeps the identity read-only and
 * lets the role and store assignment change.
 */
public class UserDialog extends Dialog {

    private final RestClientOrganizationService restClientOrganizationService;
    private final List<StoreDto> stores;
    private final List<RoleDto> roles;
    private final UserDto editingUser;
    private final Runnable onSaved;

    private final TextField usernameField = new TextField(Messages.get("label.field.userName"));
    private final EmailField emailField = new EmailField(Messages.get("label.email"));
    private final PasswordField passwordField = new PasswordField(Messages.get("label.password"));
    private final ComboBox<StoreDto> storeBox = new ComboBox<>(Messages.get(Messages.Keys.LABEL_STORE));
    private final MultiSelectComboBox<RoleDto> roleBox = new MultiSelectComboBox<>(Messages.get("label.auth"));

    public UserDialog(RestClientOrganizationService restClientOrganizationService,
                      List<StoreDto> stores, List<RoleDto> roles, UserDto editingUser, Runnable onSaved) {
        this.restClientOrganizationService = restClientOrganizationService;
        this.stores = stores;
        this.roles = roles == null ? List.of() : roles;
        this.editingUser = editingUser;
        this.onSaved = onSaved;

        setHeaderTitle(Messages.get(editingUser == null
                ? "user.dialog.title.create" : "user.dialog.title.edit"));
        setWidth("520px");

        configureFields();
        add(form());

        Button cancel = new Button(Messages.get(Messages.Keys.ACTION_CANCEL), event -> close());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button save = new Button(Messages.get(editingUser == null
                ? "user.dialog.create" : "user.dialog.update"), event -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(cancel, save);

        if (editingUser != null) {
            applyEdit(editingUser);
        }
    }

    private void configureFields() {
        usernameField.setRequired(true);
        usernameField.setWidthFull();
        usernameField.getElement().setAttribute("autocomplete", "off");

        emailField.setRequired(true);
        emailField.setWidthFull();
        emailField.getElement().setAttribute("autocomplete", "off");

        passwordField.setRequired(true);
        passwordField.setWidthFull();
        passwordField.getElement().setAttribute("autocomplete", "new-password");

        storeBox.setItems(stores);
        storeBox.setItemLabelGenerator(StoreDto::getName);
        storeBox.setClearButtonVisible(true);
        storeBox.setWidthFull();
        if (stores.size() == 1) {
            storeBox.setValue(stores.getFirst());
        }

        roleBox.setItems(roles);
        roleBox.setItemLabelGenerator(RoleDto::getName);
        roleBox.setWidthFull();
        roleBox.setVisible(!roles.isEmpty());
    }

    private FormLayout form() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("30em", 2));
        form.add(usernameField, 2);
        form.add(emailField, 2);
        form.add(passwordField, 2);
        form.add(storeBox, roleBox);
        return form;
    }

    private void applyEdit(UserDto user) {
        usernameField.setValue(user.getUsername() == null ? "" : user.getUsername());
        usernameField.setReadOnly(true);
        emailField.setValue(user.getEmail() == null ? "" : user.getEmail());
        emailField.setReadOnly(true);
        emailField.setHelperText(Messages.get("user.dialog.identityLocked"));
        passwordField.setVisible(false);
        passwordField.setRequired(false);

        stores.stream()
                .filter(store -> user.getStoreId() != null && user.getStoreId().equals(store.getId()))
                .findFirst()
                .ifPresent(storeBox::setValue);

        Set<Long> roleIds = user.getRoleIds() == null ? Set.of() : user.getRoleIds();
        roleBox.setValue(roles.stream()
                .filter(role -> roleIds.contains(role.getId()))
                .collect(Collectors.toSet()));
    }

    private void save() {
        if (!valid()) {
            return;
        }
        UserDto dto = editingUser == null ? new UserDto() : copyOf(editingUser);
        dto.setUsername(usernameField.getValue().trim());
        dto.setEmail(emailField.getValue() == null ? "" : emailField.getValue().trim());
        dto.setPassword(passwordField.getValue());
        dto.setStoreId(storeBox.getValue() == null ? null : storeBox.getValue().getId());
        Set<Long> roleIds = roleBox.getValue().stream()
                .map(RoleDto::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        dto.setRoleIds(roleIds);
        dto.setAuthId(firstRoleId(roleIds));
        if (editingUser == null) {
            restClientOrganizationService.createUser(dto).subscribe(
                    response -> success("user.created"),
                    error -> failure("user.saveFailed"));
            return;
        }
        restClientOrganizationService.updateUser(dto).subscribe(
                response -> success("user.updated"),
                error -> failure("user.saveFailed"));
    }

    private boolean valid() {
        boolean valid = true;
        if (usernameField.isEmpty() || usernameField.getValue().trim().length() < 3) {
            usernameField.setInvalid(true);
            usernameField.setErrorMessage(Messages.get(Messages.Keys.VALIDATION_NAME_MIN_LENGTH));
            valid = false;
        }
        if (editingUser == null) {
            String email = emailField.getValue() == null ? "" : emailField.getValue().trim();
            if (email.isEmpty() || !email.contains("@")) {
                emailField.setInvalid(true);
                emailField.setErrorMessage(Messages.get("validation.email.required"));
                valid = false;
            }
            if (passwordField.isEmpty()) {
                passwordField.setInvalid(true);
                passwordField.setErrorMessage(Messages.get("validation.password.required"));
                valid = false;
            }
        }
        if (storeBox.getValue() == null) {
            storeBox.setInvalid(true);
            storeBox.setErrorMessage(Messages.get("validation.store.required"));
            valid = false;
        }
        if (roleBox.isVisible() && (roleBox.getValue() == null || roleBox.getValue().isEmpty())) {
            roleBox.setInvalid(true);
            roleBox.setErrorMessage(Messages.get("validation.user.roleRequired"));
            valid = false;
        }
        return valid;
    }

    private Integer firstRoleId(Set<Long> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) {
            return null;
        }
        return roleIds.stream()
                .filter(Objects::nonNull)
                .findFirst()
                .map(Long::intValue)
                .orElse(null);
    }

    private UserDto copyOf(UserDto source) {
        UserDto dto = new UserDto();
        dto.setId(source.getId());
        dto.setAuthId(source.getAuthId());
        dto.setAuthUserId(source.getAuthUserId());
        dto.setRoleIds(source.getRoleIds());
        dto.setUsername(source.getUsername());
        dto.setEmail(source.getEmail());
        dto.setStoreId(source.getStoreId());
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
