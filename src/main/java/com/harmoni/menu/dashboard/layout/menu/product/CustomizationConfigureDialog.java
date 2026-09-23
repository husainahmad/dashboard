package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.dto.CustomizationOptionDto;
import com.harmoni.menu.dashboard.dto.ProductCustomizationConfigDto;
import com.harmoni.menu.dashboard.dto.ProductCustomizationDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.layout.util.AsyncUtil;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import org.apache.commons.lang3.ObjectUtils;

import java.util.List;
import com.harmoni.menu.dashboard.layout.util.Css;

/**
 * Per-product override dialog for one attached customization: required flag,
 * min/max selection ranges and a read-only options grid. Saving writes a
 * {@link ProductCustomizationConfigDto}; "Reset to master defaults" re-sends the
 * same payload with all overrides cleared.
 */
public class CustomizationConfigureDialog extends Dialog {

    private final ProductCustomizationDto dto;
    private final ProductFormDelegate delegate;
    private final RestClientMenuService restClientMenuService;
    private final List<TierDto> tierDtos;
    private final Runnable onSaved;

    /**
     * @param dto                     the attached customization being configured
     * @param delegate                the owner form, used for UI feedback
     * @param restClientMenuService   client for the update call
     * @param tierDtos                tiers used to pick the option price column
     * @param onSaved                 callback invoked after a successful save
     *                                (reloads the section grid)
     */
    public CustomizationConfigureDialog(ProductCustomizationDto dto, ProductFormDelegate delegate,
                                        RestClientMenuService restClientMenuService,
                                        List<TierDto> tierDtos, Runnable onSaved) {
        this.dto = dto;
        this.delegate = delegate;
        this.restClientMenuService = restClientMenuService;
        this.tierDtos = tierDtos;
        this.onSaved = onSaved;

        boolean overrideActive = ObjectUtils.anyNotNull(
                dto.getRequiredOverride(), dto.getMinSelectionOverride(), dto.getMaxSelectionOverride());
        setHeaderTitle(Messages.get("dialog.header.configure", dto.getName(),
                overrideActive ? Messages.get("dialog.overrideSuffix") : ""));
        setWidth("520px");

        ComboBox<String> typeBox = new ComboBox<>(Messages.get(Messages.Keys.LABEL_SELECTION_TYPE));
        typeBox.setItems(Messages.get("label.single"), Messages.get("label.multi"));
        typeBox.setValue(dto.getSelectionType() == null ? "" : dto.getSelectionType().getLabel());
        typeBox.setEnabled(false);
        typeBox.setHelperText(Messages.get("helper.customization.managedInMaster"));

        Checkbox requiredCheckbox = new Checkbox(Messages.get(Messages.Keys.LABEL_REQUIRED));
        requiredCheckbox.setValue(Boolean.TRUE.equals(dto.getRequired()));
        Span requiredStatus = buildOverrideStatus(dto.getRequiredOverride() != null);

        NumberField minField = new NumberField(Messages.get("label.minimumSelection"));
        minField.setMin(0);
        if (dto.getMinSelection() != null) {
            minField.setValue(dto.getMinSelection().doubleValue());
        }
        Span minStatus = buildOverrideStatus(dto.getMinSelectionOverride() != null);

        NumberField maxField = new NumberField(Messages.get("label.maximumSelection"));
        maxField.setMin(0);
        if (dto.getMaxSelection() != null) {
            maxField.setValue(dto.getMaxSelection().doubleValue());
        }
        Span maxStatus = buildOverrideStatus(dto.getMaxSelectionOverride() != null);

        HorizontalLayout requiredRow = new HorizontalLayout(requiredCheckbox, requiredStatus);
        requiredRow.setAlignItems(FlexComponent.Alignment.BASELINE);

        HorizontalLayout minRow = new HorizontalLayout(minField, minStatus);
        minRow.setAlignItems(FlexComponent.Alignment.BASELINE);
        minField.setWidthFull();
        minRow.setFlexGrow(1, minField);

        HorizontalLayout maxRow = new HorizontalLayout(maxField, maxStatus);
        maxRow.setAlignItems(FlexComponent.Alignment.BASELINE);
        maxField.setWidthFull();
        maxRow.setFlexGrow(1, maxField);

        Span optionsTitle = new Span(Messages.get("dialog.optionsTitle",
                ObjectUtils.isEmpty(dto.getOptions()) ? 0 : dto.getOptions().size()));
        optionsTitle.getStyle().set(Css.FONT_WEIGHT, "600");

        Grid<CustomizationOptionDto> optionsGrid = buildConfigureOptionsGrid(dto);

        VerticalLayout content = new VerticalLayout(typeBox, requiredRow, minRow, maxRow, optionsTitle, optionsGrid);
        content.setPadding(false);
        add(content);

        Button resetButton = new Button(Messages.get("action.resetToMaster"), event -> save(null, null, null));
        Button cancelButton = new Button(Messages.get(Messages.Keys.ACTION_CANCEL), event -> close());
        Button saveButton = new Button(Messages.get(Messages.Keys.ACTION_SAVE), event -> save(
                requiredCheckbox.getValue(),
                minField.isEmpty() ? null : minField.getValue().intValue(),
                maxField.isEmpty() ? null : maxField.getValue().intValue()));
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        getFooter().add(resetButton, cancelButton, saveButton);
    }

