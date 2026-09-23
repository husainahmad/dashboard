package com.harmoni.menu.dashboard.layout.menu.product;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.dto.*;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.event.product.ProductDeleteEventListener;
import com.harmoni.menu.dashboard.layout.AbstractListView;
import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.enums.ProductItemType;
import com.harmoni.menu.dashboard.layout.util.GridSkeleton;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.treegrid.ExpandEvent;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import com.harmoni.menu.dashboard.layout.util.Css;

/**
 * Product browsing and management view.
 *
 * <p>Shows products as a hierarchical {@link TreeGrid} (product rows expanded
 * into their SKUs), with category, brand and tier filters, pagination,
 * search and per-product edit/delete actions. Opens the edit form in the tab
 * supplied by the hosting {@link ProductTabs} view and refreshes the tree from
 * the menu service.
 */
@RequiredArgsConstructor
@UIScope
@PreserveOnRefresh
@Route(value = "product-list", layout = MainLayout.class)
@PageTitle("Product | POSHarmoni")
@org.springframework.stereotype.Component
@Slf4j
public class ProductListView extends AbstractListView implements BroadcastMessageService {

    TreeGrid<ProductTreeItem> productDtoGrid = new TreeGrid<>(ProductTreeItem.class);

    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final RestClientMenuService restClientMenuService;
    private final AccessService accessService;

    @Getter
    private final Tab defaultTab;

    ComboBox<TierDto> tierDtoComboBox = new ComboBox<>();
    ComboBox<BrandDto> brandDtoComboBox = new ComboBox<>();
    ComboBox<CategoryDto> categoryDtoComboBox = new ComboBox<>();
    transient List<CategoryDto> categoryDtos = new ArrayList<>();

    transient List<BrandDto> brandDtos = new ArrayList<>();
    transient List<TierDto> tierDtos = new ArrayList<>();
    private final GridSkeleton gridSkeleton = new GridSkeleton(15);

    transient ProductTreeItem expandTreeItem;

    /** Session key that stores the last used product filters. */
    static final String PRODUCT_FILTERS_KEY = "product-list-filters";

    /** Fields that currently hold keyboard focus while the user types. */
    private final transient Set<Component> typingFields = new HashSet<>();

    /**
     * Snapshot of the product filters remembered across navigations so the
     * view always comes back to where the user left off (circular default).
     */
    private record ProductFilters(Integer brandId, Integer categoryId, Integer tierId,
                                  String search, int page) {
    }

    /**
     * Saves the current filter values into the session for the next visit.
     */
    private void persistFilters() {
        if (ui == null) {
            return;
        }
        ui.getSession().setAttribute(PRODUCT_FILTERS_KEY, new ProductFilters(
                ObjectUtils.isEmpty(brandDtoComboBox.getValue()) ? null : brandDtoComboBox.getValue().getId(),
                ObjectUtils.isEmpty(categoryDtoComboBox.getValue()) ? null : categoryDtoComboBox.getValue().getId(),
                ObjectUtils.isEmpty(tierDtoComboBox.getValue()) ? null : tierDtoComboBox.getValue().getId(),
                filterText.getValue(), currentPage));
    }

    /**
     * Returns the filters saved in this session, or {@code null} on first use.
     */
    private ProductFilters savedFilters() {
        if (ui == null) {
            return null;
        }
        Object saved = ui.getSession().getAttribute(PRODUCT_FILTERS_KEY);
        return saved instanceof ProductFilters filters ? filters : null;
    }

    private void renderLayout() {
        setSizeFull();
        setPadding(false);
        configureGrid();
        add(getContent(), getPaginationFooter());
        fetchBrands();
    }

    private void initTempOptions() {
        if (brandDtos.stream().noneMatch(dto -> Objects.equals(dto.getId(), -1))) {
            brandDtos.add(getTempBrandDto());
            categoryDtos.add(getTempCategoryDto());
            tierDtos.add(getTempTierDtp());
        }
    }

