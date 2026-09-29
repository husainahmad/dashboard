package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.dto.SkuDto;
import com.harmoni.menu.dashboard.dto.SkuTierPriceDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.harmoni.menu.dashboard.layout.util.Css;

/**
 * Owns the SKU/tier-price editing grid of the product form: row rendering, add,
 * delete and the draft-to-payload conversion. Edits are written directly into
 * {@link SkuTreeItem}, which is the single source of truth.
 */
public class SkuSection {

    private static final int MIN_SKUS = 1;

    private final List<TierDto> tierDtos;
    private final ProductFormDelegate delegate;
    private final List<SkuTreeItem> skuItems = new ArrayList<>();
    private final ListDataProvider<SkuTreeItem> dataProvider;

    @Getter
    private final Grid<SkuTreeItem> grid = new Grid<>();

    /**
     * Creates the section around the tier price columns and the grid that
     * renders the {@link SkuTreeItem} drafts.
     *
     * @param tierDtos the price tiers shown as columns; prices are keyed by
     *                 tier id, so this list must stay stable for the life of
     *                 the section
     * @param delegate the owner form, used for feedback (e.g. delete guard)
     */
    public SkuSection(List<TierDto> tierDtos, ProductFormDelegate delegate) {
        this.tierDtos = tierDtos;
        this.delegate = delegate;
        this.dataProvider = new ListDataProvider<>(skuItems);
        grid.setDataProvider(dataProvider);
        grid.setSelectionMode(Grid.SelectionMode.NONE);
        grid.setEmptyStateText(Messages.get("grid.empty.sku"));
        configureGrid();
    }

    /** Replaces all rows with the given product SKUs, or with a single empty row when null. */
    public void load(List<SkuDto> skuDtos) {
        skuItems.clear();
        if (skuDtos != null && !skuDtos.isEmpty()) {
            skuDtos.forEach(this::addSku);
        } else {
            addSku(null);
        }
        dataProvider.refreshAll();
    }

    /**
     * Appends a new editable SKU row to the model and refreshes the grid.
     *
     * @param skuDto the existing SKU to seed the row with, or {@code null} to
     *               start from an empty row
     */
    public void addSku(SkuDto skuDto) {
        Map<Integer, Double> prices = new HashMap<>();
        tierDtos.forEach(tier -> prices.put(tier.getId(), priceByTier(skuDto, tier)));

        SkuTreeItem item = SkuTreeItem.builder()
                .skuId(skuDto == null ? null : skuDto.getId())
                .skuName(skuDto == null ? "" : skuDto.getName())
                .skuDesc(skuDto == null ? "" : skuDto.getDescription())
                .tierPrices(prices)
                .build();

        skuItems.add(item);
        dataProvider.refreshAll();
        delegate.onContentChanged();
    }

    /**
     * @return the toolbar holding the "Add SKU" action, to be placed above the grid
     */
    public HorizontalLayout getToolbar() {
        Button addButton = UiUtil.addButton(Messages.get("action.addSku"), event -> addSku(null));
        HorizontalLayout toolbar = new HorizontalLayout(addButton);
        toolbar.addClassName(Css.TOOLBAR);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        return toolbar;
    }

    /**
     * @return {@code true} when at least one row has a non-blank SKU name,
     *         used as a prerequisite before attaching customizations
     */
    public boolean hasDefinedSku() {
        return skuItems.stream().anyMatch(item -> item.getSkuName() != null && !item.getSkuName().trim().isEmpty());
    }

    /**
     * The rows as they currently stand, unsaved edits included.
     *
     * <p>For read-only consumers such as the product preview. Unmodifiable on purpose: a
     * caller that only wants to look at the variants should not be handed the live list
     * to alter, and going through {@link #toSkuDtos()} instead would lose the tier
     * identities the preview needs to lay the matrix out.</p>
     *
     * @return the live variant rows, never {@code null}
     */
    public List<SkuTreeItem> skuItems() {
        return List.copyOf(skuItems);
    }

    /** Converts the editable rows into the payload expected by the product API. */
    public List<SkuDto> toSkuDtos() {
        List<SkuDto> skuDtos = new ArrayList<>();
        skuItems.forEach(item -> {
            SkuDto skuDto = new SkuDto();
            skuDto.setId(item.getSkuId());
            skuDto.setName(item.getSkuName() == null ? "" : item.getSkuName());
            skuDto.setDescription(item.getSkuDesc() == null ? "" : item.getSkuDesc());
            skuDto.setActive(true);

            List<SkuTierPriceDto> tierPrices = new ArrayList<>();
            item.getTierPrices().forEach((tierId, price) -> {
                SkuTierPriceDto skuTierPriceDto = new SkuTierPriceDto();
                skuTierPriceDto.setSkuId(skuDto.getId());
                skuTierPriceDto.setTierId(tierId);
                skuTierPriceDto.setPrice(price != null ? price : 0.0);
                tierPrices.add(skuTierPriceDto);
            });
            skuDto.setSkuTierPriceDtos(tierPrices);
            skuDtos.add(skuDto);
        });
        return skuDtos;
    }

