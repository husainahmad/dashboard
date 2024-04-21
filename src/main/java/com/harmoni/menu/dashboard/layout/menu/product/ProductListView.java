package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.ProductDto;
import com.harmoni.menu.dashboard.dto.SkuDto;
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
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.editor.Editor;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.LumoUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.ObjectUtils;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Route(value = "product", layout = MainLayout.class)
@PageTitle("Product | POSHarmoni")
public class ProductListView extends VerticalLayout {

    private final static Logger log = LoggerFactory.getLogger(ProductListView.class);

    Registration broadcasterRegistration;

    private final TreeGrid<ProductTreeItem> productDtoGrid = new TreeGrid<>(ProductTreeItem.class);
    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientMenuService restClientMenuService;

    private final TextField filterText = new TextField();
    private ProductForm productForm;
    private ProductDialogEdit productDialogEdit;
    private UI ui;

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

            ProductEditButton button = new ProductEditButton(productTreeItem);
            button.addClickListener(buttonClickEvent -> {

                productDialogEdit = new ProductDialogEdit(this.asyncRestClientMenuService,
                        this.asyncRestClientOrganizationService,
                        this.restClientMenuService,
                        button.getProductTreeItem());

                add(productDialogEdit);
                productDialogEdit.open();

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
           event.getItems().forEach(productTreeItem -> {
               log.debug("item expand {}", productTreeItem);
               List<Integer> skuIds = new ArrayList<>();
               List<ProductTreeItem> productTreeItems = new ArrayList<>();
               event.getSource().getTreeData().getChildren(productTreeItem).forEach(productTreeItemChild -> {
                   log.debug("item expand child {}", productTreeItemChild);
                   skuIds.add(productTreeItemChild.getSkuId());
                   productTreeItems.add(productTreeItemChild);
                   productTreeItemChild.setPrice(0.0);
               });
               log.debug("skuids {}", skuIds);
               log.debug("productTreeItems {}", productTreeItems);

               fetchPriceBySku(skuIds, productTreeItems);
           });

        });

        productDtoGrid.asSingleSelect().addValueChangeListener(valueChangeEvent -> {
//            if (valueChangeEvent.getValue().getProductItemType()==ProductItemType.SKU) {
//
//            }
        });


        productDtoGrid.getColumns().forEach(productDtoColumn -> productDtoColumn.setAutoWidth(true));
//        productDtoGrid.asSingleSelect().addValueChangeListener(valueChangeEvent ->
//                editProduct(valueChangeEvent.getValue(), FormAction.EDIT));


    }

    private void configureForm() {
        productForm = new ProductForm(this.asyncRestClientMenuService,
                this.asyncRestClientOrganizationService,
                this.restClientMenuService);
        productForm.setWidth("25em");
    }

    private HorizontalLayout getToolbar() {
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);

        Button addBrandButton = new Button("Add Product");
        addBrandButton.addClickListener(buttonClickEvent -> {
            addProduct();
        });
        HorizontalLayout toolbar = new HorizontalLayout(filterText, addBrandButton);
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
                fetchProducts();
            }
            if (message.startsWith(MessageFormat.format("{0}|", String.valueOf(HttpStatus.BAD_REQUEST.value())))) {
                showErrorDialog(message);
            }
            if (message.startsWith(MessageFormat.format("{0}|", String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value())))) {
                showErrorDialog(message);
            }
        });
        fetchProducts();
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

    private void fetchProducts() {
        asyncRestClientMenuService.getAllProductAsync(result -> {
            TreeData<ProductTreeItem> productDtoTreeData = new TreeData<>();
            result.forEach(productDto -> {
                ProductTreeItem productTreeItem =  ProductTreeItem.builder()
                        .id("%s|%d".formatted(ProductItemType.PRODUCT, productDto.getId()))
                        .name(productDto.getName())
                        .productId(productDto.getId())
                        .categoryId(productDto.getCategoryId())
                        .categoryName(productDto.getCategoryDto().getName())
                        .productItemType(ProductItemType.PRODUCT)
                        .build();
                productDtoTreeData.addItems(null, productTreeItem);
                productDtoTreeData.addItems(productTreeItem, getSkus(productDto));

            });

            ui.access(()-> {
                productDtoGrid.setTreeData(productDtoTreeData);
            });
        });
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

    private void fetchPriceBySku(List<Integer> skuIds, List<ProductTreeItem> productTreeItems) {
        asyncRestClientMenuService.getDetailSkuTierPriceAsync(result -> {
            result.forEach(skuTierPriceDto -> {
                ProductTreeItem productTreeItem = productTreeItems.stream()
                        .filter(productItem -> skuTierPriceDto.getSkuId().equals(productItem.getSkuId()))
                        .findAny()
                        .orElse(null);
                log.debug("{}", productTreeItem);
                if (!ObjectUtils.isEmpty(productTreeItem)) {
                    productTreeItem.setPrice(skuTierPriceDto.getPrice().doubleValue());
                    productTreeItem.setTierName(skuTierPriceDto.getTierDto().getName());
                }
            });
        }, skuIds);
    }
}
