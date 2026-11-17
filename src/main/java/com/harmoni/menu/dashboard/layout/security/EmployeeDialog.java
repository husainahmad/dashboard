package com.harmoni.menu.dashboard.layout.security;

import com.harmoni.menu.dashboard.dto.AssignmentDto;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.dto.EmployeeDto;
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
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Modal editor for an employee: identity, role, scope and status.
 */
public class EmployeeDialog extends Dialog {

    private final RestClientOrganizationService restClientOrganizationService;
    private final List<RoleDto> roles;
    private final List<BrandDto> brands;
    private final List<ChainDto> chains;
    private final List<StoreDto> stores;
    private final EmployeeDto editing;
    private final Runnable onSaved;

    private final TextField usernameField = new TextField(Messages.get("label.field.userName"));
    private final EmailField emailField = new EmailField(Messages.get("label.email"));
    private final PasswordField passwordField = new PasswordField(Messages.get("label.password"));
    private final ComboBox<RoleDto> roleBox = new ComboBox<>(Messages.get("employee.column.role"));
    private final ComboBox<String> statusBox = new ComboBox<>(Messages.get("employee.column.status"));
    private final VerticalLayout scopeContainer = new VerticalLayout();

    private ComboBox<BrandDto> brandBox;
    private ComboBox<ChainDto> chainBox;
    private MultiSelectComboBox<StoreDto> storeBox;

