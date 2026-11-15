package com.harmoni.menu.dashboard.layout.organization.dialog;

import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ImageDto;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Modal dialog for creating a brand, matching the "Tambah Brand" mockup.
 */
public class BrandDialog extends Dialog {

    private final RestClientOrganizationService restClientOrganizationService;
    private final Runnable onSaved;
    private final BrandDto editingBrand;

    private final TextField nameField = new TextField(Messages.get("organization.brand.name"));
    private final EmailField adminEmailField = new EmailField(Messages.get("organization.brand.adminEmail"));
    private final ComboBox<String> timeZoneBox = new ComboBox<>(Messages.get("organization.brand.timeZone"));
    private final ComboBox<String> currencyBox = new ComboBox<>(Messages.get("organization.brand.currency"));
    private final RadioButtonGroup<String> menuMode = new RadioButtonGroup<>();
    private final ComboBox<BrandDto> sourceBrandBox = new ComboBox<>(Messages.get("organization.brand.sourceBrand"));
    private final Image logoPreview = new Image();
    private String logoUrl;

    public BrandDialog(RestClientOrganizationService restClientOrganizationService,
                       List<BrandDto> brands, Runnable onSaved) {
        this(restClientOrganizationService, brands, null, onSaved);
    }

    public BrandDialog(RestClientOrganizationService restClientOrganizationService,
                       List<BrandDto> brands, BrandDto editingBrand, Runnable onSaved) {
        this.restClientOrganizationService = restClientOrganizationService;
        this.onSaved = onSaved;
        this.editingBrand = editingBrand;

        setHeaderTitle(Messages.get("organization.brand.title"));
        setWidth("560px");

        nameField.setRequired(true);
        nameField.setWidthFull();

        timeZoneBox.setItems("Asia/Jakarta (WIB)", "Asia/Makassar (WITA)", "Asia/Jayapura (WIT)");
        timeZoneBox.setValue("Asia/Jakarta (WIB)");
        currencyBox.setItems("Rupiah (IDR)", "US Dollar (USD)", "Singapore Dollar (SGD)", "Malaysian Ringgit (MYR)");
        currencyBox.setValue("Rupiah (IDR)");

        adminEmailField.setWidthFull();

        menuMode.addClassName("org-choice-cards");
        menuMode.setItems("EMPTY", "COPY");
        menuMode.setValue("EMPTY");
        menuMode.setItemLabelGenerator(value -> "EMPTY".equals(value)
                ? Messages.get("organization.brand.menuEmpty")
                : Messages.get("organization.brand.menuCopy"));
        menuMode.addValueChangeListener(event ->
                sourceBrandBox.setVisible("COPY".equals(event.getValue())));

        sourceBrandBox.setItems(brands);
        sourceBrandBox.setItemLabelGenerator(BrandDto::getName);
        sourceBrandBox.setVisible(false);
        sourceBrandBox.setWidthFull();

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("20cm", 2));
        form.add(nameField, 2);
        form.add(logoField(), 2);
        form.add(timeZoneBox, currencyBox);
        form.add(adminEmailField, 2);
        form.add(menuMode, 2);
        form.add(sourceBrandBox, 2);
        add(form);

        Button cancel = new Button(Messages.get("action.cancel"), event -> close());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        Button save = new Button(Messages.get(editingBrand == null
                ? "organization.brand.create" : "organization.brand.update"), event -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(cancel, save);

        if (editingBrand != null) {
            applyEdit(editingBrand);
        }
    }

    private void applyEdit(BrandDto brand) {
        setHeaderTitle(Messages.get("organization.brand.editTitle"));
        nameField.setValue(brand.getName() == null ? "" : brand.getName());
        adminEmailField.setValue(brand.getAdminEmail() == null ? "" : brand.getAdminEmail());
        logoUrl = brand.getLogoUrl();
        if (logoUrl != null && !logoUrl.isBlank()) {
            logoPreview.setSrc(logoUrl);
            logoPreview.setVisible(true);
        }
        selectByPrefix(timeZoneBox, brand.getTimeZone());
        selectCurrency(currencyBox, brand.getCurrency());
        menuMode.setVisible(false);
        sourceBrandBox.setVisible(false);
    }