    /**
     * Persists the configured overrides (any {@code null} clears the override,
     * reverting to the master default) and, on success, closes the dialog and
     * runs {@link #onSaved}.
     *
     * @param requiredOverride flag override, or {@code null} to inherit
     * @param minOverride      minimum-selection override, or {@code null} to inherit
     * @param maxOverride      maximum-selection override, or {@code null} to inherit
     */
    private void save(Boolean requiredOverride, Integer minOverride, Integer maxOverride) {
        ProductCustomizationConfigDto config = ProductCustomizationConfigDto.builder()
                .requiredOverride(requiredOverride)
                .minSelectionOverride(minOverride)
                .maxSelectionOverride(maxOverride)
                .sortOrder(dto.getSortOrder())
                .build();

        AsyncUtil.subscribe(restClientMenuService.updateProductCustomization(
                        delegate.getProductId(), dto.getId(), config),
                delegate.getUi(), Messages.get("notification.customization.configureFailed"),
                response -> {
                    close();
                    onSaved.run();
                });
    }

    /** Builds a small status label indicating whether the value is overridden or inherited. */
    private Span buildOverrideStatus(boolean override) {
        Span span = new Span(override ? Messages.get("label.override") : Messages.get("label.inherited"));
        span.getStyle().set("font-size", "var(--lumo-font-size-xs)")
                .set(Css.FONT_WEIGHT, "600")
                .set("color", override ? "var(--lumo-primary-color)" : "var(--lumo-secondary-text-color)");
        return span;
    }

    /**
     * Builds a read-only grid showing the customization's options, their prices
     * for the first tier, and whether each option is active.
     *
     * @param dto the customization whose options to display
     * @return a configured grid component
     */
    private Grid<CustomizationOptionDto> buildConfigureOptionsGrid(ProductCustomizationDto dto) {
        Grid<CustomizationOptionDto> grid = new Grid<>();
        grid.setSelectionMode(Grid.SelectionMode.NONE);
        grid.setAllRowsVisible(true);
        grid.removeAllColumns();
        grid.addColumn(CustomizationOptionDto::getName).setHeader(Messages.get("grid.header.option"));
        grid.addColumn(this::getOptionPrice).setHeader(Messages.get(Messages.Keys.GRID_HEADER_PRICE));
        grid.addComponentColumn(this::renderActiveColumn).setHeader(Messages.get(Messages.Keys.GRID_HEADER_ACTIVE)).setWidth("90px");
        if (ObjectUtils.isNotEmpty(dto.getOptions())) {
            grid.setItems(dto.getOptions());
        }
        return grid;
    }

    /**
     * Renders the "Active" column for a customization option. If the option's
     * status is "ACTIVE", it shows a green checkmark; otherwise, it shows a dash.
     *
     * @param option the customization option to render
     * @return a component representing the active status
     */
    private Component renderActiveColumn(CustomizationOptionDto option) {
        if ("ACTIVE".equalsIgnoreCase(option.getStatus())) {
            Icon check = new Icon(VaadinIcon.CHECK);
            check.setColor("var(--lumo-success-color)");
            return check;
        }
        return new Span("-");
    }

    /**
     * Retrieves the price of a customization option for the first tier. If the
     * option has no price for the first tier, it returns a dash ("-").
     *
     * @param option the customization option whose price to retrieve
     * @return a formatted price string or "-" if no price is available
     */
    private String getOptionPrice(CustomizationOptionDto option) {
        TierDto firstTier = ObjectUtils.isEmpty(tierDtos) ? null : tierDtos.getFirst();
        if (firstTier == null || ObjectUtils.isEmpty(option.getTierPrices())) {
            return "-";
        }
        return option.getTierPrices().stream()
                .filter(tierPriceDto -> firstTier.getId().equals(tierPriceDto.getTierId()))
                .map(tierPriceDto -> UiUtil.rupiah(tierPriceDto.getPrice()))
                .findFirst()
                .orElse("-");
    }
}