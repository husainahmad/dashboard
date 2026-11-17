package com.harmoni.menu.dashboard.layout.security;

import com.harmoni.menu.dashboard.dto.PermissionDto;
import com.harmoni.menu.dashboard.dto.RoleDto;
import com.harmoni.menu.dashboard.layout.util.Permissions;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AuthorizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientRbacService;
import com.harmoni.menu.dashboard.util.Messages;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
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
 * Right-hand role editor: identity, scope and the Lihat/Ubah/Lainnya matrix.
 */
public class RoleEditorPanel extends VerticalLayout {

    private final RestClientRbacService restClientRbacService;
    private final RoleDto role;
    private final List<PermissionDto> permissions;
    private final AuthorizationService authorizationService;
    private final Runnable onChanged;

    private final TextField nameField = new TextField(Messages.get("role.name"));
    private final TextArea descriptionField = new TextArea(Messages.get("role.description"));
    private final ComboBox<String> scopeBox = new ComboBox<>(Messages.get("role.scope"));
    private final Map<String, Checkbox> boxes = new LinkedHashMap<>();
    private final Map<String, Long> permissionIdByName = new LinkedHashMap<>();

    public RoleEditorPanel(RestClientRbacService restClientRbacService, RoleDto role,
                           List<PermissionDto> permissions, AuthorizationService authorizationService,
                           Runnable onChanged) {
        this.restClientRbacService = restClientRbacService;
        this.role = role;
        this.permissions = permissions == null ? List.of() : permissions;
        this.authorizationService = authorizationService;
        this.onChanged = onChanged;

        addClassName("role-editor");
        setPadding(false);
        setSpacing(true);
        setSizeFull();

        configureFields();

        VerticalLayout matrix = matrix();
        add(header());
        if (role.getId() == null) {
            add(identity());
        }
        add(matrix);
        setFlexGrow(1, matrix);
    }

    private void configureFields() {
        nameField.setValue(role.getName() == null ? "" : role.getName());
        nameField.setWidthFull();
        nameField.setRequired(true);
        nameField.setReadOnly(role.isSystem());

        descriptionField.setValue(role.getDescription() == null ? "" : role.getDescription());
        descriptionField.setWidthFull();
        descriptionField.setMinHeight("64px");

        scopeBox.setItems(ScopeLabels.BRAND, ScopeLabels.CHAIN, ScopeLabels.STORE);
        scopeBox.setItemLabelGenerator(ScopeLabels::label);
        scopeBox.setValue(role.getScopeType() == null ? ScopeLabels.STORE : role.getScopeType());
        scopeBox.setWidthFull();
        scopeBox.setReadOnly(role.isSystem());
    }

