package com.harmoni.menu.dashboard.layout.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.harmoni.menu.dashboard.dto.PermissionDto;
import com.harmoni.menu.dashboard.dto.RoleDto;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.layout.util.Permissions;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AuthorizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientRbacService;
import com.harmoni.menu.dashboard.util.Messages;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Lists the roles and opens the role editor.
 */
public class RoleListView extends VerticalLayout {

    private final RestClientRbacService restClientRbacService;
    private final AuthorizationService authorizationService;

    private final Grid<RoleDto> grid = new Grid<>(RoleDto.class, false);
    private final List<RoleDto> roles = new ArrayList<>();
    private final List<PermissionDto> permissions = new ArrayList<>();
    private final Button addButton = new Button(Messages.get("rbac.role.add"), VaadinIcon.PLUS.create());

    private boolean loaded;

    public RoleListView(RestClientRbacService restClientRbacService,
                        AuthorizationService authorizationService) {
        this.restClientRbacService = restClientRbacService;
        this.authorizationService = authorizationService;

        setSizeFull();
        setPadding(false);
        setSpacing(true);

        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.setVisible(authorizationService.hasPermission(Permissions.RBAC_WRITE));
        addButton.addClickListener(event -> openForm(null));

        HorizontalLayout toolbar = new HorizontalLayout(addButton);
        toolbar.addClassName(Css.TOOLBAR);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        configureGrid();
        add(toolbar, grid);
        setFlexGrow(1, grid);
    }

    private void configureGrid() {
        grid.addColumn(RoleDto::getName).setHeader(Messages.get("rbac.role.name")).setAutoWidth(true);
        grid.addColumn(RoleDto::getDescription).setHeader(Messages.get("rbac.role.description")).setAutoWidth(true);
        grid.addColumn(role -> role.getPermissions() == null ? 0 : role.getPermissions().size())
                .setHeader(Messages.get("rbac.role.permissionCount")).setAutoWidth(true);
        grid.addComponentColumn(this::actions).setHeader(Messages.get("rbac.actions")).setAutoWidth(true);
        grid.setSizeFull();
    }

    private HorizontalLayout actions(RoleDto role) {
        Button edit = UiUtil.editButton(event -> openForm(role));
        edit.setVisible(authorizationService.hasPermission(Permissions.RBAC_WRITE));

        Button delete = UiUtil.deleteButton(event -> confirmDelete(role));
        delete.setVisible(authorizationService.hasPermission(Permissions.RBAC_DELETE));

        HorizontalLayout actions = new HorizontalLayout(edit, delete);
        actions.setSpacing(true);
        return actions;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (!loaded) {
            loaded = true;
            loadRoles();
            loadPermissions();
        }
    }

    private void loadRoles() {
        restClientRbacService.getRoles().subscribe(
                response -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    roles.clear();
                    if (response != null && response.getData() != null) {
                        roles.addAll(ObjectUtil.convertObjectToObject(
                                response.getData(), new TypeReference<List<RoleDto>>() { }));
                    }
                    grid.setItems(roles);
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null),
                        () -> UiUtil.error(Messages.get("rbac.role.loadFailed"))));
    }

    private void loadPermissions() {
        restClientRbacService.getPermissions().subscribe(
                response -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    permissions.clear();
                    if (response != null && response.getData() != null) {
                        permissions.addAll(ObjectUtil.convertObjectToObject(
                                response.getData(), new TypeReference<List<PermissionDto>>() { }));
                    }
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null),
                        () -> UiUtil.error(Messages.get("rbac.permission.loadFailed"))));
    }

    private void openForm(RoleDto role) {
        new RoleFormDialog(restClientRbacService, role, permissions, this::loadRoles).open();
    }

    private void confirmDelete(RoleDto role) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(Messages.get(Messages.Keys.DIALOG_CONFIRM_TITLE));
        dialog.setText(Messages.get("rbac.role.confirmDelete", role.getName()));
        dialog.setCancelable(true);
        dialog.setConfirmText(Messages.get(Messages.Keys.ACTION_DELETE));
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> restClientRbacService.deleteRole(role.getId()).subscribe(
                response -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    UiUtil.success(Messages.get("rbac.role.deleted"));
                    loadRoles();
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null),
                        () -> UiUtil.error(Messages.get("rbac.role.deleteFailed")))));
        dialog.open();
    }
}