    /**
     * Applies the edit and delete buttons to the product rows in the tree grid.
     * Only products (not SKUs) have these actions.
     *
     * @param productTreeItem the product tree item to which the buttons are applied
     * @return a horizontal layout containing the buttons, or null if the item is not a product
     */
    private HorizontalLayout applyButton(ProductTreeItem productTreeItem) {
        if (productTreeItem.getProductItemType().equals(ProductItemType.PRODUCT)) {
            HorizontalLayout horizontalLayout = new HorizontalLayout();
            horizontalLayout.add(UiUtil.editButton(event -> editProduct(productTreeItem)));
            horizontalLayout.add(UiUtil.deleteButton(
                    new ProductDeleteEventListener(restClientMenuService, productTreeItem)));
            return horizontalLayout;
        }
        return null;
    }

    /**
     * Creates the pagination footer with "Previous" and "Next" buttons and page info.
     *
     * @return a horizontal layout containing the pagination controls
     */
    private HorizontalLayout getPaginationFooter() {
        return paginationFooter(() -> {
            if (currentPage > 1) {
                currentPage--;
                persistFilters();
                fetchProducts(getCategoryId(), brandDtoComboBox.getValue().getId(), filterText.getValue());
            }
        }, () -> {
            if (currentPage < totalPages) {
                currentPage++;
                persistFilters();
                fetchProducts(getCategoryId(), brandDtoComboBox.getValue().getId(), filterText.getValue());
            }
        });
    }

    private void configureGrid() {
        productDtoGrid.setSizeFull();
        productDtoGrid.setEmptyStateText(Messages.get("grid.empty.products"));
        refreshGridColumns();
        productDtoGrid.addExpandListener(this::onComponentEventExpandListener);
    }

    /**
     * Rebuilds all columns in display order: name hierarchy, category, one
     * price column per price tier, and the action column. Called once on
     * attach and again whenever the tier list finishes loading so the matrix
     * columns stay in sync with the brand's price tiers.
     */
    private void refreshGridColumns() {
        productDtoGrid.removeAllColumns();
        productDtoGrid.addHierarchyColumn(ProductTreeItem::getName).setHeader(Messages.get(Messages.Keys.GRID_HEADER_NAME));
        productDtoGrid.addColumn(ProductTreeItem::getCategoryName).setHeader(Messages.get("grid.header.category"));
        addTierPriceColumns();
        productDtoGrid.addComponentColumn(this::applyButton).setHeader(Messages.get(Messages.Keys.GRID_HEADER_ACTION));
        productDtoGrid.getColumns().forEach(productDtoColumn -> productDtoColumn.setAutoWidth(true));
    }

    /**
     * Adds one editable price column per price tier so an expanded SKU row
     * shows the whole tier-price matrix at a glance and every price can be
     * changed in place. The synthetic "All" filter option (tier id -1) is
     * skipped. Tier prices come from the SKU payload already, so no extra
     * request is needed.
     */
    private void addTierPriceColumns() {
        if (tierDtos == null) {
            return;
        }
        tierDtos.stream()
                .filter(tier -> tier.getId() != null && tier.getId() > 0)
                .forEach(tier -> productDtoGrid.addComponentColumn(item -> tierPriceCell(item, tier.getId()))
                        .setKey("tier-price-".concat(String.valueOf(tier.getId())))
                        .setHeader(tier.getName().toUpperCase()));
    }

    /**
     * Builds the price cell for a tree item under the given tier: an editable
     * number field for SKU rows and a placeholder for product rows. The value
     * is committed to the backend on Enter or blur, the field is disabled
     * while the request is in flight, and it reverts with an error toast when
     * the save fails.
     *
     * @param productTreeItem the item (product or SKU node)
     * @param tierId          the price tier id
     * @return the editable price component for the cell
     */
    private Component tierPriceCell(ProductTreeItem productTreeItem, Integer tierId) {
        if (!isSkuRow(productTreeItem)) {
            return new Text("\u2013");
        }
        NumberField priceField = newPriceField();
        UiUtil.guardShortcutField(typingFields, priceField);
        AtomicReference<Double> committed = new AtomicReference<>(tierPrice(productTreeItem, tierId));
        Double initial = committed.get();
        if (initial != null) {
            priceField.setValue(initial);
        }
        final boolean[] saving = {false};
        wirePriceDirtyTracking(priceField, saving);
        wirePriceCommit(productTreeItem, tierId, priceField, committed, saving);
        return priceField;
    }

