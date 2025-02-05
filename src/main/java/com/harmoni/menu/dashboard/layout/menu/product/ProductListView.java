package com.harmoni.menu.dashboard.layout.menu.product;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.*;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.event.product.ProductDeleteEventListener;
import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.enums.ProductItemType;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.rest.data.RestAPIResponse;
import com.harmoni.menu.dashboard.rest.data.RestClientMenuService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.ExpandEvent;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@UIScope
@PreserveOnRefresh
@Route(value = "product-list", layout = MainLayout.class)
@PageTitle("Product | POSHarmoni")
@Component
@Slf4j
public class ProductListView extends VerticalLayout implements BroadcastMessageService {

    Registration broadcasterRegistration;

    TreeGrid<ProductTreeItem> productDtoGrid = new TreeGrid<>(ProductTreeItem.class);

    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final RestClientMenuService restClientMenuService;
    @Getter
    private final Tab defaultTab;

    TextField filterText = new TextField();
    ComboBox<TierDto> tierDtoComboBox = new ComboBox<>();
    ComboBox<BrandDto> brandDtoComboBox = new ComboBox<>();
    ComboBox<CategoryDto> categoryDtoComboBox = new ComboBox<>();
    transient List<CategoryDto> categoryDtos = new ArrayList<>();
    UI ui;
    static final Integer TEMP_BRAND_ID = 1;
    transient List<BrandDto> brandDtos = new ArrayList<>();
    transient List<TierDto> tierDtos = new ArrayList<>();
    int totalPages;
    int currentPage = 1;

    Text pageInfoText;
    transient ProductTreeItem expandTreeItem;

    private void renderLayout() {
        addClassName("list-view");

        brandDtos.add(getTempBrandDto());
        categoryDtos.add(getTempCategoryDto());
        tierDtos.add(getTempTierDtp());

        setSizeFull();
        configureGrid();
        add(getToolbar(), getContent(), getPaginationFooter());
        fetchBrands();
    }

    private HorizontalLayout applyButton(ProductTreeItem productTreeItem) {
        if (productTreeItem.getProductItemType().equals(ProductItemType.PRODUCT)) {
            HorizontalLayout horizontalLayout = new HorizontalLayout();
            Button editButton = new Button("Edit");
            editButton.addClickListener(_ -> editProduct(productTreeItem));
            Button deleteButton = new Button("Delete");
            deleteButton.addClickListener(new ProductDeleteEventListener(restClientMenuService, productTreeItem));
            horizontalLayout.add(editButton, deleteButton);
            return horizontalLayout;
        }
        return null;
    }

    private HorizontalLayout getPaginationFooter() {
        HorizontalLayout paginationFooter = new HorizontalLayout();
        Button previousButton = new Button("Previous", _ -> {
            if (currentPage > 1) {
                currentPage--;
                fetchProducts(getCategoryId(), brandDtoComboBox.getValue().getId(), filterText.getValue());
            }
        });
        Button nextButton = new Button("Next", _ -> {
            if (currentPage < totalPages) {
                currentPage++;
                fetchProducts(getCategoryId(), brandDtoComboBox.getValue().getId(), filterText.getValue());
            }
        });
        pageInfoText = new Text(getPaginationInfo());
        paginationFooter.add(previousButton, pageInfoText, nextButton);
        paginationFooter.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        paginationFooter.setWidthFull();
        paginationFooter.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        return paginationFooter;
    }

    private String getPaginationInfo() {
        return "Page "
                .concat(String.valueOf(currentPage))
                .concat(" of ")
                .concat(String.valueOf(totalPages));
    }

    private void configureGrid() {
        productDtoGrid.setSizeFull();
        productDtoGrid.removeAllColumns();
        productDtoGrid.addHierarchyColumn(ProductTreeItem::getName).setHeader("Name");
        productDtoGrid.addColumn(ProductTreeItem::getCategoryName).setHeader("Category");
        productDtoGrid.addColumn(ProductTreeItem::getPrice).setHeader("Price");
        productDtoGrid.addColumn(ProductTreeItem::getTierName).setHeader("Tier");
        productDtoGrid.addComponentColumn(this::applyButton).setHeader("Action");

        productDtoGrid.addExpandListener(this::onComponentEventExpandListener);
        productDtoGrid.getColumns().forEach(productDtoColumn -> productDtoColumn.setAutoWidth(true));
    }

    private BrandDto getTempBrandDto() {
        BrandDto brandDto = new BrandDto();
        brandDto.setId(-1);
        brandDto.setName("All");
        return brandDto;
    }

    private CategoryDto getTempCategoryDto() {
        CategoryDto categoryDto = new CategoryDto();
        categoryDto.setId(-1);
        categoryDto.setName("All");
        return categoryDto;
    }

    private TierDto getTempTierDtp() {
        TierDto tierDto = new TierDto();
        tierDto.setId(-1);
        tierDto.setName("All");
        return tierDto;
    }

