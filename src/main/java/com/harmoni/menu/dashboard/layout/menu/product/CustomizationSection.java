package com.harmoni.menu.dashboard.layout.menu.product;

import com.fasterxml.jackson.core.type.TypeReference;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.CustomizationDto;
import com.harmoni.menu.dashboard.dto.ProductCustomizationDto;
import com.harmoni.menu.dashboard.dto.ProductCustomizationReplaceDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.layout.util.AsyncUtil;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Owns the customization grid of the product form: attach list, add/search
 * dialog, per-product configure dialog, and the merge + delete logic. A product
 * that has not been saved yet keeps the attachment locally; a saved product
 * talks to the customization endpoints directly.
 */
public class CustomizationSection {

    private final RestClientMenuService restClientMenuService;
    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final BrandDto brandDto;
    private final List<TierDto> tierDtos;
    private final SkuSection skuSection;
    private final ProductFormDelegate delegate;

    private final List<ProductCustomizationDto> productCustomizations = new ArrayList<>();
    private final Grid<ProductCustomizationDto> grid = new Grid<>();

    /**
     * Creates the section that manages which customizations are attached to the
     * product and how they are configured.
     *
     * @param restClientMenuService       blocking REST client for customization CRUD
     * @param asyncRestClientMenuService  async client for the search dialog
     * @param brandDto                    brand context used for unsaved attachments
     * @param tierDtos                    tiers used by the configure dialog options grid
     * @param skuSection                  consulted before allowing attachments
     * @param delegate                    owner form, used for UI feedback
     */
    public CustomizationSection(RestClientMenuService restClientMenuService,
                                AsyncRestClientMenuService asyncRestClientMenuService,
                                BrandDto brandDto, List<TierDto> tierDtos,
                                SkuSection skuSection, ProductFormDelegate delegate) {
        this.restClientMenuService = restClientMenuService;
        this.asyncRestClientMenuService = asyncRestClientMenuService;
        this.brandDto = brandDto;
        this.tierDtos = tierDtos;
        this.skuSection = skuSection;
        this.delegate = delegate;
        configureGrid();
    }

    /**
     * @return the toolbar + grid block, placed under the "SKU &amp; Pricing" section of the form
     */
    public VerticalLayout getLayout() {
        Button addButton = new Button("Add Customization");
        addButton.setIcon(new Icon(VaadinIcon.PLUS));
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.addClickListener(event -> onAddCustomization());

        HorizontalLayout header = new HorizontalLayout(addButton);
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        header.setAlignItems(FlexComponent.Alignment.CENTER);

        VerticalLayout section = new VerticalLayout();
        section.addClassName("customization-section");
        section.setPadding(false);
        section.setSpacing(false);
        section.add(header, grid);
        return section;
    }

    /**
     * @return the currently attached customizations; for an unsaved product this
     *         is also the data source that ends up in the save payload
     */
    public List<ProductCustomizationDto> getProductCustomizations() {
        return productCustomizations;
    }

    /**
     * Fetches the customization attachments for a persisted product and renders
     * them into the grid.
     *
     * @param productId id of the saved product
     */
    public void load(Integer productId) {
        AsyncUtil.subscribe(restClientMenuService.getProductCustomizations(productId),
                delegate.getUi(), "Failed to load customizations",
                response -> render(customizationsFrom(response)));
    }

    /**
     * Re-fetches the attachments for the currently edited product, where saved.
     */
    public void reload() {
        Integer productId = delegate.getProductId();
        if (productId != null) {
            load(productId);
        }
    }

    private List<ProductCustomizationDto> customizationsFrom(RestAPIResponse response) {
        return ObjectUtils.isEmpty(response.getData())
                ? new ArrayList<>()
                : ObjectUtil.convertObjectToObject(response.getData(), new TypeReference<>() {
        });
    }

    private void render(List<ProductCustomizationDto> customizations) {
        productCustomizations.clear();
        productCustomizations.addAll(ObjectUtils.isEmpty(customizations) ? new ArrayList<>() : customizations);
        grid.getDataProvider().refreshAll();
    }

    private void configureGrid() {
        grid.setSelectionMode(Grid.SelectionMode.NONE);
        grid.setAllRowsVisible(true);
        grid.setEmptyStateText("No customizations added yet");
        grid.addClassName("customization-grid");
        grid.addColumn(ProductCustomizationDto::getName).setHeader("Name");
        grid.addComponentColumn(this::renderRequiredColumn).setHeader("Required").setWidth("100px");
        grid.addColumn(dto -> dto.getSelectionType() == null ? "" : dto.getSelectionType().getLabel())
                .setHeader("Type");
        grid.addColumn(this::renderMinMaxText).setHeader("Min/Max").setWidth("110px");
        grid.addComponentColumn(this::renderOptionsColumn).setHeader("Options").setWidth("90px");
        grid.addComponentColumn(this::renderCustomizationActions).setWidth("120px");
        grid.setItems(productCustomizations);
    }

    private void onAddCustomization() {
        if (!skuSection.hasDefinedSku()) {
            delegate.showErrorDialog("Define the SKU before adding customizations");
            return;
        }
        new CustomizationAddDialog(this, delegate, asyncRestClientMenuService, brandDto).open();
    }