    private boolean isSkuRow(ProductTreeItem productTreeItem) {
        return productTreeItem.getProductItemType().equals(ProductItemType.SKU);
    }

    private NumberField newPriceField() {
        NumberField priceField = new NumberField();
        priceField.setValueChangeMode(ValueChangeMode.ON_CHANGE);
        priceField.setPlaceholder("0");
        priceField.setMin(0);
        priceField.setWidth("7em");
        priceField.setTitle(Messages.get("label.priceHint"));
        return priceField;
    }

    private void wirePriceDirtyTracking(NumberField priceField, boolean[] saving) {
        Span commitHint = new Span("\u21b5");
        commitHint.addClassName("price-commit-hint");
        priceField.addFocusListener(event -> {
            if (!saving[0]) {
                priceField.addClassName(Css.PRICE_DIRTY);
                priceField.setSuffixComponent(commitHint);
            }
        });
        priceField.addBlurListener(event -> {
            if (!saving[0]) {
                priceField.removeClassName(Css.PRICE_DIRTY);
                priceField.setSuffixComponent(null);
            }
        });
    }

    private void wirePriceCommit(ProductTreeItem productTreeItem, Integer tierId, NumberField priceField,
                                 AtomicReference<Double> committed, boolean[] saving) {
        priceField.addValueChangeListener(event -> {
            if (saving[0]) {
                return;
            }
            Double previous = committed.get();
            Double next = event.getValue();
            if (Objects.equals(previous, next)) {
                return;
            }
            if (isInvalidPrice(next)) {
                revertPrice(priceField, previous, saving);
                return;
            }
            savePrice(productTreeItem, tierId, priceField, committed, previous, next, saving);
        });
    }

    private boolean isInvalidPrice(Double next) {
        return next == null || next <= 0;
    }

    private void revertPrice(NumberField priceField, Double previous, boolean[] saving) {
        saving[0] = true;
        priceField.setValue(previous);
        saving[0] = false;
    }

    private void savePrice(ProductTreeItem productTreeItem, Integer tierId, NumberField priceField,
                           AtomicReference<Double> committed, Double previous, Double next, boolean[] saving) {
        saving[0] = true;
        priceField.setEnabled(false);
        priceField.setSuffixComponent(null);
        priceField.addClassName(Css.PRICE_SAVING);
        priceField.removeClassName(Css.PRICE_DIRTY);
        committed.set(next);
        SkuTierPriceDto skuTierPriceDto = new SkuTierPriceDto();
        skuTierPriceDto.setSkuId(productTreeItem.getSkuId());
        skuTierPriceDto.setTierId(tierId);
        skuTierPriceDto.setPrice(next);
        restClientMenuService.updateSkuTierPrice(skuTierPriceDto)
                .doOnError(error -> UiUtil.safeAccess(ui, () ->
                        onPriceSaveError(priceField, committed, previous, saving)))
                .subscribe(response -> UiUtil.safeAccess(ui, () ->
                        onPriceSaved(productTreeItem, tierId, priceField, next, saving)));
    }

    private void onPriceSaveError(NumberField priceField, AtomicReference<Double> committed,
                                  Double previous, boolean[] saving) {
        saving[0] = false;
        priceField.setEnabled(true);
        priceField.removeClassName(Css.PRICE_SAVING);
        priceField.addClassName(Css.PRICE_ERROR);
        UiUtil.flash(priceField, Css.PRICE_ERROR, 1500);
        committed.set(previous);
        priceField.setValue(previous);
        UiUtil.error(Messages.get("notification.price.saveFailed"));
    }

