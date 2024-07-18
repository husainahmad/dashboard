package com.harmoni.menu.dashboard.layout.menu.product;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.dto.ProductDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.enums.ProductItemType;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.rest.data.RestClientMenuService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.PreserveOnRefresh;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.spring.annotation.UIScope;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@UIScope
@PreserveOnRefresh
@Route(value = "product", layout = MainLayout.class)
@PageTitle("Product | POSHarmoni")
@Component
@Slf4j
public class ProductListView extends VerticalLayout {

    Registration broadcasterRegistration;

    private final TreeGrid<ProductTreeItem> productDtoGrid = new TreeGrid<>(ProductTreeItem.class);
    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final RestClientMenuService restClientMenuService;

    private final TextField filterText = new TextField();
    private final ComboBox<TierDto> tierDtoComboBox = new ComboBox<>();
    private final ComboBox<BrandDto> brandDtoComboBox = new ComboBox<>();
    private final ComboBox<CategoryDto> categoryDtoComboBox = new ComboBox<>();
    private ProductForm productForm;
    private transient TierDto tierDto;
    private transient BrandDto brandDto;
    private transient List<CategoryDto> categoryDtos;
    private UI ui;

    public ProductListView(@Autowired AsyncRestClientMenuService asyncRestClientMenuService,
                           @Autowired RestClientMenuService restClientMenuService) {

        this.asyncRestClientMenuService = asyncRestClientMenuService;
        this.restClientMenuService = restClientMenuService;

        addClassName("list-view");
        setSizeFull();

        configureGrid();
        configureForm();

        add(getToolbar(), getContent());

        closeEditor();
    }

    private void configureGrid() {
        productDtoGrid.setSizeFull();
        productDtoGrid.removeAllColumns();
        productDtoGrid.addHierarchyColumn(ProductTreeItem::getName).setHeader("Name");
        productDtoGrid.addColumn(ProductTreeItem::getCategoryName).setHeader("Category");
        productDtoGrid.addColumn(ProductTreeItem::getPrice).setHeader("Price");
        productDtoGrid.addColumn(ProductTreeItem::getTierName).setHeader("Tier");
        productDtoGrid.addComponentColumn(productTreeItem -> {
            AtomicReference<ProductTreeItem> atomicProductItem = new AtomicReference<>(productTreeItem);
            ProductEditButton button = new ProductEditButton(atomicProductItem.get());
            button.addClickListener(buttonClickEvent -> {
                if (buttonClickEvent.isFromClient()) {
                    ProductDialogEdit productDialogEdit = new ProductDialogEdit(this.asyncRestClientMenuService,
                            this.restClientMenuService,
                            button.getProductTreeItem(),
                            this.tierDtoComboBox.getValue(),
                            categoryDtos);
                    add(productDialogEdit);
                    productDialogEdit.open();
                }
            });

            if (!ObjectUtils.isEmpty(productTreeItem)
                    && productTreeItem.getProductItemType()==ProductItemType.SKU) {
                button.setVisible(false);
            }
            return button;
        });

        productDtoGrid.addCollapseListener(event -> event.getItems().forEach(productTreeItem ->
                log.debug("item collapse {}", productTreeItem)));

        productDtoGrid.addExpandListener(event -> {
            if (event.isFromClient()) {
                event.getItems().forEach(productTreeItem -> {
                    log.debug("item expand {}", productTreeItem);
                    List<Integer> skuIds = new ArrayList<>();
                    List<ProductTreeItem> productTreeItems = new ArrayList<>();
                    event.getSource().getTreeData().getChildren(productTreeItem).forEach(productTreeItemChild -> {
                        log.debug("item expand child {}", productTreeItemChild);
                        skuIds.add(productTreeItemChild.getSkuId());
                        productTreeItems.add(productTreeItemChild);
                        productTreeItemChild.setPrice(0.0);
                        productTreeItemChild.setTierName("");
                    });
                    log.debug("skuids {}", skuIds);
                    log.debug("productTreeItems {}", productTreeItems);

                    fetchPriceBySku(skuIds, productTreeItems, tierDto.getId());
                });
            }
        });

        productDtoGrid.getColumns().forEach(productDtoColumn -> productDtoColumn.setAutoWidth(true));

    }

    private void configureForm() {
        productForm = new ProductForm(this.asyncRestClientMenuService);
        productForm.setWidth("25em");
    }


    private HorizontalLayout getToolbar() {
        filterText.setLabel("Product");
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);

        brandDtoComboBox.setLabel("Brand");
        brandDtoComboBox.setItems(new ArrayList<>());
        brandDtoComboBox.setItemLabelGenerator(BrandDto::getName);
        brandDtoComboBox.addValueChangeListener(valueChangeEvent -> {
            if (valueChangeEvent.isFromClient()) {
                brandDto = valueChangeEvent.getValue();
                fetchCategories(brandDto.getId());
            }
        });