    /**
     * Configures the grid to render the SKU name, description, tier prices and
     * delete button. The tier price columns are built dynamically from the
     * {@link #tierDtos} list, which must remain stable for the life of the section.
     */
    private void configureGrid() {
        grid.addClassName("sku-grid");
        grid.removeAllColumns();
        grid.addComponentColumn(this::applySkuNameTextField).setHeader(Messages.get(Messages.Keys.GRID_HEADER_NAME)).setFlexGrow(1);
        grid.addComponentColumn(this::applySkuDescTextField).setHeader(Messages.get("grid.header.description")).setFlexGrow(1);
        tierDtos.forEach(tier ->
                grid.addComponentColumn(item -> applyTierPriceField(item, tier))
                        .setHeader(tier.getName().toUpperCase())
                        .setWidth("116px"));
        grid.addComponentColumn(this::applyButtonDelete).setWidth("72px");
        grid.setAllRowsVisible(true);
    }

    /**
     * @param skuDto  the SKU to look up
     * @param tierDto the tier to look up
     * @return the price for the given SKU and tier, or 0.0 when no price is set
     */
    private static Double priceByTier(SkuDto skuDto, TierDto tierDto) {
        if (skuDto == null || tierDto == null || skuDto.getSkuTierPriceDtos() == null) {
            return 0.0;
        }
        return skuDto.getSkuTierPriceDtos().stream()
                .filter(skuTierPriceDto -> tierDto.getId().equals(skuTierPriceDto.getTierId()))
                .map(SkuTierPriceDto::getPrice)
                .findFirst()
                .orElse(0.0);
    }

    /**
     * Removes the given row from the model, unless it is the last remaining row,
     * in which case a warning is shown and the action is rejected.
     *
     * @param skuTreeItem the row to remove
     */
    private void removeSku(SkuTreeItem skuTreeItem) {
        if (skuItems.size() <= MIN_SKUS) {
            delegate.showErrorDialog(Messages.get("notification.sku.deleteRejected"));
            return;
        }
        skuItems.remove(skuTreeItem);
        dataProvider.refreshAll();
        delegate.onContentChanged();
    }

    /**
     * @param skuTreeItem the row being rendered
     * @return a delete button that removes the given row from the model
     */
    private Component applyButtonDelete(SkuTreeItem skuTreeItem) {
        return UiUtil.deleteButton(event -> removeSku(skuTreeItem));
    }

    /**
     * @param skuTreeItem the row being rendered
     * @return a text field that edits the SKU name for the given row
     */
    private TextField applySkuNameTextField(SkuTreeItem skuTreeItem) {
        TextField textField = new TextField();
        textField.setValue(skuTreeItem.getSkuName() == null ? "" : skuTreeItem.getSkuName());
        textField.addValueChangeListener(changeEvent -> {
            skuTreeItem.setSkuName(changeEvent.getValue() == null ? "" : changeEvent.getValue());
            delegate.onContentChanged();
        });
        return textField;
    }

    /**
     * @param skuTreeItem the row being rendered
     * @return a text field that edits the SKU description for the given row
     */
    private TextField applySkuDescTextField(SkuTreeItem skuTreeItem) {
        TextField textField = new TextField();
        textField.setValue(skuTreeItem.getSkuDesc() == null ? "" : skuTreeItem.getSkuDesc());
        textField.addValueChangeListener(changeEvent ->
                skuTreeItem.setSkuDesc(changeEvent.getValue() == null ? "" : changeEvent.getValue()));
        return textField;
    }

    /**
     * @param skuTreeItem the row being rendered
     * @param tier        the price tier for the column being rendered
     * @return a number field that edits the price for the given SKU and tier
     */
    private NumberField applyTierPriceField(SkuTreeItem skuTreeItem, TierDto tier) {
        NumberField numberField = new NumberField();
        numberField.setValue(skuTreeItem.getTierPrices().get(tier.getId()) == null
                ? 0.0 : skuTreeItem.getTierPrices().get(tier.getId()));
        numberField.setWidth("104px");
        numberField.addClassName("tier-price-field");
        numberField.addValueChangeListener(changeEvent -> {
            skuTreeItem.getTierPrices().put(tier.getId(),
                    changeEvent.getValue() == null ? 0.0 : changeEvent.getValue());
            delegate.onContentChanged();
        });
        return numberField;
    }
}