    private void onPriceSaved(ProductTreeItem productTreeItem, Integer tierId, NumberField priceField,
                              Double next, boolean[] saving) {
        saving[0] = false;
        priceField.setEnabled(true);
        priceField.removeClassName(Css.PRICE_SAVING);
        priceField.addClassName(Css.PRICE_SAVED);
        UiUtil.flash(priceField, Css.PRICE_SAVED, 1200);
        if (productTreeItem.getTierPrices() == null) {
            productTreeItem.setTierPrices(new HashMap<>());
        }
        productTreeItem.getTierPrices().put(tierId, next);
        UiUtil.success(Messages.get("notification.price.updated"));
    }

    /**
     * Resolves the price of a tree item under the given tier, or {@code null}
     * when the tier is not offered for that SKU.
     *
     * @param productTreeItem the item (product or SKU node)
     * @param tierId          the price tier id
     * @return the price, or {@code null} when the item has no entry for the tier
     */
    private Double tierPrice(ProductTreeItem productTreeItem, Integer tierId) {
        if (productTreeItem.getTierPrices() == null) {
            return null;
        }
        return productTreeItem.getTierPrices().get(tierId);
    }

    /**
     * Creates a temporary BrandDto with ID -1 and name "All" to represent the "All" option in the brand filter.
     *
     * @return a temporary BrandDto for the "All" option
     */
    private BrandDto getTempBrandDto() {
        BrandDto brandDto = new BrandDto();
        brandDto.setId(-1);
        brandDto.setName(Messages.get(Messages.Keys.LABEL_ALL));
        return brandDto;
    }

    /**
     * Creates a temporary CategoryDto with ID -1 and name "All" to represent the "All" option in the category filter.
     *
     * @return a temporary CategoryDto for the "All" option
     */
    private CategoryDto getTempCategoryDto() {
        CategoryDto categoryDto = new CategoryDto();
        categoryDto.setId(-1);
        categoryDto.setName(Messages.get(Messages.Keys.LABEL_ALL));
        return categoryDto;
    }

    /**
     * Creates a temporary TierDto with ID -1 and name "All" to represent the "All" option in the tier filter.
     *
     * @return a temporary TierDto for the "All" option
     */
    private TierDto getTempTierDtp() {
        TierDto tierDto = new TierDto();
        tierDto.setId(-1);
        tierDto.setName(Messages.get(Messages.Keys.LABEL_ALL));
        return tierDto;
    }