    private void selectByPrefix(ComboBox<String> box, String value) {
        if (value == null) {
            return;
        }
        box.getListDataView().getItems()
                .filter(item -> item.startsWith(value + " "))
                .findFirst()
                .ifPresent(box::setValue);
    }

    private void selectCurrency(ComboBox<String> box, String code) {
        if (code == null) {
            return;
        }
        box.getListDataView().getItems()
                .filter(item -> item.endsWith("(" + code + ")"))
                .findFirst()
                .ifPresent(box::setValue);
    }

    private FormLayout logoField() {
        MemoryBuffer buffer = new MemoryBuffer();
        Upload upload = new Upload(buffer);
        upload.setAcceptedFileTypes(MediaType.IMAGE_PNG_VALUE, "image/svg+xml");
        upload.setMaxFiles(1);
        upload.addSucceededListener(event -> {
            try {
                byte[] data = buffer.getInputStream().readAllBytes();
                ImageDto imageDto = ImageDto.builder()
                        .fileName(buffer.getFileName())
                        .mimeType(event.getMIMEType())
                        .fileStream(new java.io.ByteArrayInputStream(data))
                        .build();
                restClientOrganizationService.uploadImage(imageDto).subscribe(
                        response -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                            Map<?, ?> payload = ObjectUtil.convertValueToObject(response.getData(), Map.class);
                            Object url = payload == null ? null : payload.get("url");
                            if (url != null) {
                                logoUrl = url.toString();
                                logoPreview.setSrc(logoUrl);
                                logoPreview.setVisible(true);
                            }
                        }),
                        error -> UiUtil.safeAccess(getUI().orElse(null),
                                () -> UiUtil.error(Messages.get("organization.logo.failed"))));
            } catch (IOException e) {
                UiUtil.error(Messages.get("organization.logo.failed"));
            }
        });

        logoPreview.setWidth("64px");
        logoPreview.setHeight("64px");
        logoPreview.setVisible(false);
        logoPreview.getStyle().set("object-fit", "contain")
                .set("border-radius", "var(--lumo-border-radius-m)")
                .set("border", "1px solid var(--lumo-contrast-20pct)");

        Span hint = new Span(Messages.get("organization.brand.logoHint"));
        hint.getStyle().set("font-size", "var(--lumo-font-size-s)")
                .set("color", "var(--lumo-secondary-text-color)");

        FormLayout field = new FormLayout();
        field.add(new Span(Messages.get("organization.brand.logo")), upload, logoPreview, hint);
        field.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        return field;
    }

    private void save() {
        if (nameField.isEmpty() || nameField.getValue().trim().length() < 2) {
            nameField.setInvalid(true);
            nameField.setErrorMessage(Messages.get("validation.name.minLength"));
            return;
        }
        BrandDto dto = new BrandDto();
        dto.setName(nameField.getValue().trim());
        dto.setLogoUrl(logoUrl);
        dto.setTimeZone(stripLabel(timeZoneBox.getValue()));
        dto.setCurrency(stripLabel(currencyBox.getValue()));
        dto.setAdminEmail(adminEmailField.getValue());

        if (editingBrand == null) {
            dto.setInitialMenuMode(menuMode.getValue());
            if ("COPY".equals(menuMode.getValue()) && sourceBrandBox.getValue() != null) {
                dto.setSourceBrandId(sourceBrandBox.getValue().getId());
            }
            restClientOrganizationService.createBrand(dto).subscribe(
                    response -> success("organization.brand.created"),
                    error -> failure("organization.brand.failed"));
            return;
        }

        dto.setId(editingBrand.getId());
        restClientOrganizationService.updateBrand(dto).subscribe(
                response -> success("organization.brand.updated"),
                error -> failure("organization.brand.updateFailed"));
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

    private String stripLabel(String value) {
        if (value == null) {
            return null;
        }
        int index = value.indexOf('(');
        return index > 0 ? value.substring(0, index).trim() : value;
    }
}
