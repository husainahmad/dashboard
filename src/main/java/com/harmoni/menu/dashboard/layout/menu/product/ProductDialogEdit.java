package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.dto.*;
import com.harmoni.menu.dashboard.layout.enums.ProductItemType;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.rest.data.RestClientMenuService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
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
    private final List<SkuDto> skuDtos = new ArrayList<>();
    private final VirtualList<SkuDto> skuDtoVirtualList;
    private final TierDto tierDto;
    private final List<CategoryDto> categoryDtos;

    public ProductDialogEdit(@Autowired AsyncRestClientMenuService asyncRestClientMenuService,
                             @Autowired AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                             @Autowired RestClientMenuService restClientMenuService,
                             ProductTreeItem productTreeItem, BrandDto brandDto, TierDto tierDto,
                             List<CategoryDto> categoryDtos) {

        this.asyncRestClientMenuService = asyncRestClientMenuService;
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
        this.restClientMenuService = restClientMenuService;
        this.productTreeItem = productTreeItem;
        this.skuDtoVirtualList = new VirtualList<>();
        this.tierDto = tierDto;
        this.categoryDtos = categoryDtos;

        setHeaderTitle("Edit Product");

        VerticalLayout dialogLayout = createDialogLayout();
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);

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
        ListDataProvider<SkuDto> dataProvider = new ListDataProvider<SkuDto>(this.productTreeItem.getSkus());
        this.skuDtoVirtualList.setDataProvider(dataProvider);
        this.skuDtoVirtualList.setRenderer(new ComponentRenderer<>(this::skusRender));

        final VerticalLayout titleLayout = new VerticalLayout(new Text("SKU name"), setDialogLayout(this.skuDtoVirtualList));
        titleLayout.setPadding(false);

        skuDiv.add(titleLayout);
        return skuDiv;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        ui = attachEvent.getUI();
    }

    private VerticalLayout setDialogLayout(Component... children) {
        VerticalLayout dialogLayout = new VerticalLayout(children);
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);
        dialogLayout.setAlignItems(FlexComponent.Alignment.STRETCH);
        dialogLayout.getStyle().set("width", "45rem").set("max-width", "100%");
        return dialogLayout;
    }

    private static Button createSaveButton(Dialog dialog) {
        Button saveButton = new Button("Update", e -> dialog.close());
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        return saveButton;
    }

    private void fetchCategories() {
        categoryBox.setItems(this.categoryDtos);
        this.categoryDtos.forEach(categoryDto -> {
            if (categoryDto.getId().equals(this.productTreeItem.getCategoryId())) {
                categoryBox.setValue(categoryDto);
            }
        });
    }

    private Component skusRender(SkuDto skuDto) {

        final TextField nameField = new TextField();
        nameField.setValue(skuDto.getName());

        final VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setPadding(false);

        final HorizontalLayout layout = new HorizontalLayout(nameField, verticalLayout);
        layout.setPadding(false);

        fetchPriceBySku(skuDto.getId(), verticalLayout);

        layout.setFlexGrow(2);
        return layout;
    }

    private void fetchPriceBySku(Integer skuId, VerticalLayout verticalLayout) {
        List<Integer> skuIds = new ArrayList<>();
        skuIds.add(skuId);

        asyncRestClientMenuService.getDetailSkuTierPriceAsync(result -> {
            HorizontalLayout horizontalLayout = new HorizontalLayout();

            if (ObjectUtils.isEmpty(result)) {

                final BeanValidationBinder<SkuTierPriceDto> binder = new BeanValidationBinder<>(
                        SkuTierPriceDto.class);

                final TextField text = new TextField();
                text.setValue(tierDto.getName());
                text.setEnabled(false);

                final NumberField priceField = new NumberField();
                priceField.setVisible(false);

                binder.forField(priceField).bind("price");
                binder.addValueChangeListener(valueChangeEvent -> {
                    if (binder.isValid()) {

                    }
                });

                final Button addButton = new Button("Add");
                addButton.addClickListener(buttonClickEvent -> {
                    priceField.setVisible(true);
                    addButton.setVisible(false);
                });
                horizontalLayout.add(text, priceField, addButton);
            }
            ui.access(() -> {
                result.forEach(skuTierPriceDto -> {
                    log.debug("{}", skuTierPriceDto);
                    final TextField text = new TextField();
                    text.setValue(skuTierPriceDto.getTierDto().getName());
                    text.setEnabled(false);
                    final NumberField priceField = new NumberField();
                    priceField.setValue(skuTierPriceDto.getPrice().doubleValue());
                    final BeanValidationBinder<SkuTierPriceDto> binder = new BeanValidationBinder<>(
                            SkuTierPriceDto.class);
                    binder.forField(priceField).bind("price");
                    binder.addValueChangeListener(valueChangeEvent -> {
                        log.debug("{}", valueChangeEvent.getValue());
                        if (binder.isValid()) {

                        }
                    });

                    horizontalLayout.add(text);
                    horizontalLayout.add(priceField);
                });

                verticalLayout.add(horizontalLayout);
            });

        }, skuIds, tierDto.getId());
    }
}