    /**
     * Merges the customizations picked in the add dialog into the attachments.
     * For an unsaved product the merge stays local and is carried in the save
     * payload; for a saved product the full replaced list is sent via
     * {@code saveProductCustomizations} and the grid reloads.
     *
     * @param selected the customizations the user picked in the dialog
     * @param dialog   the dialog to close once the action succeeds
     */
    void onAddSelected(Set<CustomizationDto> selected, Dialog dialog) {
        List<Integer> merged = new ArrayList<>(productCustomizations.stream()
                .map(ProductCustomizationDto::getCustomizationId).collect(Collectors.toList()));
        selected.stream()
                .map(CustomizationDto::getId)
                .filter(id -> !merged.contains(id))
                .forEach(merged::add);

        if (delegate.getProductId() == null) {
            Map<Integer, CustomizationDto> selectedById = selected.stream()
                    .collect(Collectors.toMap(CustomizationDto::getId, c -> c, (first, second) -> first));
            merged.stream()
                    .filter(id -> productCustomizations.stream()
                            .noneMatch(pc -> id.equals(pc.getCustomizationId())))
                    .map(selectedById::get)
                    .filter(Objects::nonNull)
                    .map(this::toProductCustomizationDto)
                    .forEach(productCustomizations::add);
            dialog.close();
            grid.getDataProvider().refreshAll();
            delegate.showNotification("Customizations will be saved with the product");
            return;
        }

        Integer productId = delegate.getProductId();
        AsyncUtil.subscribe(restClientMenuService.saveProductCustomizations(productId,
                        ProductCustomizationReplaceDto.builder().customizationIds(merged).build()),
                delegate.getUi(), "Failed to update customizations",
                response -> {
                    dialog.close();
                    reload();
                    delegate.showNotification("Customizations updated");
                });
    }

    private void onDeleteCustomization(ProductCustomizationDto dto) {
        if (delegate.getProductId() == null) {
            productCustomizations.removeIf(pc -> Objects.equals(pc.getCustomizationId(), dto.getCustomizationId()));
            grid.getDataProvider().refreshAll();
            delegate.showNotification("Customization removed");
            return;
        }

        Integer productId = delegate.getProductId();
        AsyncUtil.subscribe(restClientMenuService.deleteProductCustomization(productId, dto.getId()),
                delegate.getUi(), "Failed to remove customization",
                response -> {
                    reload();
                    delegate.showNotification("Customization removed");
                });
    }

    /**
     * Converts a master {@link CustomizationDto} into the per-product attachment
     * draft, seeding the sort order past the currently attached items.
     */
    private ProductCustomizationDto toProductCustomizationDto(CustomizationDto customization) {
        return ProductCustomizationDto.builder()
                .customizationId(customization.getId())
                .name(customization.getName())
                .description(customization.getDescription())
                .selectionType(customization.getSelectionType())
                .required(customization.getRequired())
                .minSelection(customization.getMinimumSelection())
                .maxSelection(customization.getMaximumSelection())
                .brandId(brandDto.getId())
                .sortOrder(productCustomizations.size() + 1)
                .options(customization.getCustomizationOptions())
                .build();
    }

    private void openConfigureDialog(ProductCustomizationDto dto) {
        if (delegate.getProductId() == null) {
            UiUtil.show("Save the product first, then open the settings to adjust each customization",
                    NotificationVariant.LUMO_PRIMARY, 4000);
            return;
        }
        new CustomizationConfigureDialog(dto, delegate, restClientMenuService, tierDtos, this::reload).open();
    }

    private Component renderRequiredColumn(ProductCustomizationDto dto) {
        HorizontalLayout wrapper = new HorizontalLayout();
        wrapper.setWidthFull();
        wrapper.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        if (Boolean.TRUE.equals(dto.getRequired())) {
            Icon check = new Icon(VaadinIcon.CHECK);
            check.setColor("var(--lumo-primary-color)");
            wrapper.add(check);
        }
        return wrapper;
    }

    private Component renderOptionsColumn(ProductCustomizationDto dto) {
        HorizontalLayout wrapper = new HorizontalLayout();
        wrapper.setWidthFull();
        wrapper.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        int count = ObjectUtils.isEmpty(dto.getOptions()) ? 0 : dto.getOptions().size();
        wrapper.add(new Span(String.valueOf(count)));
        return wrapper;
    }

    private String renderMinMaxText(ProductCustomizationDto dto) {
        String min = dto.getMinSelection() == null ? "0" : String.valueOf(dto.getMinSelection());
        String max = dto.getMaxSelection() == null ? "n" : String.valueOf(dto.getMaxSelection());
        return min + " / " + max;
    }

    private Component renderCustomizationActions(ProductCustomizationDto dto) {
        HorizontalLayout actions = new HorizontalLayout();
        actions.setSpacing(false);

        Button configureButton = new Button(new Icon(VaadinIcon.COG));
        configureButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        configureButton.setTooltipText("Configuration");
        configureButton.addClickListener(event -> openConfigureDialog(dto));

        actions.add(configureButton, UiUtil.deleteButton(event -> onDeleteCustomization(dto)));
        return actions;
    }
}