    public EmployeeDialog(RestClientOrganizationService restClientOrganizationService,
                          List<RoleDto> roles, List<BrandDto> brands, List<ChainDto> chains,
                          List<StoreDto> stores, EmployeeDto editing, Runnable onSaved) {
        this.restClientOrganizationService = restClientOrganizationService;
        this.roles = roles == null ? List.of() : roles;
        this.brands = brands == null ? List.of() : brands;
        this.chains = chains == null ? List.of() : chains;
        this.stores = stores == null ? List.of() : stores;
        this.editing = editing;
        this.onSaved = onSaved;

        setHeaderTitle(Messages.get(editing == null ? "employee.dialog.createTitle" : "employee.dialog.editTitle"));
        setWidth("560px");

        configureFields();
        add(form());
        getFooter().add(cancelButton(), saveButton());

        if (editing != null) {
            applyEdit(editing);
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

        roleBox.setItems(roles);
        roleBox.setItemLabelGenerator(RoleDto::getName);
        roleBox.setWidthFull();
        roleBox.addValueChangeListener(event -> rebuildScope());

        statusBox.setItems("ACTIVE", "INACTIVE");
        statusBox.setItemLabelGenerator(status -> Messages.get(
                "INACTIVE".equals(status) ? "employee.status.inactive" : "employee.status.active"));
        statusBox.setValue("ACTIVE");
        statusBox.setWidthFull();

        scopeContainer.setPadding(false);
        scopeContainer.setSpacing(true);
        scopeContainer.setWidthFull();
    }

    private FormLayout form() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("30em", 2));
        form.add(usernameField, 2);
        form.add(emailField, 2);
        form.add(passwordField, 2);
        form.add(roleBox, statusBox);
        form.add(scopeContainer, 2);
        return form;
    }

    private void applyEdit(EmployeeDto employee) {
        usernameField.setValue(employee.getUsername() == null ? "" : employee.getUsername());
        usernameField.setReadOnly(true);
        emailField.setValue(employee.getEmail() == null ? "" : employee.getEmail());
        emailField.setReadOnly(true);
        passwordField.setVisible(false);
        passwordField.setRequired(false);
        statusBox.setValue("INACTIVE".equalsIgnoreCase(employee.getStatus()) ? "INACTIVE" : "ACTIVE");

        if (!employee.getAssignments().isEmpty()) {
            AssignmentDto first = employee.getAssignments().getFirst();
            roles.stream()
                    .filter(role -> Objects.equals(role.getId(), first.getRoleId()))
                    .findFirst()
                    .ifPresent(roleBox::setValue);
        }
    }

    private void rebuildScope() {
        scopeContainer.removeAll();
        brandBox = null;
        chainBox = null;
        storeBox = null;

        RoleDto role = roleBox.getValue();
        if (role == null) {
            return;
        }
        String scopeType = role.getScopeType() == null ? ScopeLabels.STORE : role.getScopeType();
        switch (scopeType) {
            case ScopeLabels.BRAND -> {
                brandBox = new ComboBox<>(Messages.get("scope.brand"));
                brandBox.setItems(brands);
                brandBox.setItemLabelGenerator(BrandDto::getName);
                brandBox.setWidthFull();
                preselectBrand();
                scopeContainer.add(brandBox);
            }
            case ScopeLabels.CHAIN -> {
                chainBox = new ComboBox<>(Messages.get("scope.chain"));
                chainBox.setItems(chains);
                chainBox.setItemLabelGenerator(ChainDto::getName);
                chainBox.setWidthFull();
                preselectChain();
                scopeContainer.add(chainBox);
            }
            default -> {
                storeBox = new MultiSelectComboBox<>(Messages.get("scope.store"));
                storeBox.setItems(stores);
                storeBox.setItemLabelGenerator(StoreDto::getName);
                storeBox.setWidthFull();
                preselectStores();
                scopeContainer.add(storeBox);
            }
        }
    }

    private void preselectBrand() {
        if (editing == null || editing.getAssignments().isEmpty()) {
            return;
        }
        Long scopeId = editing.getAssignments().getFirst().getScopeId();
        brands.stream()
                .filter(brand -> Objects.equals(brand.getId(), scopeId == null ? null : scopeId.intValue()))
                .findFirst()
                .ifPresent(brandBox::setValue);
    }

    private void preselectChain() {
        if (editing == null || editing.getAssignments().isEmpty()) {
            return;
        }
        Long scopeId = editing.getAssignments().getFirst().getScopeId();
        chains.stream()
                .filter(chain -> Objects.equals(chain.getId(), scopeId == null ? null : scopeId.intValue()))
                .findFirst()
                .ifPresent(chainBox::setValue);
    }

    private void preselectStores() {
        if (editing == null || editing.getAssignments().isEmpty()) {
            return;
        }
        Set<Integer> scopeIds = editing.getAssignments().stream()
                .map(AssignmentDto::getScopeId)
                .filter(Objects::nonNull)
                .map(Long::intValue)
                .collect(Collectors.toSet());
        storeBox.setValue(stores.stream()
                .filter(store -> scopeIds.contains(store.getId()))
                .collect(Collectors.toSet()));
    }

    private Button cancelButton() {
        Button cancel = new Button(Messages.get(Messages.Keys.ACTION_CANCEL), event -> close());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        return cancel;
    }

    private Button saveButton() {
        Button save = new Button(Messages.get(editing == null
                ? "employee.dialog.create" : "employee.dialog.update"), event -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        return save;
    }

    private void save() {
        if (!valid()) {
            return;
        }
        RoleDto role = roleBox.getValue();
        List<AssignmentDto> assignments = assignments(role);
        if (assignments.isEmpty()) {
            return;
        }

        UserDto dto = new UserDto();
        if (editing != null) {
            dto.setId(editing.getId());
            dto.setAuthUserId(editing.getAuthUserId());
        }
        dto.setUsername(usernameField.getValue().trim());
        dto.setEmail(emailField.getValue() == null ? "" : emailField.getValue().trim());
        dto.setPassword(passwordField.getValue());
        dto.setStatus(statusBox.getValue());
        dto.setAssignments(assignments);
        dto.setRoleIds(Set.of(role.getId()));
        dto.setStoreId(primaryStoreId(assignments));

        if (editing == null) {
            restClientOrganizationService.createUser(dto).subscribe(
                    response -> success("employee.created"),
                    error -> failure("employee.saveFailed"));
            return;
        }
        restClientOrganizationService.updateUser(dto).subscribe(
                response -> success("employee.updated"),
                error -> failure("employee.saveFailed"));
    }

    private List<AssignmentDto> assignments(RoleDto role) {
        List<AssignmentDto> assignments = new ArrayList<>();
        String scopeType = role.getScopeType() == null ? ScopeLabels.STORE : role.getScopeType();
        if (ScopeLabels.BRAND.equals(scopeType) && brandBox != null && brandBox.getValue() != null) {
            assignments.add(assignment(role, scopeType, brandBox.getValue().getId().longValue()));
        } else if (ScopeLabels.CHAIN.equals(scopeType) && chainBox != null && chainBox.getValue() != null) {
            assignments.add(assignment(role, scopeType, chainBox.getValue().getId().longValue()));
        } else if (storeBox != null && storeBox.getValue() != null) {
            storeBox.getValue().forEach(store ->
                    assignments.add(assignment(role, ScopeLabels.STORE, store.getId().longValue())));
        }
        return assignments;
    }

    private AssignmentDto assignment(RoleDto role, String scopeType, Long scopeId) {
        return AssignmentDto.builder()
                .roleId(role.getId())
                .roleName(role.getName())
                .roleCode(role.getCode())
                .scopeType(scopeType)
                .scopeId(scopeId)
                .build();
    }

    private Integer primaryStoreId(List<AssignmentDto> assignments) {
        Integer storeId = assignments.stream()
                .filter(assignment -> ScopeLabels.STORE.equals(assignment.getScopeType()))
                .map(AssignmentDto::getScopeId)
                .filter(Objects::nonNull)
                .map(Long::intValue)
                .findFirst()
                .orElse(null);
        if (storeId != null) {
            return storeId;
        }
        if (brandBox != null && brandBox.getValue() != null) {
            Integer brandId = brandBox.getValue().getId();
            return stores.stream()
                    .filter(store -> Objects.equals(brandIdOf(store.getChainId()), brandId))
                    .map(StoreDto::getId)
                    .findFirst()
                    .orElse(editing == null ? null : editing.getStoreId());
        }
        if (chainBox != null && chainBox.getValue() != null) {
            Integer chainId = chainBox.getValue().getId();
            return stores.stream()
                    .filter(store -> Objects.equals(store.getChainId(), chainId))
                    .map(StoreDto::getId)
                    .findFirst()
                    .orElse(editing == null ? null : editing.getStoreId());
        }
        return editing == null ? null : editing.getStoreId();
    }

    private Integer brandIdOf(Integer chainId) {
        return chains.stream()
                .filter(chain -> Objects.equals(chain.getId(), chainId))
                .map(ChainDto::getBrandId)
                .findFirst()
                .orElse(null);
    }

    private boolean valid() {
        boolean valid = true;
        String username = usernameField.getValue() == null ? "" : usernameField.getValue().trim();
        if (username.length() < 6 || !username.matches("^[A-Za-z0-9]+$")) {
            usernameField.setInvalid(true);
            usernameField.setErrorMessage(Messages.get("validation.username.pattern"));
            valid = false;
        }
        if (editing == null) {
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
        if (roleBox.getValue() == null) {
            roleBox.setInvalid(true);
            roleBox.setErrorMessage(Messages.get("validation.user.roleRequired"));
            valid = false;
        } else if (assignments(roleBox.getValue()).isEmpty()
                || primaryStoreId(assignments(roleBox.getValue())) == null) {
            valid = false;
            if (brandBox != null) {
                brandBox.setInvalid(true);
            }
            if (chainBox != null) {
                chainBox.setInvalid(true);
            }
            if (storeBox != null) {
                storeBox.setInvalid(true);
            }
        }
        return valid;
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
        UiUtil.safeAccess(getUI().orElse(null), () -> UiUtil.error(Messages.get(messageKey)));
    }
}