    /**
     * Creates the toolbar component containing the brand, category, and tier filters,
     * search field, and "New Product" button.
     *
     * @return a horizontal layout containing the toolbar components
     */
    public HorizontalLayout getToolbarComponent() {
        initTempOptions();
        filterText.setLabel(Messages.get(Messages.Keys.LABEL_SEARCH));
        configureSearchFilter();
        filterText.addValueChangeListener(changeEvent -> {
            if (changeEvent.isFromClient()) {
                currentPage = 1;
                persistFilters();
                fetchProducts(getCategoryId(), brandDtoComboBox.getValue().getId(), changeEvent.getValue());
            }
        });

        brandDtoComboBox.setItems(brandDtos);
        brandDtoComboBox.setLabel(Messages.get(Messages.Keys.LABEL_BRAND));
        brandDtoComboBox.setItemLabelGenerator(BrandDto::getName);
        brandDtoComboBox.addValueChangeListener(valueChangeEvent -> {
            if (valueChangeEvent.isFromClient()) {
                persistFilters();
                fetchCategories(accessService.getUserDetail().getStoreDto().getChainDto().getBrandId());
            }
        });

        categoryDtoComboBox.setItems(categoryDtos);
        categoryDtoComboBox.setLabel(Messages.get(Messages.Keys.LABEL_CATEGORY));
        categoryDtoComboBox.setItemLabelGenerator(CategoryDto::getName);
        categoryDtoComboBox.addValueChangeListener(valueChangeEvent -> {
           if (valueChangeEvent.isFromClient()) {
               persistFilters();
               fetchTier(brandDtoComboBox.getValue().getId());
           }
        });

        tierDtoComboBox.setItems(tierDtos);
        tierDtoComboBox.setLabel(Messages.get(Messages.Keys.LABEL_TIER));
        tierDtoComboBox.setItemLabelGenerator(TierDto::getName);
        tierDtoComboBox.addValueChangeListener(valueChangeEvent -> {
            if (valueChangeEvent.isFromClient()) {
                persistFilters();
                fetchProducts(getCategoryId(), brandDtoComboBox.getValue().getId(), filterText.getValue());
            }
        });
        Button addProduct = UiUtil.addButton(Messages.get(Messages.Keys.ACTION_NEW_PRODUCT), this::onAddProductListener);
        Button shortcutsHelpButton = new Button(new Icon(VaadinIcon.QUESTION_CIRCLE_O));
        shortcutsHelpButton.setTooltipText(Messages.get(Messages.Keys.UI_KEYS_TITLE));
        shortcutsHelpButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        shortcutsHelpButton.addClickListener(event -> UiUtil.shortcutsHelpDialog().open());

        UiUtil.registerListShortcuts(this, typingFields, filterText, () -> onAddProductListener(null));

        HorizontalLayout toolbar = new HorizontalLayout(brandDtoComboBox, categoryDtoComboBox,
                tierDtoComboBox, filterText, addProduct, shortcutsHelpButton);
        toolbar.addClassName(Css.TOOLBAR);
        toolbar.setAlignItems(Alignment.BASELINE);
        return toolbar;
    }

    /**
     * Creates the main content layout containing the product tree grid with the
     * skeleton overlay that covers it while product data reloads.
     *
     * @return a horizontal layout containing the product tree grid
     */
    private HorizontalLayout getContent() {
        return gridSlot(productDtoGrid, gridSkeleton);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        ProductFilters saved = savedFilters();
        if (saved != null) {
            currentPage = saved.page() > 0 ? saved.page() : 1;
            if (saved.search() != null) {
                filterText.setValue(saved.search());
            }
        }
        trackBroadcast(Broadcaster.register(this::acceptNotification));
        renderLayout();
        brandDtoComboBox.setValue(brandDtos.getFirst());
        categoryDtoComboBox.setValue(categoryDtos.getFirst());
        tierDtoComboBox.setValue(tierDtos.getFirst());
    }

    /**
     * Fetches the list of brands from the REST client service and updates the brand filter.
     */
    private void fetchBrands() {
        restClientMenuService.getAllBrand().subscribe(this::acceptBrand);
    }

    /**
     * Opens the "New Product" form in a new tab or selects the existing tab if already open.
     */
    private void addProduct() {
        productDtoGrid.asSingleSelect().clear();
        if (!(this.getParent().orElseThrow() instanceof TabSheet tabSheet)) {
            return;
        }
        new TabManager(tabSheet).addOrSelect(Messages.get(Messages.Keys.ACTION_NEW_PRODUCT), tab ->
                new ProductForm(this.restClientMenuService,
                        this.asyncRestClientMenuService,
                        this.brandDtoComboBox.getValue(),
                        this.categoryDtos, this.tierDtos, tab, null));
    }

    /**
     * Opens the "Edit Product" form for the selected product in a new tab or selects the existing tab if already open.
     *
     * @param productTreeItem the product tree item to be edited
     */
    private void editProduct(ProductTreeItem productTreeItem) {
        productDtoGrid.asSingleSelect().clear();
        if (!(this.getParent().orElseThrow() instanceof TabSheet tabSheet)) {
            return;
        }
        new TabManager(tabSheet).addOrSelect(Messages.get(Messages.Keys.ACTION_EDIT_NAME, productTreeItem.getName()), tab ->
                new ProductForm(this.restClientMenuService,
                        this.asyncRestClientMenuService,
                        this.brandDtoComboBox.getValue(),
                        this.categoryDtos, this.tierDtos, tab, productTreeItem));
    }

