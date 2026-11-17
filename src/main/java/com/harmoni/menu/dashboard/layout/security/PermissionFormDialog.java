package com.harmoni.menu.dashboard.layout.security;

import com.harmoni.menu.dashboard.dto.PermissionDto;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.RestClientRbacService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;

/**
 * Modal editor for a permission.
 */
public class PermissionFormDialog extends Dialog {

    private final RestClientRbacService restClientRbacService;
    private final PermissionDto editing;
    private final Runnable onSaved;

    private final TextField nameField = new TextField(Messages.get("rbac.permission.name"));
    private final TextArea descriptionField = new TextArea(Messages.get("rbac.permission.description"));

    public PermissionFormDialog(RestClientRbacService restClientRbacService, PermissionDto editing,
                                Runnable onSaved) {
        this.restClientRbacService = restClientRbacService;
        this.editing = editing;
        this.onSaved = onSaved;

        setHeaderTitle(Messages.get(editing == null
                ? "rbac.permission.createTitle" : "rbac.permission.editTitle"));
        setWidth("520px");

        nameField.setRequired(true);
        nameField.setWidthFull();
        nameField.setHelperText(Messages.get("rbac.permission.nameHint"));
        descriptionField.setWidthFull();
        descriptionField.setMinHeight("72px");

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.add(nameField, descriptionField);
        add(form);

        Button cancel = new Button(Messages.get(Messages.Keys.ACTION_CANCEL), event -> close());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button save = new Button(Messages.get(editing == null
                ? "rbac.permission.create" : "rbac.permission.update"), event -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(cancel, save);

        if (editing != null) {
            nameField.setValue(editing.getName() == null ? "" : editing.getName());
            descriptionField.setValue(editing.getDescription() == null ? "" : editing.getDescription());
        }
    }

    private void save() {
        if (nameField.getValue() == null || nameField.getValue().isBlank()) {
            nameField.setInvalid(true);
            nameField.setErrorMessage(Messages.get("validation.name.required"));
            return;
        }
        PermissionDto dto = new PermissionDto();
        dto.setName(nameField.getValue().trim());
        dto.setDescription(descriptionField.getValue());

        if (editing == null) {
            restClientRbacService.createPermission(dto).subscribe(
                    response -> success(), error -> failure("rbac.permission.saveFailed"));
            return;
        }
        dto.setId(editing.getId());
        restClientRbacService.updatePermission(dto).subscribe(
                response -> success(), error -> failure("rbac.permission.saveFailed"));
    }

    private void success() {
        UiUtil.safeAccess(getUI().orElse(null), () -> {
            UiUtil.success(Messages.get("rbac.permission.saved"));
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
