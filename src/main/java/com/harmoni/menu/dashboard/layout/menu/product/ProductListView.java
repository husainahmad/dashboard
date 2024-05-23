package com.harmoni.menu.dashboard.layout.menu.product;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.dto.ProductDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.component.DialogClosing;
import com.harmoni.menu.dashboard.layout.enums.ProductItemType;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.rest.data.RestClientMenuService;
import com.vaadin.flow.component.AttachEvent;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

@UIScope
@PreserveOnRefresh
@Route(value = "product", layout = MainLayout.class)
@PageTitle("Product | POSHarmoni")
@Component
public class ProductListView extends VerticalLayout {

    private final static Logger log = LoggerFactory.getLogger(ProductListView.class);
    Registration broadcasterRegistration;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            .configure(JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES, true);

    private final TreeGrid<ProductTreeItem> productDtoGrid = new TreeGrid<>(ProductTreeItem.class);
    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientMenuService restClientMenuService;

    private final TextField filterText = new TextField();
    private final ComboBox<TierDto> tierDtoComboBox = new ComboBox<>();
    private final ComboBox<BrandDto> brandDtoComboBox = new ComboBox<>();
    private final ComboBox<CategoryDto> categoryDtoComboBox = new ComboBox<>();
    private ProductForm productForm;
    private TierDto tierDto;
    private BrandDto brandDto;
    private List<CategoryDto> categoryDtos;
    private UI ui;
    private int numFromClient = 0;
    public ProductListView(@Autowired AsyncRestClientMenuService asyncRestClientMenuService,
                           @Autowired AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                           @Autowired RestClientMenuService restClientMenuService) {

        this.asyncRestClientMenuService = asyncRestClientMenuService;
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
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
                            this.asyncRestClientOrganizationService,
                            this.restClientMenuService,
                            button.getProductTreeItem(),
                            this.brandDtoComboBox.getValue(),
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

        productDtoGrid.addCollapseListener(event -> {
            event.getItems().forEach(productTreeItem -> {
                log.debug("item collapse {}", productTreeItem);
            });
        });

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

        productDtoGrid.asSingleSelect().addValueChangeListener(valueChangeEvent -> {
        });

        productDtoGrid.getColumns().forEach(productDtoColumn -> productDtoColumn.setAutoWidth(true));

    }

    private void configureForm() {
        productForm = new ProductForm(this.asyncRestClientMenuService,
                this.asyncRestClientOrganizationService,
                this.restClientMenuService);
        productForm.setWidth("25em");
    }


    private HorizontalLayout getToolbar() {
        filterText.setLabel("Product");
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);

        brandDtoComboBox.setLabel("Brand");
        brandDtoComboBox.setItems(new ArrayList<BrandDto>());
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
        addBrandButton.addClickListener(buttonClickEvent -> {
            addProduct();
        });
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
            if (message.equals(BroadcastMessage.PRODUCT_INSERT_SUCCESS)) {
                fetchProducts(getCategoryId(), brandDto.getId());
            }
            if (message.startsWith(MessageFormat.format("{0}|", String.valueOf(HttpStatus.BAD_REQUEST.value())))) {
                showErrorDialog(message);
            }
            if (message.startsWith(MessageFormat.format("{0}|", String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value())))) {
                showErrorDialog(message);
            }
        });

        this.fetchBrands();
    }

    private void fetchBrands() {

        restClientMenuService.getAllBrand()
                .subscribe(restAPIResponse -> {
                    if (!ObjectUtils.isEmpty(restAPIResponse.getData())) {

                        final List<BrandDto> brands = objectMapper.convertValue(
                                Objects.requireNonNull(restAPIResponse.getData()),
                                new TypeReference<>() {
                                });

                        brandDto = brands.getFirst();
                        if (!ObjectUtils.isEmpty(brands)) {
                            ui.access(() -> {
                                brandDtoComboBox.setItems(brands);
                                brandDtoComboBox.setValue(brandDto);
                            });

                            fetchCategories(brandDto.getId());
                            fetchTier(brandDto.getId());

                        }
                        numFromClient++;
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

    private void showErrorDialog(String message) {
        DialogClosing dialog = new DialogClosing(message);
        ui.access(()-> {
            add(dialog);
            dialog.open();
        });
    }


    private void fetchCategories(Integer brandId) {
        restClientMenuService.getAllCategoryByBrand(brandId)
            .subscribe(restAPIResponse -> {
                if (!ObjectUtils.isEmpty(restAPIResponse.getData())) {

                    final List<CategoryDto> categories = objectMapper.convertValue(
                            Objects.requireNonNull(restAPIResponse.getData()),
                            new TypeReference<>() {
                            });

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
                    final List<TierDto> tierDtos = objectMapper.convertValue(
                            Objects.requireNonNull(restAPIResponse.getData()),
                            new TypeReference<>() {
                            });

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

            ui.access(()-> {
                productDtoGrid.setTreeData(productDtoTreeData);
            });

        }, categoryId, brandId);
    }

    public List<ProductTreeItem> getSkus(ProductDto productDto) {
        List<ProductTreeItem> productTreeItems = new ArrayList<>();
        productDto.getSkuDtos().forEach(skuDto -> {
            productTreeItems.add(ProductTreeItem.builder()
                            .id("%s|%d".formatted(ProductItemType.SKU, skuDto.getId()))
                            .name(skuDto.getName())
                            .productItemType(ProductItemType.SKU)
                            .productId(productDto.getId())
                            .skuId(skuDto.getId())
                            .categoryName("")
                            .price(0.0)
                    .build());
        });
        return productTreeItems;
    }

    private void fetchPriceBySku(List<Integer> skuIds,
                                 List<ProductTreeItem> productTreeItems, Integer tierId) {
        asyncRestClientMenuService.getDetailSkuTierPriceAsync(result -> {
            result.forEach(skuTierPriceDto -> {
                ProductTreeItem productTreeItem = productTreeItems.stream()
                        .filter(productItem -> skuTierPriceDto.getSkuId().equals(productItem.getSkuId()))
                        .findAny()
                        .orElse(null);
                log.debug("{}", productTreeItem);
                if (!ObjectUtils.isEmpty(productTreeItem)) {
                    productTreeItem.setPrice(skuTierPriceDto.getPrice());
                    productTreeItem.setTierName(skuTierPriceDto.getTierDto().getName());
                }
            });
        }, skuIds, tierId);
    }

    private Integer getCategoryId() {
        Integer categoryId = -1;
        if (!ObjectUtils.isEmpty(categoryDtoComboBox.getValue())) {
            categoryId = categoryDtoComboBox.getValue().getId();
        }
        return categoryId;
    }

}
