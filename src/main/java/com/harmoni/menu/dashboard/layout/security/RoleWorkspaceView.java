package com.harmoni.menu.dashboard.layout.security;

import com.fasterxml.jackson.core.type.TypeReference;
import com.harmoni.menu.dashboard.dto.PermissionDto;
import com.harmoni.menu.dashboard.dto.RoleDto;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AuthorizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientRbacService;
import com.harmoni.menu.dashboard.util.Messages;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Role master-detail workspace: the role list on the left, the permission
 * matrix on the right.
 */
public class RoleWorkspaceView extends HorizontalLayout {

    private final RestClientRbacService restClientRbacService;
    private final AuthorizationService authorizationService;

    private final VerticalLayout roleList = new VerticalLayout();
    private final VerticalLayout editorHost = new VerticalLayout();

    private final List<RoleDto> roles = new ArrayList<>();
    private final List<PermissionDto> permissions = new ArrayList<>();
    private RoleDto selected;

    private boolean loaded;

    public RoleWorkspaceView(RestClientRbacService restClientRbacService,
                             AuthorizationService authorizationService) {
        this.restClientRbacService = restClientRbacService;
        this.authorizationService = authorizationService;

        setSizeFull();
        setPadding(false);
        setSpacing(true);

        VerticalLayout left = new VerticalLayout();
        left.addClassName("role-list-panel");
        left.setPadding(false);
        left.setSpacing(true);
        left.setWidth("320px");
        left.setHeightFull();

        roleList.setPadding(false);
        roleList.setSpacing(false);
        roleList.setWidthFull();

        left.add(roleList);
        left.setFlexGrow(1, roleList);

        editorHost.addClassName("role-editor-host");
        editorHost.setPadding(false);
        editorHost.setSpacing(false);
        editorHost.setSizeFull();

        add(left, editorHost);
        setFlexGrow(1, editorHost);
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
                    loadRoles();
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    UiUtil.error(Messages.get("rbac.permission.loadFailed"));
                    loadRoles();
                }));
    }

    private void loadRoles() {
        restClientRbacService.getRoles().subscribe(
                response -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    roles.clear();
                    if (response != null && response.getData() != null) {
                        roles.addAll(ObjectUtil.convertObjectToObject(
                                response.getData(), new TypeReference<List<RoleDto>>() { }));
                    }
                    renderRoleList();
                    RoleDto target = selected == null ? null : roles.stream()
                            .filter(role -> Objects.equals(role.getId(), selected.getId()))
                            .findFirst()
                            .orElse(null);
                    if (target == null && !roles.isEmpty()) {
                        target = roles.getFirst();
                    }
                    select(target);
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null),
                        () -> UiUtil.error(Messages.get("rbac.role.loadFailed"))));
    }

    private void renderRoleList() {
        roleList.removeAll();
        for (RoleDto role : roles) {
            roleList.add(roleItem(role));
        }
    }

    private Div roleItem(RoleDto role) {
        Span name = new Span(role.getName());
        name.addClassName("role-list-name");

        Span meta = new Span(ScopeLabels.label(role.getScopeType())
                + " · " + (role.getEmployeeCount() == null ? 0 : role.getEmployeeCount())
                + " " + Messages.get("role.employeeCountSuffix"));
        meta.addClassName("role-list-meta");

        Div item = new Div(name, meta);
        item.addClassName("role-list-item");
        item.getElement().getClassList().set("role-list-item-selected",
                selected != null && Objects.equals(selected.getId(), role.getId()));
        item.addClickListener(event -> select(role));
        return item;
    }

    private void select(RoleDto role) {
        selected = role;
        renderRoleList();
        editorHost.removeAll();
        if (role == null) {
            return;
        }
        editorHost.add(new RoleEditorPanel(restClientRbacService, role, permissions,
                authorizationService, this::loadRoles));
    }

    /**
     * Opens the create-role editor, used by the page header action.
     */
    public void openCreate() {
        RoleDto draft = RoleDto.builder()
                .name("")
                .scopeType(ScopeLabels.STORE)
                .build();
        editorHost.removeAll();
        editorHost.add(new RoleEditorPanel(restClientRbacService, draft, permissions,
                authorizationService, this::loadRoles));
    }
}