    /**
     * Fetches the list of categories for the selected brand from the REST client service and updates the category filter.
     *
     * @param brandId the ID of the selected brand
     */
    private void fetchCategories(Integer brandId) {
        restClientMenuService.getAllCategoryByBrand(brandId)
            .subscribe(this::acceptCategories);
    }

    /**
     * Fetches the list of tiers for the selected brand from the REST client service and updates the tier filter.
     *
     * @param brandId the ID of the selected brand
     */
    private void fetchTier(Integer brandId) {
        restClientMenuService.getAllTierByBrand(brandId, TierTypeDto.PRICE.toString())
            .subscribe(restAPIResponse -> {
                if (!ObjectUtils.isEmpty(restAPIResponse.getData())) {
                    tierDtos = convertJsonToObject(restAPIResponse.getData(), new TypeReference<>() {
                    });

                    UiUtil.safeAccess(ui, ()-> {
                        TierDto defaultTier = tierDtos.getFirst();
                        ProductFilters saved = savedFilters();
                        if (saved != null && saved.tierId() != null) {
                            defaultTier = tierDtos.stream()
                                    .filter(tier -> tier.getId().equals(saved.tierId()))
                                    .findFirst().orElse(defaultTier);
                        }
                        tierDtoComboBox.setItems(tierDtos);
                        tierDtoComboBox.setValue(defaultTier);
                        refreshGridColumns();
                        fetchProducts(getCategoryId(), brandId, filterText.getValue());
                    });
                }
            });
    }

    /**
     * Fetches the list of products based on the selected category, brand, and search text,
     * and updates the product tree grid with the results.
     *
     * @param categoryId    the ID of the selected category
     * @param brandId       the ID of the selected brand
     * @param searchProduct the search text for filtering products by name
     */
    private void fetchProducts(Integer categoryId, Integer brandId, String searchProduct) {
        int pageSize = 15;
        gridSkeleton.show();
        asyncRestClientMenuService.getAllProductCategoryBrandAsync(result -> UiUtil.safeAccess(ui, () -> {
            gridSkeleton.hide();
            if (ObjectUtils.isNotEmpty(result.get("data"))
                && result.get("data") instanceof List<?> dataList && !dataList.isEmpty()) {
                    TreeData<ProductTreeItem> productDtoTreeData = new TreeData<>();

                    dataList.forEach(o -> {
                        ProductDto productDto = ObjectUtil.convertValueToObject(o, ProductDto.class);
                        extractedProductDtoToItem(productDto, productDtoTreeData);
                    });

                    totalPages = Integer.parseInt(result.get("page") == null ? "0" :result.get("page").toString());

                    productDtoGrid.setTreeData(productDtoTreeData);
                    if (ObjectUtils.isNotEmpty(expandTreeItem)) {
                        productDtoGrid.expand(expandTreeItem);
                    }
                    updatePagination();
                } else {
                    productDtoGrid.setTreeData(new TreeData<>());
                    expandTreeItem = null;
                    totalPages = 0;
                    updatePagination();
                }
        }), error -> UiUtil.safeAccess(ui, () -> {
            gridSkeleton.hide();
            UiUtil.errorWithRetry(Messages.get(Messages.Keys.NOTIFICATION_PRODUCT_LOAD_FAILED), () -> fetchProducts(categoryId, brandId, searchProduct));
        }), categoryId, brandId, currentPage, pageSize, searchProduct);
    }

    /**
     * Converts a ProductDto into a ProductTreeItem and adds it to the provided TreeData structure.
     * Also adds the SKUs of the product as child items in the tree.
     *
     * @param productDto          the ProductDto to convert
     * @param productDtoTreeData  the TreeData structure to which the ProductTreeItem will be added
     */
    private void extractedProductDtoToItem(ProductDto productDto, TreeData<ProductTreeItem> productDtoTreeData) {
        ProductTreeItem productTreeItem =  ProductTreeItem.builder()
                .id("%s|%d".formatted(ProductItemType.PRODUCT, productDto.getId()))
                .name(productDto.getName())
                .productId(productDto.getId())
                .categoryId(productDto.getCategoryId())
                .categoryName(productDto.getCategoryDto().getName())
                .productItemType(ProductItemType.PRODUCT)
                .skus(productDto.getSkuDtos())
                .build();
        productDtoTreeData.addItems(null, productTreeItem);
        productDtoTreeData.addItems(productTreeItem, getSkus(productDto));
    }

