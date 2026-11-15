package com.harmoni.menu.dashboard.layout.organization.dialog;

import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Modal dialog for creating a store, matching the "Tambah Store" mockup.
 */
public class StoreDialog extends Dialog {

    private final RestClientOrganizationService restClientOrganizationService;
    private final Runnable onSaved;
    private final Runnable onCreateChain;

    private final TextField nameField = new TextField(Messages.get("organization.store.name"));
    private final ComboBox<ChainDto> chainBox = new ComboBox<>(Messages.get("organization.store.chain"));
    private final TextField addressField = new TextField(Messages.get("organization.store.address"));
    private final CheckboxGroup<String> orderTypes = new CheckboxGroup<>();

    public StoreDialog(RestClientOrganizationService restClientOrganizationService,
                       List<ChainDto> chains, ChainDto defaultChain,
                       Runnable onCreateChain, Runnable onSaved) {
        this.restClientOrganizationService = restClientOrganizationService;
        this.onCreateChain = onCreateChain;
        this.onSaved = onSaved;

        setHeaderTitle(Messages.get("organization.store.title"));
        setWidth("560px");

        nameField.setRequired(true);
        nameField.setWidthFull();

        chainBox.setItems(chains);
        chainBox.setItemLabelGenerator(ChainDto::getName);
        chainBox.setValue(defaultChain);
        chainBox.setWidthFull();

        Button addChain = new Button(Messages.get("organization.store.addChain"), event -> {
            close();
            if (onCreateChain != null) {
                onCreateChain.run();
            }
        });
        addChain.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        addChain.addClassName("org-inline-add");

        HorizontalLayout chainRow = new HorizontalLayout(chainBox, addChain);
        chainRow.setWidthFull();
        chainRow.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.END);
        chainRow.setFlexGrow(1, chainBox);

        addressField.setWidthFull();

        orderTypes.addClassName("org-choice-chips");
        orderTypes.setItems("DINE_IN", "TAKEAWAY", "DELIVERY");
        orderTypes.setValue(Set.of("DINE_IN"));
        orderTypes.setItemLabelGenerator(value -> switch (value) {
            case "TAKEAWAY" -> Messages.get("organization.store.takeaway");
            case "DELIVERY" -> Messages.get("organization.store.delivery");
            default -> Messages.get("organization.store.dineIn");
        });

        Div info = new Div();
        info.addClassName("org-dialog-info");
        info.setText(Messages.get("organization.store.info"));

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.add(nameField);
        form.add(chainRow);
        form.add(addressField);
        form.add(orderTypes);
        form.add(info);
        add(form);

        Button cancel = new Button(Messages.get("action.cancel"), event -> close());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        Button save = new Button(Messages.get("organization.store.create"), event -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(cancel, save);
    }

    private void save() {
        if (nameField.isEmpty() || nameField.getValue().trim().length() < 2) {
            nameField.setInvalid(true);
            nameField.setErrorMessage(Messages.get("validation.name.minLength"));
            return;
        }
        if (chainBox.getValue() == null) {
            chainBox.setInvalid(true);
            chainBox.setErrorMessage(Messages.get("validation.chain.required"));
            return;
        }
        if (addressField.isEmpty()) {
            addressField.setInvalid(true);
            addressField.setErrorMessage(Messages.get("validation.address.required"));
            return;
        }

        StoreDto dto = new StoreDto();
        dto.setName(nameField.getValue().trim());
        dto.setChainId(chainBox.getValue().getId());
        dto.setAddress(addressField.getValue().trim());
        dto.setOrderTypes(orderTypes.getValue().stream().sorted().collect(Collectors.joining(",")));
        dto.setTaxServiceMode("INHERIT");
        dto.setReceiptLogoMode("INHERIT");
        dto.setMenuPriceMode("INHERIT");
        dto.setOrderTypeMode("INHERIT");

        restClientOrganizationService.createStore(dto).subscribe(
                response -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    UiUtil.success(Messages.get("organization.store.created"));
                    close();
                    if (onSaved != null) {
                        onSaved.run();
                    }
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null),
                        () -> UiUtil.error(Messages.get("organization.store.failed"))));
    }
}