    private HorizontalLayout getToolbar() {
        filterText.setLabel("Product");
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(changeEvent -> {
            if (changeEvent.getValue().isEmpty()) {
                currentPage = 1;
                fetchProducts(getCategoryId(), brandDtoComboBox.getValue().getId(), filterText.getValue());
            }
        });

        brandDtoComboBox.setItems(brandDtos);
        brandDtoComboBox.setLabel("Brand");
        brandDtoComboBox.setItemLabelGenerator(BrandDto::getName);
        brandDtoComboBox.addValueChangeListener(valueChangeEvent -> {
            if (valueChangeEvent.isFromClient()) {
                fetchCategories(TEMP_BRAND_ID);
            }
        });

        categoryDtoComboBox.setItems(categoryDtos);
        categoryDtoComboBox.setLabel("Category");
        categoryDtoComboBox.setItemLabelGenerator(CategoryDto::getName);
        categoryDtoComboBox.addValueChangeListener(valueChangeEvent -> {
           if (valueChangeEvent.isFromClient()) {
               fetchTier(brandDtoComboBox.getValue().getId());
           }
        });

        tierDtoComboBox.setItems(tierDtos);
        tierDtoComboBox.setLabel("Tier");
        tierDtoComboBox.setItemLabelGenerator(TierDto::getName);
        tierDtoComboBox.addValueChangeListener(valueChangeEvent -> {
            if (valueChangeEvent.isFromClient()) {
                fetchProducts(getCategoryId(), brandDtoComboBox.getValue().getId(), filterText.getValue());
            }
        });
        Button searchProduct = new Button("Search");
        searchProduct.addClickListener(this::onSearchProductListener);
        Button addProduct = new Button("Add Product");
        addProduct.addClickListener(this::onAddProductListener);

        HorizontalLayout toolbar = new HorizontalLayout(brandDtoComboBox, categoryDtoComboBox,
                tierDtoComboBox, filterText, searchProduct, addProduct);
        toolbar.addClassName("toolbar");
        toolbar.setAlignItems(Alignment.BASELINE);
        return toolbar;
    }