    /**
     * Converts the SKUs of a ProductDto into a list of ProductTreeItems representing the SKUs.
     *
     * @param productDto the ProductDto containing the SKUs to convert
     * @return a list of ProductTreeItems representing the SKUs of the product
     */
    public List<ProductTreeItem> getSkus(ProductDto productDto) {
        List<ProductTreeItem> productTreeItems = new ArrayList<>();
        productDto.getSkuDtos().forEach(skuDto -> {
            Map<Integer, Double> tierPrices = new HashMap<>();
            if (skuDto.getSkuTierPriceDtos() != null) {
                skuDto.getSkuTierPriceDtos().forEach(skuTierPriceDto ->
                        tierPrices.put(skuTierPriceDto.getTierId(), skuTierPriceDto.getPrice()));
            }
            productTreeItems.add(ProductTreeItem.builder()
                    .id("%s|%d".formatted(ProductItemType.SKU, skuDto.getId()))
                    .name(skuDto.getName())
                    .productItemType(ProductItemType.SKU)
                    .productId(productDto.getId())
                    .skuId(skuDto.getId())
                    .categoryName("")
                    .price(0.0)
                    .tierPrices(tierPrices)
                    .build());
        });
        return productTreeItems;
    }

    /**
     * Retrieves the ID of the selected category from the category combo box.
     * If no category is selected, returns -1 to indicate "All" categories.
     *
     * @return the ID of the selected category, or -1 if no category is selected
     */
    private Integer getCategoryId() {
        Integer categoryId = -1;
        if (!ObjectUtils.isEmpty(categoryDtoComboBox.getValue())) {
            categoryId = categoryDtoComboBox.getValue().getId();
        }
        return categoryId;
    }

    public static <T> T convertJsonToObject(Object data, TypeReference<T> typeReference) {
        return ObjectUtil.convertObjectToObject(data, typeReference);
    }

    /**
     * Accepts the REST API response for fetching brands and updates the brand combo box.
     *
     * @param restAPIResponse the response from the REST API containing brand data
     */
    private void acceptBrand(RestAPIResponse restAPIResponse) {
        if (!ObjectUtils.isEmpty(restAPIResponse.getData())) {

           brandDtos = convertJsonToObject(restAPIResponse.getData(),
                    new TypeReference<>() {});

            if (!ObjectUtils.isEmpty(brandDtos)) {

                brandDtos.addFirst(getTempBrandDto());
                if (ObjectUtils.isNotEmpty(ui)) {
                    UiUtil.safeAccess(ui, () -> {
                        BrandDto defaultBrand = brandDtos.get(1);
                        ProductFilters saved = savedFilters();
                        if (saved != null && saved.brandId() != null) {
                            defaultBrand = brandDtos.stream()
                                    .filter(brand -> brand.getId().equals(saved.brandId()))
                                    .findFirst().orElse(defaultBrand);
                        }
                        brandDtoComboBox.setItems(brandDtos);
                        brandDtoComboBox.setValue(defaultBrand);
                        fetchCategories(brandDtoComboBox.getValue().getId());
                        fetchTier(brandDtoComboBox.getValue().getId());
                    });
                }
            }
        }
    }