    private HorizontalLayout header() {
        H2 title = new H2(role.getId() == null ? Messages.get("role.createTitle") : role.getName());
        title.addClassName("role-editor-title");

        VerticalLayout text = new VerticalLayout(title);
        text.setPadding(false);
        text.setSpacing(false);

        if (role.getId() != null) {
            Span scope = new Span(Messages.get("role.scopePrefix") + " "
                    + ScopeLabels.label(role.getScopeType()));
            scope.addClassName("scope-badge");
            scope.addClassName(ScopeLabels.cssClass(role.getScopeType()));

            Span hint = new Span(Messages.get("role.scopeHint", ScopeLabels.label(role.getScopeType())));
            hint.addClassName("role-scope-hint");

            text.add(scope, hint);
        }

        boolean canWrite = authorizationService.hasAnyPermission(
                Permissions.RBAC_MANAGE, Permissions.RBAC_WRITE);

        Button duplicate = new Button(Messages.get("role.duplicate"), event -> duplicate());
        duplicate.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        duplicate.setVisible(role.getId() != null && canWrite);

        Button save = new Button(Messages.get("role.save"), event -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.setVisible(canWrite);

        HorizontalLayout actions = new HorizontalLayout(duplicate, save);
        actions.setSpacing(true);

        HorizontalLayout header = new HorizontalLayout(text, actions);
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setFlexGrow(1, text);
        return header;
    }

    private VerticalLayout identity() {
        VerticalLayout identity = new VerticalLayout(nameField, descriptionField, scopeBox);
        identity.setPadding(false);
        identity.setSpacing(true);
        identity.setWidthFull();
        return identity;
    }

    private VerticalLayout matrix() {
        VerticalLayout matrix = new VerticalLayout();
        matrix.addClassName("permission-matrix");
        matrix.setPadding(false);
        matrix.setSpacing(false);
        matrix.setWidthFull();

        matrix.add(matrixHeader());

        Map<String, List<PermissionDto>> byModule = new LinkedHashMap<>();
        for (PermissionDto permission : permissions) {
            if (permission.getModule() != null) {
                byModule.computeIfAbsent(permission.getModule(), key -> new ArrayList<>()).add(permission);
            }
        }
        for (Map.Entry<String, List<PermissionDto>> entry : byModule.entrySet()) {
            matrix.add(matrixRow(entry.getKey(), entry.getValue()));
        }
        return matrix;
    }

    private HorizontalLayout matrixHeader() {
        Span module = new Span(Messages.get("role.matrix.module"));
        module.addClassName("matrix-header");
        Span view = new Span(Messages.get("role.matrix.view"));
        view.addClassName("matrix-header");
        Span edit = new Span(Messages.get("role.matrix.edit"));
        edit.addClassName("matrix-header");
        Span other = new Span(Messages.get("role.matrix.other"));
        other.addClassName("matrix-header");

        HorizontalLayout header = new HorizontalLayout(module, view, edit, other);
        header.addClassName("matrix-row");
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setFlexGrow(1, module);
        return header;
    }

    private HorizontalLayout matrixRow(String module, List<PermissionDto> modulePermissions) {
        PermissionDto view = find(modulePermissions, "view");
        PermissionDto edit = find(modulePermissions, "edit");
        PermissionDto other = modulePermissions.stream()
                .filter(permission -> "OTHER".equalsIgnoreCase(permission.getCategory()))
                .findFirst()
                .orElse(null);

        String labelText = view != null && view.getLabel() != null ? view.getLabel()
                : other != null && other.getLabel() != null ? other.getLabel()
                : module;
        Span label = new Span(labelText);
        label.addClassName("matrix-module");

        HorizontalLayout row = new HorizontalLayout(label,
                checkbox(view), checkbox(edit), otherCheckbox(other));
        row.addClassName("matrix-row");
        row.setWidthFull();
        row.setAlignItems(FlexComponent.Alignment.CENTER);
        row.setFlexGrow(1, label);
        return row;
    }

    private PermissionDto find(List<PermissionDto> modulePermissions, String action) {
        return modulePermissions.stream()
                .filter(permission -> action.equalsIgnoreCase(permission.getAction()))
                .findFirst()
                .orElse(null);
    }

    private Div checkbox(PermissionDto permission) {
        Div cell = new Div();
        cell.addClassName("matrix-cell");
        if (permission == null) {
            cell.add(new Span("—"));
            return cell;
        }
        Checkbox box = new Checkbox();
        box.setValue(isGranted(permission));
        box.setEnabled(authorizationService.getPermissions().contains(permission.getName()));
        boxes.put(permission.getName(), box);
        permissionIdByName.put(permission.getName(), permission.getId());
        cell.add(box);
        return cell;
    }

    private Div otherCheckbox(PermissionDto permission) {
        Div cell = new Div();
        cell.addClassName("matrix-cell");
        if (permission == null) {
            cell.add(new Span("—"));
            return cell;
        }
        Checkbox box = new Checkbox(permission.getLabel());
        box.setValue(isGranted(permission));
        box.setEnabled(authorizationService.getPermissions().contains(permission.getName()));
        boxes.put(permission.getName(), box);
        permissionIdByName.put(permission.getName(), permission.getId());
        cell.add(box);
        return cell;
    }

    private boolean isGranted(PermissionDto permission) {
        return role.getPermissions() != null && role.getPermissions().stream()
                .anyMatch(granted -> Objects.equals(granted.getName(), permission.getName()));
    }

    private void save() {
        if (nameField.getValue() == null || nameField.getValue().isBlank()) {
            nameField.setInvalid(true);
            nameField.setErrorMessage(Messages.get("validation.name.required"));
            return;
        }
        RoleDto dto = RoleDto.builder()
                .id(role.getId())
                .name(nameField.getValue().trim())
                .description(descriptionField.getValue())
                .scopeType(scopeBox.getValue())
                .code(role.getCode())
                .system(role.isSystem())
                .build();

        if (role.getId() == null) {
            restClientRbacService.createRole(dto).subscribe(
                    response -> {
                        RoleDto created = ObjectUtil.convertValueToObject(response.getData(), RoleDto.class);
                        savePermissions(created.getId());
                    },
                    error -> failure("role.saveFailed"));
            return;
        }
        restClientRbacService.updateRole(dto).subscribe(
                response -> savePermissions(role.getId()),
                error -> failure("role.saveFailed"));
    }

    private void savePermissions(Long roleId) {
        Set<Long> permissionIds = new LinkedHashSet<>();
        boxes.forEach((name, box) -> {
            if (Boolean.TRUE.equals(box.getValue())) {
                Long id = permissionIdByName.get(name);
                if (id != null) {
                    permissionIds.add(id);
                }
            }
        });
        restClientRbacService.replaceRolePermissions(roleId, permissionIds).subscribe(
                response -> success("role.saved"),
                error -> failure("role.permissionsFailed"));
    }

    private void duplicate() {
        restClientRbacService.duplicateRole(role.getId()).subscribe(
                response -> success("role.duplicated"),
                error -> failure("role.saveFailed"));
    }

    private void success(String messageKey) {
        UiUtil.safeAccess(getUI().orElse(null), () -> {
            UiUtil.success(Messages.get(messageKey));
            if (onChanged != null) {
                onChanged.run();
            }
        });
    }

    private void failure(String messageKey) {
        UiUtil.safeAccess(getUI().orElse(null), () -> UiUtil.error(Messages.get(messageKey)));
    }
}
