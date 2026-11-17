package com.harmoni.menu.dashboard.layout.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.harmoni.menu.dashboard.dto.PermissionDto;
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
 * Lists the permission catalog and opens the permission editor.
 */
public class PermissionListView extends VerticalLayout {

    private final RestClientRbacService restClientRbacService;
    private final AuthorizationService authorizationService;

    private final Grid<PermissionDto> grid = new Grid<>(PermissionDto.class, false);
    private final List<PermissionDto> permissions = new ArrayList<>();
    private final Button addButton = new Button(Messages.get("rbac.permission.add"), VaadinIcon.PLUS.create());

    private boolean loaded;

    public PermissionListView(RestClientRbacService restClientRbacService,
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
        grid.addColumn(PermissionDto::getName).setHeader(Messages.get("rbac.permission.name")).setAutoWidth(true);
        grid.addColumn(PermissionDto::getDescription).setHeader(Messages.get("rbac.permission.description")).setAutoWidth(true);
        grid.addComponentColumn(this::actions).setHeader(Messages.get("rbac.actions")).setAutoWidth(true);
        grid.setSizeFull();
    }

    private HorizontalLayout actions(PermissionDto permission) {
        Button edit = UiUtil.editButton(event -> openForm(permission));
        edit.setVisible(authorizationService.hasPermission(Permissions.RBAC_WRITE));

        Button delete = UiUtil.deleteButton(event -> confirmDelete(permission));
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
            loadPermissions();
        }
    }

    private void loadPermissions() {
        restClientRbacService.getPermissions().subscribe(
                response -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    permissions.clear();
                    if (response != null && response.getData() != null) {
                        permissions.addAll(ObjectUtil.convertObjectToObject(
                                response.getData(), new TypeReference<List<PermissionDto>>() { }));
                    }
                    grid.setItems(permissions);
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null),
                        () -> UiUtil.error(Messages.get("rbac.permission.loadFailed"))));
    }

    private void openForm(PermissionDto permission) {
        new PermissionFormDialog(restClientRbacService, permission, this::loadPermissions).open();
    }

    private void confirmDelete(PermissionDto permission) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(Messages.get(Messages.Keys.DIALOG_CONFIRM_TITLE));
        dialog.setText(Messages.get("rbac.permission.confirmDelete", permission.getName()));
        dialog.setCancelable(true);
        dialog.setConfirmText(Messages.get(Messages.Keys.ACTION_DELETE));
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> restClientRbacService.deletePermission(permission.getId()).subscribe(
                response -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    UiUtil.success(Messages.get("rbac.permission.deleted"));
                    loadPermissions();
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null),
                        () -> UiUtil.error(Messages.get("rbac.permission.deleteFailed")))));
        dialog.open();
    }
}
