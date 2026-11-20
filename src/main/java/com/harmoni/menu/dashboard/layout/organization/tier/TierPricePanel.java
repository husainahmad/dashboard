package com.harmoni.menu.dashboard.layout.organization.tier;

import com.harmoni.menu.dashboard.dto.ProductDto;
import com.harmoni.menu.dashboard.dto.SkuTierPriceDto;
import com.harmoni.menu.dashboard.dto.SkuDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.layout.component.PaginationBar;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.util.Messages;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Harga panel of the Tier page: lists the SKUs of the brand with their brand
 * price and the price under the selected price tier.
 *
 * <p>Products are read one cursor page at a time. The cursor is the id of the
 * last product on the page, so an insert between two requests cannot shift the
 * rows and make the next page repeat one. Edits are kept in {@link #changedRows}
 * so they survive moving between pages and are saved together.</p>
 */
public class TierPricePanel extends TierPanel {

    private static final int PAGE_SIZE = 10;

    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final RestClientMenuService restClientMenuService;
    private final TierDto tier;
    private final Runnable onSaved;

    private final TextField search = new TextField();
    private final NumberField markup = new NumberField();
    private final Button applyMarkup = new Button(Messages.get("tier.price.applyMarkup"));
    private final Grid<TierSkuRow> grid = new Grid<>();
    private final List<TierSkuRow> rows = new ArrayList<>();
    private final Map<Integer, TierSkuRow> changedRows = new LinkedHashMap<>();
    private final List<Integer> cursorHistory = new ArrayList<>();
    private final PaginationBar paginationBar = new PaginationBar(this::previousPage, this::nextPage);

    private Integer beforeId;
    private Integer nextCursor;
    private boolean hasMore;
    private int currentPage = 1;
    private boolean loaded;

    public TierPricePanel(AsyncRestClientMenuService asyncRestClientMenuService,
                          RestClientMenuService restClientMenuService,
                          TierDto tier, Runnable onSaved, boolean readOnly) {
        super(readOnly);
        this.asyncRestClientMenuService = asyncRestClientMenuService;
        this.restClientMenuService = restClientMenuService;
        this.tier = tier;
        this.onSaved = onSaved;

        configureToolbar();
        configureGrid();
        add(toolbar(), grid, paginationBar);
        setFlexGrow(1, grid);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (!loaded) {
            loadPage();
        }
    }

    private void configureToolbar() {
        search.setLabel(Messages.get(Messages.Keys.LABEL_SEARCH));
        search.setPlaceholder(Messages.get("tier.price.search"));
        search.setClearButtonVisible(true);
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.addValueChangeListener(event -> {
            currentPage = 1;
            beforeId = null;
            cursorHistory.clear();
            loadPage();
        });

        markup.setValue(10d);
        markup.setMin(-100);
        markup.setMax(1000);
        markup.setStep(1);
        markup.setWidth("7em");
        markup.setSuffixComponent(new Span("%"));
    }

    private HorizontalLayout toolbar() {
        applyMarkup.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        applyMarkup.addClickListener(event -> applyMarkup());

        HorizontalLayout markupRow = new HorizontalLayout(
                new Span(Messages.get("tier.price.bulkMarkup")), markup, applyMarkup);
        markupRow.setAlignItems(FlexComponent.Alignment.CENTER);
        markupRow.setSpacing(true);

        HorizontalLayout toolbar = new HorizontalLayout(search, markupRow);
        toolbar.addClassName("tier-price-toolbar");
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.setFlexGrow(1, search);
        if (isReadOnly()) {
            markup.setVisible(false);
            applyMarkup.setVisible(false);
        }
        return toolbar;
    }

    private void configureGrid() {
        grid.addClassName("tier-price-grid");
        grid.setSizeFull();
        grid.addColumn(TierSkuRow::getName)
                .setHeader(Messages.get("tier.price.menu"))
                .setFlexGrow(2);
        grid.addComponentColumn(this::priceField)
                .setHeader(Messages.get("tier.price.tier"))
                .setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.END);
    }

    private NumberField priceField(TierSkuRow row) {
        NumberField field = new NumberField();
        field.addClassName("money-field");
        field.setWidth("9em");
        field.setMin(0);
        field.setStep(500);
        field.setValue(row.getTierPrice());
        if (isReadOnly()) {
            field.setReadOnly(true);
            return field;
        }
        field.addValueChangeListener(event -> {
            row.setTierPrice(event.getValue());
            if (row.isChanged()) {
                changedRows.put(row.getSkuId(), row);
            } else {
                changedRows.remove(row.getSkuId());
            }
            markDirty();
        });
        return field;
    }

    private void loadPage() {
        String term = search.getValue() == null ? "" : search.getValue().trim();
        asyncRestClientMenuService.getBrandTierCursorAsync(result ->
                UiUtil.safeAccess(getUI().orElse(null), () -> {
                    rows.clear();
                    Object data = result == null ? null : result.get("products");
                    if (data instanceof List<?> list) {
                        for (Object item : list) {
                            ProductDto product = ObjectUtil.convertValueToObject(item, ProductDto.class);
                            addProductRows(product);
                        }
                    }
                    hasMore = result != null && Boolean.TRUE.equals(result.get("hasMore"));
                    nextCursor = result == null || result.get("nextCursor") == null
                            ? null : Integer.valueOf(result.get("nextCursor").toString());
                    loaded = true;
                    grid.setItems(rows);
                    paginationBar.updateCursor(currentPage, !cursorHistory.isEmpty(), hasMore);
                }), error -> UiUtil.safeAccess(getUI().orElse(null),
                        () -> UiUtil.error(Messages.get("tier.price.loadFailed"))),
                tier.getBrandId(), tier.getId(), beforeId, term, PAGE_SIZE);
    }

    private void addProductRows(ProductDto product) {
        if (product == null || product.getSkuDtos() == null) {
            return;
        }
        for (SkuDto sku : product.getSkuDtos()) {
            Double tierPrice = priceFor(sku, tier.getId());
            String name = product.getName();
            if (sku.getName() != null && !sku.getName().isBlank()
                    && !sku.getName().equalsIgnoreCase(product.getName())) {
                name = product.getName() + " - " + sku.getName();
            }
            TierSkuRow row = new TierSkuRow(sku.getId(), name, tierPrice);
            TierSkuRow changed = changedRows.get(sku.getId());
            if (changed != null) {
                row.setTierPrice(changed.getTierPrice());
            }
            rows.add(row);
        }
    }

    private Double priceFor(SkuDto sku, Integer tierId) {
        if (tierId == null) {
            return null;
        }
        SkuTierPriceDto scoped = sku.getTierPriceDto();
        if (scoped != null && Objects.equals(scoped.getTierId(), tierId)) {
            return scoped.getPrice();
        }
        if (sku.getSkuTierPriceDtos() == null) {
            return null;
        }
        return sku.getSkuTierPriceDtos().stream()
                .filter(price -> Objects.equals(price.getTierId(), tierId))
                .map(SkuTierPriceDto::getPrice)
                .findFirst()
                .orElse(null);
    }

    private void previousPage() {
        if (!cursorHistory.isEmpty()) {
            beforeId = cursorHistory.remove(cursorHistory.size() - 1);
            currentPage--;
            loadPage();
        }
    }

    private void nextPage() {
        if (hasMore && nextCursor != null) {
            cursorHistory.add(beforeId);
            beforeId = nextCursor;
            currentPage++;
            loadPage();
        }
    }

    private void applyMarkup() {
        if (isReadOnly()) {
            return;
        }
        Double percent = markup.getValue();
        if (percent == null) {
            return;
        }
        for (TierSkuRow row : rows) {
            if (row.getTierPrice() != null) {
                row.setTierPrice(row.getTierPrice() * (1 + percent / 100d));
                if (row.isChanged()) {
                    changedRows.put(row.getSkuId(), row);
                } else {
                    changedRows.remove(row.getSkuId());
                }
            }
        }
        grid.getDataProvider().refreshAll();
        markDirty();
    }

    @Override
    public void save() {
        if (isReadOnly()) {
            return;
        }
        List<Mono<?>> saves = new ArrayList<>();
        for (TierSkuRow row : changedRows.values()) {
            if (row.getTierPrice() == null) {
                continue;
            }
            saves.add(restClientMenuService.updateSkuTierPrice(
                    row.getSkuId(), tier.getId(), row.getTierPrice()));
        }
        if (saves.isEmpty()) {
            UiUtil.success(Messages.get("tier.price.noChanges"));
            return;
        }
        Mono.when(saves).subscribe(
                ignored -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    changedRows.clear();
                    UiUtil.success(Messages.get("tier.price.saved"));
                    if (onSaved != null) {
                        onSaved.run();
                    }
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null),
                        () -> UiUtil.error(Messages.get("tier.price.saveFailed"))));
    }
}