        categoryDtoComboBox.setLabel("Category");
        categoryDtoComboBox.setItemLabelGenerator(CategoryDto::getName);
        categoryDtoComboBox.addValueChangeListener(valueChangeEvent -> {
           if (valueChangeEvent.isFromClient()) {
               fetchTier(brandDto.getId());
           }
        });

        tierDtoComboBox.setLabel("Tier");
        tierDtoComboBox.setItemLabelGenerator(TierDto::getName);
        tierDtoComboBox.addValueChangeListener(valueChangeEvent -> {
            if (valueChangeEvent.isFromClient()) {
                fetchProducts(getCategoryId(), brandDto.getId());
            }

            if (!ObjectUtils.isEmpty(valueChangeEvent.getValue())) {
                tierDto = valueChangeEvent.getValue();
            }
        });
        Button addBrandButton = new Button("Add Product");
        addBrandButton.addClickListener(buttonClickEvent -> addProduct());
        HorizontalLayout toolbar = new HorizontalLayout(brandDtoComboBox, categoryDtoComboBox,
                tierDtoComboBox, filterText, addBrandButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private HorizontalLayout getContent() {
        HorizontalLayout content = new HorizontalLayout(productDtoGrid, productForm);
        content.setFlexGrow(1, productDtoGrid);
        content.setFlexGrow(1, productForm);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(message -> {
            try {
                BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
                if (ObjectUtils.isNotEmpty(broadcastMessage) && org.apache.commons.lang3.ObjectUtils.isNotEmpty(broadcastMessage.getType())) {
                    if (broadcastMessage.getType().equals(BroadcastMessage.PRODUCT_INSERT_SUCCESS)) {
                        fetchBrands();
                    } else {
                        UiUtil.showErrorDialog(ui, this, message);
                    }
                }
            } catch (JsonProcessingException e) {
                log.error("Broadcast Handler Error", e);
            }
        });

        this.fetchBrands();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    private void fetchBrands() {

        restClientMenuService.getAllBrand()
                .subscribe(restAPIResponse -> {
                    if (!ObjectUtils.isEmpty(restAPIResponse.getData())) {

                        final List<BrandDto> brands = ObjectUtil.convertObjectToObject(restAPIResponse.getData());

                        brandDto = brands.getFirst();
                        if (!ObjectUtils.isEmpty(brands)) {
                            ui.access(() -> {
                                brandDtoComboBox.setItems(brands);
                                brandDtoComboBox.setValue(brandDto);
                            });

                            fetchCategories(brandDto.getId());
                            fetchTier(brandDto.getId());

                        }
                    }
                });
    }

    private void addProduct() {
        productDtoGrid.asSingleSelect().clear();
        editProduct(new ProductDto(), FormAction.CREATE);
    }

    public void editProduct(ProductDto productDto, FormAction formAction) {
        if (productDto == null) {
            closeEditor();
        } else {
            productForm.setProductDto(productDto);
            productForm.restructureButton(formAction);
            productForm.setVisible(true);
            addClassName("editing");
        }
    }

    private void closeEditor() {
        productForm.setVisible(false);
        removeClassName("editing");
    }

    private void fetchCategories(Integer brandId) {
        restClientMenuService.getAllCategoryByBrand(brandId)
            .subscribe(restAPIResponse -> {
                if (!ObjectUtils.isEmpty(restAPIResponse.getData())) {

                    final List<CategoryDto> categories = ObjectUtil.convertObjectToObject(
                            restAPIResponse.getData()
                    );

                    categoryDtos = categories;
                    CategoryDto categoryDto = new CategoryDto();
                    categoryDto.setId(-1);
                    categoryDto.setName("All");
                    categories.addFirst(categoryDto);
                    ui.access(()-> {
                        categoryDtoComboBox.setItems(categories);
                        categoryDtoComboBox.setValue(categories.getFirst());
                    });
                }
            });
    }

    private void fetchTier(Integer brandId) {
        restClientMenuService.getAllTierByBrand(brandId)
            .subscribe(restAPIResponse -> {
                if (!ObjectUtils.isEmpty(restAPIResponse.getData())) {
                    final List<TierDto> tierDtos = ObjectUtil.convertObjectToObject(
                            restAPIResponse.getData()
                    );

                    ui.access(()-> {
                        tierDto = tierDtos.getLast();
                        tierDtoComboBox.setItems(tierDtos);
                        tierDtoComboBox.setValue(tierDto);
                        fetchProducts(getCategoryId(), brandId);
                    });
                }
            });
    }

    private void fetchProducts(Integer categoryId, Integer brandId) {
        asyncRestClientMenuService.getAllProductCategoryBrandAsync(result -> {
            TreeData<ProductTreeItem> productDtoTreeData = new TreeData<>();
            result.forEach(productDto -> {
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
            });

            ui.access(()-> productDtoGrid.setTreeData(productDtoTreeData));

        }, categoryId, brandId);
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

}
