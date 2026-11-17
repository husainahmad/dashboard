package com.harmoni.menu.dashboard.layout.security;

import com.harmoni.menu.dashboard.dto.PermissionDto;
import com.harmoni.menu.dashboard.dto.RoleDto;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.RestClientRbacService;
import com.harmoni.menu.dashboard.util.Messages;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Modal editor for a role: name, description and the permission matrix.
 */
public class RoleFormDialog extends Dialog {

    private final RestClientRbacService restClientRbacService;
    private final RoleDto editing;
    private final List<PermissionDto> permissions;
    private final Runnable onSaved;

    private final TextField nameField = new TextField(Messages.get("rbac.role.name"));
    private final TextArea descriptionField = new TextArea(Messages.get("rbac.role.description"));
    private final Map<String, Checkbox> permissionBoxes = new LinkedHashMap<>();
    private final Map<String, Long> permissionIdByName = new LinkedHashMap<>();

    public RoleFormDialog(RestClientRbacService restClientRbacService, RoleDto editing,
                          List<PermissionDto> permissions, Runnable onSaved) {
        this.restClientRbacService = restClientRbacService;
        this.editing = editing;
        this.permissions = permissions == null ? List.of() : permissions;
        this.onSaved = onSaved;

        setHeaderTitle(Messages.get(editing == null ? "rbac.role.createTitle" : "rbac.role.editTitle"));
        setWidth("680px");

        configureFields();
        add(form());
        getFooter().add(cancelButton(), saveButton());

        if (editing != null) {
            nameField.setValue(editing.getName() == null ? "" : editing.getName());
            descriptionField.setValue(editing.getDescription() == null ? "" : editing.getDescription());
        }
    }

    private void configureFields() {
        nameField.setRequired(true);
        nameField.setWidthFull();
        descriptionField.setWidthFull();
        descriptionField.setMinHeight("72px");
    }

    private FormLayout form() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.add(nameField, descriptionField);
        form.add(permissionMatrix());
        return form;
    }

    private VerticalLayout permissionMatrix() {
        VerticalLayout matrix = new VerticalLayout();
        matrix.setPadding(false);
        matrix.setSpacing(true);
        matrix.setWidthFull();

        Map<String, List<PermissionDto>> byModule = new LinkedHashMap<>();
        for (PermissionDto permission : permissions) {
            byModule.computeIfAbsent(moduleOf(permission.getName()), key -> new ArrayList<>())
                    .add(permission);
        }

        for (Map.Entry<String, List<PermissionDto>> entry : byModule.entrySet()) {
            Span moduleLabel = new Span(entry.getKey());
            moduleLabel.addClassName("permission-module");

            HorizontalLayout row = new HorizontalLayout(moduleLabel);
            row.setAlignItems(FlexComponent.Alignment.CENTER);
            row.setWidthFull();

            for (PermissionDto permission : entry.getValue()) {
                Checkbox box = new Checkbox(actionOf(permission.getName()));
                box.setValue(isGranted(permission));
                permissionBoxes.put(permission.getName(), box);
                permissionIdByName.put(permission.getName(), permission.getId());
                row.add(box);
            }
            matrix.add(row);
        }
        return matrix;
    }

    private boolean isGranted(PermissionDto permission) {
        return editing != null && editing.getPermissions() != null
                && editing.getPermissions().stream()
                .anyMatch(granted -> Objects.equals(granted.getName(), permission.getName()));
    }

    private Button cancelButton() {
        Button cancel = new Button(Messages.get(Messages.Keys.ACTION_CANCEL), event -> close());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        return cancel;
    }

    private Button saveButton() {
        Button save = new Button(Messages.get(editing == null
                ? "rbac.role.create" : "rbac.role.update"), event -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        return save;
    }

    private void save() {
        if (nameField.getValue() == null || nameField.getValue().isBlank()) {
            nameField.setInvalid(true);
            nameField.setErrorMessage(Messages.get("validation.name.required"));
            return;
        }
        RoleDto roleDto = new RoleDto();
        roleDto.setName(nameField.getValue().trim());
        roleDto.setDescription(descriptionField.getValue());

        if (editing == null) {
            restClientRbacService.createRole(roleDto).subscribe(
                    response -> {
                        RoleDto created = ObjectUtil.convertValueToObject(response.getData(), RoleDto.class);
                        savePermissions(created.getId());
                    },
                    error -> failure("rbac.role.saveFailed"));
            return;
        }
        roleDto.setId(editing.getId());
        restClientRbacService.updateRole(roleDto).subscribe(
                response -> savePermissions(editing.getId()),
                error -> failure("rbac.role.saveFailed"));
    }

    private void savePermissions(Long roleId) {
        Set<Long> permissionIds = new LinkedHashSet<>();
        permissionBoxes.forEach((name, box) -> {
            if (Boolean.TRUE.equals(box.getValue())) {
                Long id = permissionIdByName.get(name);
                if (id != null) {
                    permissionIds.add(id);
                }
            }
        });
        restClientRbacService.replaceRolePermissions(roleId, permissionIds).subscribe(
                response -> success(),
                error -> failure("rbac.role.permissionsFailed"));
    }

    private void success() {
        UiUtil.safeAccess(getUI().orElse(null), () -> {
            UiUtil.success(Messages.get("rbac.role.saved"));
            close();
            if (onSaved != null) {
                onSaved.run();
            }
        });
    }

    private void failure(String messageKey) {
        UiUtil.safeAccess(getUI().orElse(null), () -> UiUtil.error(Messages.get(messageKey)));
    }

    private static String moduleOf(String permission) {
        int index = permission == null ? -1 : permission.indexOf(':');
        return index < 0 ? "general" : permission.substring(0, index);
    }

    private static String actionOf(String permission) {
        int index = permission == null ? -1 : permission.indexOf(':');
        return index < 0 ? permission : permission.substring(index + 1);
    }
}
