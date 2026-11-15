package com.harmoni.menu.dashboard.layout.organization.dialog;

import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;

import java.math.BigDecimal;
import java.util.List;

/**
 * Modal dialog for creating a chain, matching the "Tambah Chain" mockup.
 */
public class ChainDialog extends Dialog {

    private final RestClientOrganizationService restClientOrganizationService;
    private final Runnable onSaved;
    private final ChainDto editingChain;

    private final TextField nameField = new TextField(Messages.get("organization.chain.name"));
    private final ComboBox<BrandDto> brandBox = new ComboBox<>(Messages.get("organization.chain.brand"));
    private final RadioButtonGroup<String> typeGroup = new RadioButtonGroup<>();
    private final EmailField managerEmailField = new EmailField(Messages.get("organization.chain.managerEmail"));
    private final RadioButtonGroup<String> taxMode = new RadioButtonGroup<>();
    private final NumberField taxField = new NumberField(Messages.get("organization.tax.percent"));
    private final NumberField serviceField = new NumberField(Messages.get("organization.service.percent"));

    public ChainDialog(RestClientOrganizationService restClientOrganizationService,
                       List<BrandDto> brands, BrandDto defaultBrand, Runnable onSaved) {
        this(restClientOrganizationService, brands, defaultBrand, null, onSaved);
    }

    public ChainDialog(RestClientOrganizationService restClientOrganizationService,
                       List<BrandDto> brands, BrandDto defaultBrand, ChainDto editingChain,
                       Runnable onSaved) {
        this.restClientOrganizationService = restClientOrganizationService;
        this.onSaved = onSaved;
        this.editingChain = editingChain;

        setHeaderTitle(Messages.get("organization.chain.title"));
        setWidth("560px");

        nameField.setRequired(true);
        nameField.setWidthFull();

        brandBox.setItems(brands);
        brandBox.setItemLabelGenerator(BrandDto::getName);
        brandBox.setValue(defaultBrand);
        brandBox.setWidthFull();

        typeGroup.addClassName("org-choice-chips");
        typeGroup.setItems("REGION", "FRANCHISE", "OTHER");
        typeGroup.setValue("REGION");
        typeGroup.setItemLabelGenerator(value -> switch (value) {
            case "FRANCHISE" -> Messages.get("organization.chain.type.franchise");
            case "OTHER" -> Messages.get("organization.chain.type.other");
            default -> Messages.get("organization.chain.type.region");
        });

        managerEmailField.setWidthFull();

        taxMode.setItems("INHERIT", "OWN");
        taxMode.setValue("INHERIT");
        taxMode.setItemLabelGenerator(value -> "OWN".equals(value)
                ? Messages.get("organization.chain.taxOwn")
                : Messages.get("organization.chain.taxInherit"));
        taxMode.addValueChangeListener(event -> {
            boolean own = "OWN".equals(event.getValue());
            taxField.setVisible(own);
            serviceField.setVisible(own);
        });

        taxField.setMin(0);
        taxField.setMax(100);
        taxField.setStep(0.5);
        taxField.setVisible(false);
        serviceField.setMin(0);
        serviceField.setMax(100);
        serviceField.setStep(0.5);
        serviceField.setVisible(false);

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("20cm", 2));
        form.add(nameField, 2);
        form.add(brandBox, 2);
        form.add(typeGroup, 2);
        form.add(managerEmailField, 2);
        form.add(taxMode, 2);
        form.add(taxField, serviceField);
        add(form);

        Button cancel = new Button(Messages.get("action.cancel"), event -> close());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        Button save = new Button(Messages.get(editingChain == null
                ? "organization.chain.create" : "organization.chain.update"), event -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(cancel, save);

        if (editingChain != null) {
            applyEdit(editingChain, brands, defaultBrand);
        }
    }

    private void applyEdit(ChainDto chain, List<BrandDto> brands, BrandDto defaultBrand) {
        setHeaderTitle(Messages.get("organization.chain.editTitle"));
        nameField.setValue(chain.getName() == null ? "" : chain.getName());
        managerEmailField.setValue(chain.getManagerEmail() == null ? "" : chain.getManagerEmail());
        typeGroup.setValue(chain.getType() == null ? "REGION" : chain.getType());

        Integer brandId = chain.getBrandId();
        BrandDto brand = brandId == null ? null : brands.stream()
                .filter(item -> brandId.equals(item.getId()))
                .findFirst()
                .orElse(null);
        brandBox.setValue(brand != null ? brand : defaultBrand);

        String mode = chain.getTaxServiceMode() == null ? "INHERIT" : chain.getTaxServiceMode();
        taxMode.setValue(mode);
        boolean own = "OWN".equals(mode);
        taxField.setVisible(own);
        serviceField.setVisible(own);
        if (chain.getTaxPercent() != null) {
            taxField.setValue(chain.getTaxPercent().doubleValue());
        }
        if (chain.getServiceChargePercent() != null) {
            serviceField.setValue(chain.getServiceChargePercent().doubleValue());
        }
    }

    private void save() {
        if (nameField.isEmpty() || nameField.getValue().trim().length() < 2) {
            nameField.setInvalid(true);
            nameField.setErrorMessage(Messages.get("validation.name.minLength"));
            return;
        }
        if (brandBox.getValue() == null) {
            brandBox.setInvalid(true);
            brandBox.setErrorMessage(Messages.get("validation.brand.required"));
            return;
        }
        ChainDto dto = new ChainDto();
        if (editingChain != null) {
            dto.setId(editingChain.getId());
        }
        dto.setName(nameField.getValue().trim());
        dto.setBrandId(brandBox.getValue().getId());
        dto.setType(typeGroup.getValue());
        dto.setManagerEmail(managerEmailField.getValue());
        dto.setTaxServiceMode(taxMode.getValue());
        if ("OWN".equals(taxMode.getValue())) {
            dto.setTaxPercent(BigDecimal.valueOf(taxField.getValue() == null ? 0d : taxField.getValue()));
            dto.setServiceChargePercent(BigDecimal.valueOf(serviceField.getValue() == null ? 0d : serviceField.getValue()));
        }

        if (editingChain == null) {
            restClientOrganizationService.createChain(dto).subscribe(
                    response -> success("organization.chain.created"),
                    error -> failure("organization.chain.failed"));
            return;
        }

        restClientOrganizationService.updateChain(dto).subscribe(
                response -> success("organization.chain.updated"),
                error -> failure("organization.chain.updateFailed"));
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
