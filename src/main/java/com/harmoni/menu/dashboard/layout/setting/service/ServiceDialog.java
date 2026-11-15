package com.harmoni.menu.dashboard.layout.setting.service;

import com.harmoni.menu.dashboard.dto.ServiceDto;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextField;

/**
 * Modal dialog for creating and editing a service name.
 */
public class ServiceDialog extends Dialog {

    private final RestClientMenuService restClientMenuService;
    private final ServiceDto editingService;
    private final Runnable onSaved;

    private final TextField nameField = new TextField(Messages.get(Messages.Keys.LABEL_FIELD_SERVICE_NAME));

    public ServiceDialog(RestClientMenuService restClientMenuService,
                         ServiceDto editingService, Runnable onSaved) {
        this.restClientMenuService = restClientMenuService;
        this.editingService = editingService;
        this.onSaved = onSaved;

        setHeaderTitle(Messages.get(editingService == null
                ? "service.dialog.title.create" : "service.dialog.title.edit"));
        setWidth("460px");

        nameField.setRequired(true);
        nameField.setWidthFull();
        nameField.getElement().setAttribute("autocomplete", "off");
        nameField.setHelperText(Messages.get("service.dialog.nameHint"));

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.add(nameField);
        add(form);

        Button cancel = new Button(Messages.get(Messages.Keys.ACTION_CANCEL), event -> close());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button save = new Button(Messages.get(editingService == null
                ? "service.dialog.create" : "service.dialog.update"), event -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(cancel, save);

        if (editingService != null) {
            nameField.setValue(editingService.getName() == null ? "" : editingService.getName());
        }
    }

    private void save() {
        if (nameField.isEmpty() || nameField.getValue().trim().length() < 3) {
            nameField.setInvalid(true);
            nameField.setErrorMessage(Messages.get(Messages.Keys.VALIDATION_NAME_MIN_LENGTH));
            return;
        }
        ServiceDto dto = editingService == null ? new ServiceDto() : copyOf(editingService);
        dto.setName(nameField.getValue().trim());

        if (editingService == null) {
            restClientMenuService.createService(dto).subscribe(
                    response -> success("service.created"),
                    error -> failure("service.saveFailed"));
            return;
        }
        restClientMenuService.updateService(dto).subscribe(
                response -> success("service.updated"),
                error -> failure("service.saveFailed"));
    }

    private ServiceDto copyOf(ServiceDto source) {
        ServiceDto dto = new ServiceDto();
        dto.setId(source.getId());
        dto.setName(source.getName());
        dto.setSubServices(source.getSubServices());
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