    /**
     * Accepts the REST API response for fetching categories and updates the category combo box.
     *
     * @param restAPIResponse the response from the REST API containing category data
     */
    private void acceptCategories(RestAPIResponse restAPIResponse) {
        if (!ObjectUtils.isEmpty(restAPIResponse.getData())) {

            categoryDtos = convertJsonToObject(restAPIResponse.getData(),
                    new TypeReference<>() {});

            if (ObjectUtils.isNotEmpty(categoryDtos)) {
                categoryDtos.addFirst(getTempCategoryDto());
                UiUtil.safeAccess(ui, () -> {
                    CategoryDto defaultCategory = categoryDtos.getFirst();
                    ProductFilters saved = savedFilters();
                    if (saved != null && saved.categoryId() != null) {
                        defaultCategory = categoryDtos.stream()
                                .filter(category -> category.getId().equals(saved.categoryId()))
                                .findFirst().orElse(defaultCategory);
                    }
                    categoryDtoComboBox.setItems(categoryDtos);
                    categoryDtoComboBox.setValue(defaultCategory);
                });
            }
        }
    }

    /**
     * Accepts broadcast messages and triggers appropriate actions based on the message type.
     *
     * @param message the broadcast message received
     */
    private void acceptNotification(String message) {
        try {
            BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
            if (ObjectUtils.isNotEmpty(broadcastMessage) && ObjectUtils.isNotEmpty(broadcastMessage.getType())) {
                if (broadcastMessage.getType().equals(BroadcastMessage.PRODUCT_INSERT_SUCCESS)) {
                    UiUtil.safeAccess(ui, this::fetchBrands);
                } else if (broadcastMessage.getType().equals(BroadcastMessage.PRODUCT_UPDATE_SUCCESS)) {
                    UiUtil.safeAccess(ui, () -> fetchProducts(getCategoryId(),
                            brandDtoComboBox.getValue().getId(), filterText.getValue()));
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Broadcast Handler Error", e);
        }
    }

    /**
     * Handles the expand event of the product tree grid. Remembers the last
     * expanded product so the tree can be re-expanded after a re-fetch and
     * loads the price of every SKU under each price tier, filling the matrix
     * columns.
     *
     * @param event the expand event containing the expanded items
     */
    private void onComponentEventExpandListener(ExpandEvent<ProductTreeItem, TreeGrid<ProductTreeItem>> event) {
        event.getItems().forEach(productTreeItem -> {
            expandTreeItem = productTreeItem;
            List<Integer> skuIds = new ArrayList<>();
            List<ProductTreeItem> skuItems = new ArrayList<>();
            event.getSource().getTreeData().getChildren(productTreeItem).forEach(productTreeItemChild -> {
                skuIds.add(productTreeItemChild.getSkuId());
                skuItems.add(productTreeItemChild);
            });
            tierDtos.stream()
                    .filter(tier -> tier.getId() != null && tier.getId() > 0)
                    .forEach(tier -> fetchPriceByTier(skuIds, skuItems, tier));
        });
    }

    /**
     * Fetches the price of the given SKUs under a single tier and stores it in
     * the {@link ProductTreeItem#tierPrices} matrix, refreshing the affected
     * rows once the response arrives.
     *
     * @param skuIds   the SKU ids to fetch prices for
     * @param skuItems the matching SKU tree items to update
     * @param tier     the price tier to load
     */
    private void fetchPriceByTier(List<Integer> skuIds, List<ProductTreeItem> skuItems, TierDto tier) {
        asyncRestClientMenuService.getDetailSkuTierPriceAsync(skuTierPriceDtos -> skuTierPriceDtos.forEach(skuTierPriceDto -> skuItems.stream()
                .filter(skuItem -> skuItem.getSkuId().equals(skuTierPriceDto.getSkuId()))
                .findAny()
                .ifPresent(skuItem -> UiUtil.safeAccess(ui, () -> {
                    if (skuItem.getTierPrices() == null) {
                        skuItem.setTierPrices(new HashMap<>());
                    }
                    skuItem.getTierPrices().put(tier.getId(), skuTierPriceDto.getPrice());
                    productDtoGrid.getDataProvider().refreshItem(skuItem);
                }))), skuIds, tier.getId());
    }

    private void onAddProductListener(ClickEvent<Button> buttonClickEvent) {
        addProduct();
    }

}
