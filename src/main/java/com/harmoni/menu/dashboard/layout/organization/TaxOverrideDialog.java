package com.harmoni.menu.dashboard.layout.organization;

import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.NumberField;

import java.math.BigDecimal;
import java.util.function.BiConsumer;

/**
 * Small dialog used when a chain or store overrides the tax and service-charge
 * percentages instead of inheriting them.
 */
public class TaxOverrideDialog extends Dialog {

    private final NumberField taxField = new NumberField(Messages.get("organization.tax.percent"));
    private final NumberField serviceField = new NumberField(Messages.get("organization.service.percent"));

    public TaxOverrideDialog(BigDecimal tax, BigDecimal service,
                             BiConsumer<BigDecimal, BigDecimal> onSave) {
        setHeaderTitle(Messages.get("organization.tax.title"));
        setWidth("420px");

        taxField.setMin(0);
        taxField.setMax(100);
        taxField.setStep(0.5);
        taxField.setValue(tax == null ? 0d : tax.doubleValue());

        serviceField.setMin(0);
        serviceField.setMax(100);
        serviceField.setStep(0.5);
        serviceField.setValue(service == null ? 0d : service.doubleValue());

        FormLayout form = new FormLayout(taxField, serviceField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("20cm", 2));
        add(form);

        Button cancel = new Button(Messages.get("action.cancel"), event -> close());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        Button save = new Button(Messages.get("action.save"), event -> {
            BigDecimal taxValue = BigDecimal.valueOf(taxField.getValue() == null ? 0d : taxField.getValue());
            BigDecimal serviceValue = BigDecimal.valueOf(serviceField.getValue() == null ? 0d : serviceField.getValue());
            onSave.accept(taxValue, serviceValue);
            close();
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(cancel, save);
    }
}
