package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.dto.ProductDto;
import com.harmoni.menu.dashboard.dto.SkuDto;
import com.harmoni.menu.dashboard.layout.enums.ProductItemType;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.rest.data.RestClientMenuService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.Route;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;

@Route("product-dialog")
public class ProductDialogEdit extends Dialog {
    private final static Logger log = LoggerFactory.getLogger(ProductListView.class);

    @Getter
    private TextField productNameField;
    @Getter
    ComboBox<CategoryDto> categoryBox;
    private  AsyncRestClientMenuService asyncRestClientMenuService;
    private AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private RestClientMenuService restClientMenuService;
    private UI ui;

    private final TabSheet tabSheet = new TabSheet();

    private ProductTreeItem productTreeItem;
    private List<SkuDto> skuDtos = new ArrayList<>();
    private final VirtualList<SkuDto> skuDtoVirtualList;
    private ListDataProvider<SkuDto> dataProvider;

    public ProductDialogEdit(@Autowired AsyncRestClientMenuService asyncRestClientMenuService,
                             @Autowired AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                             @Autowired RestClientMenuService restClientMenuService,
                             ProductTreeItem productTreeItem) {

        this.asyncRestClientMenuService = asyncRestClientMenuService;
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
        this.restClientMenuService = restClientMenuService;
        this.productTreeItem = productTreeItem;
        this.skuDtoVirtualList = new VirtualList<>();


        setHeaderTitle("Edit Product");

        VerticalLayout dialogLayout = createDialogLayout();
        add(dialogLayout);

        Button saveButton = createSaveButton(this);
        Button cancelButton = new Button("Cancel", e -> this.close());
        getFooter().add(cancelButton);
        getFooter().add(saveButton);

        fetchCategories();
    }


    private VerticalLayout createDialogLayout() {

        tabSheet.add("Product", productLayout());
        tabSheet.add("SKU", skuLayout());

        return new VerticalLayout(tabSheet);
    }

    private Div productLayout() {
        Div productDiv = new Div();
        productNameField = new TextField("Product name");
        productNameField.setValue(this.productTreeItem.getName());

        categoryBox = new ComboBox<>("Category");
        categoryBox.setItemLabelGenerator(CategoryDto::getName);
        productDiv.add(setDialogLayout(productNameField, categoryBox));
        return productDiv;
    }

    private Div skuLayout() {
        Div skuDiv = new Div();


        productNameField = new TextField("SKU name");
        skuDiv.add(setDialogLayout(productNameField));
        return skuDiv;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        getSkus(this.productTreeItem.getProductId());
    }

    private VerticalLayout setDialogLayout(Component... children) {
        VerticalLayout dialogLayout = new VerticalLayout(children);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("width", "25rem").set("max-width", "100%");
        return dialogLayout;
    }

    private static Button createSaveButton(Dialog dialog) {
        Button saveButton = new Button("Add", e -> dialog.close());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        return saveButton;
    }

    private void fetchCategories() {
        asyncRestClientMenuService.getAllCategoryAsync(result -> {
            this.getUI().get().access(() -> {
                categoryBox.setItems(result);
            });
        });
    }

    public void getSkus(Integer productId) {

        asyncRestClientMenuService.getAllSkuByProductAsync(result -> {
            log.debug("skus {}", result);
            List<Integer> skuIds = new ArrayList<>();
            result.forEach(skuDto -> {
                skuIds.add(skuDto.getId());
            });

            skuDtos = result;
            this.dataProvider = new ListDataProvider<SkuDto>(skuDtos);
            this.skuDtoVirtualList.setDataProvider(this.dataProvider);

            //this.skuDtoVirtualList.setRenderer(this.dataProvider);
            fetchPriceBySku(skuIds);

        }, productId);


    }

    private void fetchPriceBySku(List<Integer> skuIds) {
        asyncRestClientMenuService.getDetailSkuTierPriceAsync(result -> {
            result.forEach(skuTierPriceDto -> {
                log.debug("{}", skuTierPriceDto);
            });
        }, skuIds);
    }
}