    private HorizontalLayout getContent() {
        HorizontalLayout content = new HorizontalLayout(productDtoGrid);
        content.setFlexGrow(1, productDtoGrid);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(this::acceptNotification);
        renderLayout();
        brandDtoComboBox.setValue(brandDtos.getFirst());
        categoryDtoComboBox.setValue(categoryDtos.getFirst());
        tierDtoComboBox.setValue(tierDtos.getFirst());
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    private void fetchBrands() {
        restClientMenuService.getAllBrand().subscribe(this::acceptBrand);
    }

    private void addProduct() {
        productDtoGrid.asSingleSelect().clear();
        if (!(this.getParent().orElseThrow() instanceof TabSheet tabSheet)) {
            return;
        }
        Tab tabNewProduct = new Tab();
        tabNewProduct.setLabel("New Product");
        tabSheet.add(tabNewProduct, new ProductForm(this.restClientMenuService,
                this.brandDtoComboBox.getValue(),
                this.categoryDtos, this.tierDtos, tabNewProduct, null));
        tabSheet.setSizeFull();
        tabSheet.setSelectedTab(tabNewProduct);
    }

    private void editProduct(ProductTreeItem productTreeItem) {
        productDtoGrid.asSingleSelect().clear();
        if (!(this.getParent().orElseThrow() instanceof TabSheet tabSheet)) {
            return;
        }
        Tab tabNewProduct = new Tab();
        tabNewProduct.setLabel("Edit ".concat(productTreeItem.getName()));
        tabSheet.add(tabNewProduct, new ProductForm(this.restClientMenuService,
                this.brandDtoComboBox.getValue(),
                this.categoryDtos, this.tierDtos, tabNewProduct, productTreeItem));
        tabSheet.setSizeFull();
        tabSheet.setSelectedTab(tabNewProduct);
    }

    private void fetchCategories(Integer brandId) {
        restClientMenuService.getAllCategoryByBrand(brandId)
            .subscribe(this::acceptCategories);
    }

    private void fetchTier(Integer brandId) {
        restClientMenuService.getAllTierByBrand(brandId, TierTypeDto.PRICE.toString())
            .subscribe(restAPIResponse -> {
                if (!ObjectUtils.isEmpty(restAPIResponse.getData())) {
                    tierDtos = convertJsonToObject(restAPIResponse.getData(), new TypeReference<>() {
                    });

                    ui.access(()-> {
                        tierDtoComboBox.setItems(tierDtos);
                        tierDtoComboBox.setValue(tierDtos.getFirst());
                        fetchProducts(getCategoryId(), brandId, filterText.getValue());
                    });
                }
            });
    }

    private void fetchProducts(Integer categoryId, Integer brandId, String searchProduct) {
        int pageSize = 15;
        asyncRestClientMenuService.getAllProductCategoryBrandAsync(result -> {
            if (ObjectUtils.isNotEmpty(result.get("data"))
                && result.get("data") instanceof List<?> dataList && !dataList.isEmpty()) {
                    TreeData<ProductTreeItem> productDtoTreeData = new TreeData<>();

                    dataList.forEach(o -> {
                        ProductDto productDto = ObjectUtil.convertValueToObject(o, ProductDto.class);
                        extractedProductDtoToItem(productDto, productDtoTreeData);
                    });

                    totalPages = Integer.parseInt(result.get("page") == null ? "0" :result.get("page").toString());

                    ui.access(()-> {
                        productDtoGrid.setTreeData(productDtoTreeData);
                        if (ObjectUtils.isNotEmpty(expandTreeItem)) {
                            productDtoGrid.expand(expandTreeItem);
                        }
                        pageInfoText.setText(getPaginationInfo());
                    });
                }
        }, categoryId, brandId, currentPage, pageSize, searchProduct);
    }

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

    public List<ProductTreeItem> getSkus(ProductDto productDto) {
        List<ProductTreeItem> productTreeItems = new ArrayList<>();
        productDto.getSkuDtos().forEach(skuDto -> productTreeItems.add(ProductTreeItem.builder()
                        .id("%s|%d".formatted(ProductItemType.SKU, skuDto.getId()))
                        .name(skuDto.getName())
                        .productItemType(ProductItemType.SKU)
                        .productId(productDto.getId())
                        .skuId(skuDto.getId())
                        .categoryName("")
                        .price(0.0)
                .build()));
        return productTreeItems;
    }

    private void fetchPriceBySku(List<Integer> skuIds,
                                 List<ProductTreeItem> productTreeItems, Integer tierId) {
        asyncRestClientMenuService.getDetailSkuTierPriceAsync(result -> result.forEach(skuTierPriceDto -> {
            ProductTreeItem productTreeItem = productTreeItems.stream()
                    .filter(productItem -> skuTierPriceDto.getSkuId().equals(productItem.getSkuId()))
                    .findAny()
                    .orElse(null);
            log.debug("{}", productTreeItem);
            if (!ObjectUtils.isEmpty(productTreeItem)) {
                productTreeItem.setPrice(skuTierPriceDto.getPrice());
                productTreeItem.setTierName(skuTierPriceDto.getTierDto().getName());
            }
        }), skuIds, tierId);
    }

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

    private void acceptBrand(RestAPIResponse restAPIResponse) {
        if (!ObjectUtils.isEmpty(restAPIResponse.getData())) {

           brandDtos = convertJsonToObject(restAPIResponse.getData(),
                    new TypeReference<>() {});

            if (!ObjectUtils.isEmpty(brandDtos)) {

                brandDtos.addFirst(getTempBrandDto());
                if (ObjectUtils.isNotEmpty(ui)) {
                    ui.access(() -> {
                        brandDtoComboBox.setItems(brandDtos);
                        brandDtoComboBox.setValue(brandDtos.get(1));
                        fetchCategories(brandDtoComboBox.getValue().getId());
                        fetchTier(brandDtoComboBox.getValue().getId());
                    });
                }
            }
        }
    }

    private void acceptCategories(RestAPIResponse restAPIResponse) {
        if (!ObjectUtils.isEmpty(restAPIResponse.getData())) {

            categoryDtos = convertJsonToObject(restAPIResponse.getData(),
                    new TypeReference<>() {});

            if (ObjectUtils.isNotEmpty(categoryDtos)) {
                categoryDtos.addFirst(getTempCategoryDto());
                ui.access(() -> {
                    categoryDtoComboBox.setItems(categoryDtos);
                    categoryDtoComboBox.setValue(categoryDtos.getFirst());
                });
            }
        }
    }

    private void acceptNotification(String message) {
        try {
            BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
            if (ObjectUtils.isNotEmpty(broadcastMessage) && ObjectUtils.isNotEmpty(broadcastMessage.getType())) {
                if (broadcastMessage.getType().equals(BroadcastMessage.PRODUCT_INSERT_SUCCESS)) {
                    fetchBrands();
                } else if (broadcastMessage.getType().equals(BroadcastMessage.PRODUCT_UPDATE_SUCCESS)) {
                    fetchProducts(getCategoryId(), brandDtoComboBox.getValue().getId(), filterText.getValue());
                }
            }
        } catch (JsonProcessingException e) {
            log.error("Broadcast Handler Error", e);
        }
    }

    private void onComponentEventExpandListener(ExpandEvent<ProductTreeItem, TreeGrid<ProductTreeItem>> event) {

        event.getItems().forEach(productTreeItem -> {
            expandTreeItem = productTreeItem;
            List<Integer> skuIds = new ArrayList<>();
            List<ProductTreeItem> productTreeItems = new ArrayList<>();
            event.getSource().getTreeData().getChildren(productTreeItem).forEach(productTreeItemChild -> {
                skuIds.add(productTreeItemChild.getSkuId());
                productTreeItems.add(productTreeItemChild);
                productTreeItemChild.setPrice(0.0);
                productTreeItemChild.setTierName("");
            });
            fetchPriceBySku(skuIds, productTreeItems, tierDtoComboBox.getValue().getId());
        });
    }

    private void onAddProductListener(ClickEvent<Button> buttonClickEvent) {
        addProduct();
    }

    private void onSearchProductListener(ClickEvent<Button> buttonClickEvent) {
        currentPage = 1;
        fetchProducts(getCategoryId(), brandDtoComboBox.getValue().getId(), filterText.getValue());
    }

}
