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
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.treegrid.ExpandEvent;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.vaadin.flow.component.ItemLabelGenerator;
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

    ComboBox<TierDto> tierDtoComboBox = new ComboBox<>();
    ComboBox<BrandDto> brandDtoComboBox = new ComboBox<>();
    ComboBox<CategoryDto> categoryDtoComboBox = new ComboBox<>();
    transient List<CategoryDto> categoryDtos = new ArrayList<>();

    transient List<BrandDto> brandDtos = new ArrayList<>();
    transient List<TierDto> tierDtos = new ArrayList<>();
    private static final int PAGE_SIZE = 15;

    private final GridSkeleton gridSkeleton = new GridSkeleton(PAGE_SIZE);

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

    /**
     * Renders the main layout of the product list view, including the grid and pagination footer.
     */
    private void renderLayout() {
        setSizeFull();
        setPadding(false);
        configureGrid();
        add(getContent(), getPaginationFooter());
        fetchBrands();
    }

    /**
     * Initializes the temporary "All" options for brand, category, and tier filters.
     * These options are added to the respective lists if they are not already present.
     */
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
                fetchProducts();
            }
        }, () -> {
            if (currentPage < totalPages) {
                currentPage++;
                persistFilters();
                fetchProducts();
            }
        });
    }

    /**
     * Configures the product tree grid, including size, empty state text, and column setup.
     * Also adds an expand listener to handle row expansion events.
     */
    private void configureGrid() {
        productDtoGrid.setSizeFull();
        productDtoGrid.setEmptyStateText(Messages.get("grid.empty.products"));
        refreshGridColumns();
        productDtoGrid.addExpandListener(this::onComponentEventExpandListener);
    }

    /** Signature of the tier columns currently rendered, so an unchanged tier set is a no-op. */
    private String tierColumnSignature = "";

    /**
     * Rebuilds all columns in display order: name hierarchy, category, one
     * price column per price tier, and the action column.
     *
     * <p>Called once on attach and again when the tier list finishes loading.
     * The rebuild is skipped when the tier set is unchanged, so re-selecting the
     * same brand no longer resets column widths and sort state.</p>
     */
    private void refreshGridColumns() {
        String signature = tierSignature();
        if (signature.equals(tierColumnSignature) && !productDtoGrid.getColumns().isEmpty()) {
            return;
        }
        tierColumnSignature = signature;

        productDtoGrid.removeAllColumns();
        addBaseColumns();
        addTierPriceColumns();
        productDtoGrid.addComponentColumn(this::applyButton).setHeader(Messages.get(Messages.Keys.GRID_HEADER_ACTION));
        productDtoGrid.getColumns().forEach(productDtoColumn -> productDtoColumn.setAutoWidth(true));
    }

    private void addBaseColumns() {
        productDtoGrid.addHierarchyColumn(ProductTreeItem::getName).setHeader(Messages.get(Messages.Keys.GRID_HEADER_NAME));
        productDtoGrid.addColumn(ProductTreeItem::getCategoryName).setHeader(Messages.get("grid.header.category"));
    }

    private String tierSignature() {
        if (tierDtos == null) {
            return "";
        }
        return tierDtos.stream()
                .filter(tier -> tier.getId() != null && tier.getId() > 0)
                .map(tier -> tier.getId() + ":" + tier.getName())
                .collect(java.util.stream.Collectors.joining(","));
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
                        .setHeader(tier.getName() == null ? "" : tier.getName().toUpperCase(Locale.ROOT)));
    }

    /**
     * Builds the price cell for a tree item under the given tier: an editable
     * number field for SKU rows and a placeholder for product rows.
     *
     * @param productTreeItem the item (product or SKU node)
     * @param tierId          the price tier id
     * @return the editable price component for the cell
     */
    private Component tierPriceCell(ProductTreeItem productTreeItem, Integer tierId) {
        if (!isSkuRow(productTreeItem)) {
            return new Text("\u2013");
        }
        return new TierPriceCell(productTreeItem, tierId, restClientMenuService, ui, typingFields);
    }

    /**
     * Determines if the given product tree item is a SKU row.
     *
     * @param productTreeItem the item to check
     * @return {@code true} if the item is a SKU row, {@code false} otherwise
     */
    private boolean isSkuRow(ProductTreeItem productTreeItem) {
        return productTreeItem.getProductItemType().equals(ProductItemType.SKU);
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
                onFilterChanged(this::fetchProducts);
            }
        });

        configureFilter(brandDtoComboBox, brandDtos, Messages.Keys.LABEL_BRAND, BrandDto::getName,
                () -> fetchCategories(selectedBrandId()));
        configureFilter(categoryDtoComboBox, categoryDtos, Messages.Keys.LABEL_CATEGORY, CategoryDto::getName,
                () -> fetchTier(selectedBrandId()));
        configureFilter(tierDtoComboBox, tierDtos, Messages.Keys.LABEL_TIER, TierDto::getName,
                this::fetchProducts);
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
     * Resets paging, remembers the filters and runs the reload the changed
     * filter needs.
     *
     * @param reload the fetch triggered by the changed filter
     */
    private void onFilterChanged(Runnable reload) {
        currentPage = 1;
        persistFilters();
        reload.run();
    }

    /**
     * Wires one toolbar combo box: items, label, display text and the client-side
     * change handling shared by every filter.
     *
     * @param comboBox        the filter combo box
     * @param items           the options backing it
     * @param labelKey        message key of the combo box label
     * @param labelGenerator  renders an option's display text
     * @param reload          the fetch triggered when the selection changes
     */
    private <T> void configureFilter(ComboBox<T> comboBox, List<T> items, String labelKey,
                                     ItemLabelGenerator<T> labelGenerator, Runnable reload) {
        comboBox.setItems(items);
        comboBox.setLabel(Messages.get(labelKey));
        comboBox.setItemLabelGenerator(labelGenerator);
        comboBox.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                onFilterChanged(reload);
            }
        });
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
        restClientMenuService.getAllBrand()
                .subscribe(this::acceptBrand, error -> UiUtil.safeAccess(ui, () ->
                        UiUtil.errorWithRetry(Messages.get(Messages.Keys.NOTIFICATION_BRAND_LOAD_FAILED),
                                this::fetchBrands)));
    }

    /**
     * Opens the "New Product" form in a new tab or selects the existing tab if already open.
     */
    private void addProduct() {
        openEditor(Messages.get(Messages.Keys.ACTION_NEW_PRODUCT), null);
    }

    /**
     * Opens the "Edit Product" form for the selected product in a new tab or selects the existing tab if already open.
     *
     * @param productTreeItem the product tree item to be edited
     */
    private void editProduct(ProductTreeItem productTreeItem) {
        openEditor(Messages.get(Messages.Keys.ACTION_EDIT_NAME, productTreeItem.getName()), productTreeItem);
    }

    /**
     * Opens (or selects) the editor tab for a product.
     *
     * @param title           the tab title
     * @param productTreeItem the product being edited, or {@code null} for a new one
     */
    private void openEditor(String title, ProductTreeItem productTreeItem) {
        productDtoGrid.asSingleSelect().clear();
        if (!(this.getParent().orElseThrow() instanceof TabSheet tabSheet)) {
            return;
        }
        TabManager tabManager = new TabManager(tabSheet);
        tabManager.addOrSelect(title, tab -> new ProductFormWithPreview(
                this.restClientMenuService,
                this.asyncRestClientMenuService,
                editorContext(productTreeItem, tab, tabManager)));
    }

    private ProductEditorContext editorContext(ProductTreeItem productTreeItem, Tab tab, TabManager tabManager) {
        return ProductEditorContext.builder()
                .brandDto(this.brandDtoComboBox.getValue())
                .categoryDtos(this.categoryDtos)
                .tierDtos(this.tierDtos)
                .productTab(tab)
                .productTreeItem(productTreeItem)
                .tabManager(tabManager)
                .build();
    }

    /**
     * Fetches the list of categories for the selected brand from the REST client service and updates the category filter.
     *
     * @param brandId the ID of the selected brand
     */
    private void fetchCategories(Integer brandId) {
        restClientMenuService.getAllCategoryByBrand(brandId)
            .subscribe(this::acceptCategories, error -> UiUtil.safeAccess(ui, () ->
                    UiUtil.errorWithRetry(Messages.get(Messages.Keys.NOTIFICATION_CATEGORY_LOAD_FAILED),
                            () -> fetchCategories(brandId))));
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
                    if (ObjectUtils.isEmpty(tierDtos)) {
                        return;
                    }

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
            }, error -> UiUtil.safeAccess(ui, () ->
                    UiUtil.errorWithRetry(Messages.get("notification.tierPrice.loadFailed"),
                            () -> fetchTier(brandId))));
    }

    /**
     * Fetches products for the filters currently shown in the toolbar.
     */
    private void fetchProducts() {
        fetchProducts(getCategoryId(), selectedBrandId(), filterText.getValue());
    }

    private Integer selectedBrandId() {
        return brandDtoComboBox.getValue() == null ? null : brandDtoComboBox.getValue().getId();
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
                } else if (currentPage > 1) {
                    // Deleting the last row of a page leaves it empty; step back one
                    // page and refetch instead of showing an empty grid.
                    currentPage--;
                    persistFilters();
                    fetchProducts(categoryId, brandId, searchProduct);
                } else {
                    productDtoGrid.setTreeData(new TreeData<>());
                    expandTreeItem = null;
                    totalPages = 0;
                    updatePagination();
                }
        }), error -> UiUtil.safeAccess(ui, () -> {
            gridSkeleton.hide();
            UiUtil.errorWithRetry(Messages.get(Messages.Keys.NOTIFICATION_PRODUCT_LOAD_FAILED), () -> fetchProducts(categoryId, brandId, searchProduct));
        }), categoryId, brandId, currentPage, PAGE_SIZE, searchProduct);
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
                switch (broadcastMessage.getType()) {
                    case BroadcastMessage.PRODUCT_INSERT_SUCCESS, BroadcastMessage.PRODUCT_UPDATE_SUCCESS,
                         BroadcastMessage.PRODUCT_DELETE_SUCCESS ->
                            UiUtil.safeAccess(ui, this::fetchProducts);
                    default -> {
                        // ignore unrelated broadcast types
                    }
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
            List<ProductTreeItem> skuItems = new ArrayList<>();
            event.getSource().getTreeData().getChildren(productTreeItem).forEach(skuItems::add);

            List<Integer> skuIds = skuItems.stream()
                    .map(ProductTreeItem::getSkuId)
                    .filter(Objects::nonNull)
                    .toList();

            // The product payload already carries each SKU's tier prices, so only
            // fetch the tiers that are actually missing. This used to fire one
            // request per tier on every expand even when nothing was needed.
            tierDtos.stream()
                    .filter(tier -> tier.getId() != null && tier.getId() > 0)
                    .filter(tier -> hasMissingPrice(skuItems, tier.getId()))
                    .forEach(tier -> fetchPriceByTier(skuIds, skuItems, tier));
        });
    }

    private boolean hasMissingPrice(List<ProductTreeItem> skuItems, Integer tierId) {
        return skuItems.stream()
                .anyMatch(item -> item.getTierPrices() == null || item.getTierPrices().get(tierId) == null);
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
