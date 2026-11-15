package com.harmoni.menu.dashboard.layout.organization.tier;

import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.dto.TierTypeDto;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.TextField;

import java.util.List;

/**
 * Modal dialog for creating a tier. The type decides which settings panel the
 * tier exposes on the unified Tier page.
 */
public class TierDialog extends Dialog {

    private final RestClientOrganizationService restClientOrganizationService;
    private final Runnable onSaved;

    private final TextField nameField = new TextField(Messages.get("tier.name"));
    private final ComboBox<BrandDto> brandBox = new ComboBox<>(Messages.get("tier.brand"));
    private final RadioButtonGroup<TierTypeDto> typeGroup = new RadioButtonGroup<>();

    public TierDialog(RestClientOrganizationService restClientOrganizationService,
                      List<BrandDto> brands, BrandDto defaultBrand,
                      TierTypeDto defaultType, Runnable onSaved) {
        this.restClientOrganizationService = restClientOrganizationService;
        this.onSaved = onSaved;

        setHeaderTitle(Messages.get("tier.add"));
        setWidth("480px");

        nameField.setRequired(true);
        nameField.setWidthFull();

        brandBox.setItems(brands);
        brandBox.setItemLabelGenerator(BrandDto::getName);
        brandBox.setValue(defaultBrand != null ? defaultBrand
                : (brands == null || brands.isEmpty() ? null : brands.getFirst()));
        brandBox.setWidthFull();

        typeGroup.addClassName("org-choice-chips");
        typeGroup.setItems(TierTypeDto.PRICE, TierTypeDto.SERVICE, TierTypeDto.MENU);
        typeGroup.setValue(defaultType == null ? TierTypeDto.PRICE : defaultType);
        typeGroup.setItemLabelGenerator(type -> switch (type) {
            case SERVICE -> Messages.get("tier.type.service");
            case MENU -> Messages.get("tier.type.menu");
            default -> Messages.get("tier.type.price");
        });

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.add(nameField, brandBox, typeGroup);
        add(form);

        Button cancel = new Button(Messages.get("action.cancel"), event -> close());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        Button save = new Button(Messages.get("tier.create"), event -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(cancel, save);
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
        TierDto dto = new TierDto();
        dto.setName(nameField.getValue().trim());
        dto.setBrandId(brandBox.getValue().getId());
        dto.setBrandDto(brandBox.getValue());
        dto.setType(typeGroup.getValue());

        restClientOrganizationService.createTier(dto).subscribe(
                response -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    UiUtil.success(Messages.get("tier.created"));
                    close();
                    if (onSaved != null) {
                        onSaved.run();
                    }
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null),
                        () -> UiUtil.error(Messages.get("tier.failed"))));
    }
}
