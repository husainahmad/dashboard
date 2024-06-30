package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.layout.component.SkuComponentRender;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.dto.SkuDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.event.product.ProductUpdateEventListener;
import com.harmoni.menu.dashboard.layout.menu.product.binder.ProductBinderBean;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.rest.data.RestClientMenuService;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.ArrayList;
import java.util.List;

@Route("product-dialog-edit")
@Slf4j
public class ProductDialogEdit extends Dialog {

    @Getter
    private TextField productNameField;
    @Getter
    ComboBox<CategoryDto> categoryBox;
    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final RestClientMenuService restClientMenuService;
    @Getter
    private UI ui;
    private final TabSheet tabSheet = new TabSheet();
    @Getter
    private final ProductTreeItem productTreeItem;
    private final VirtualList<SkuDto> skuDtoVirtualList;
    @Getter
    private final TierDto tierDto;
    private final List<CategoryDto> categoryDtos;
    @Getter
    private final List<Binder<ProductBinderBean>> binders;
    private int newSkuTempId = -1;
    @Getter
    private final ConfirmDialog confirmDialog = new ConfirmDialog();

    public ProductDialogEdit(@Autowired AsyncRestClientMenuService asyncRestClientMenuService,
                             @Autowired RestClientMenuService restClientMenuService,
                             ProductTreeItem productTreeItem, BrandDto brandDto, TierDto tierDto,
                             List<CategoryDto> categoryDtos) {

        this.asyncRestClientMenuService = asyncRestClientMenuService;
        this.restClientMenuService = restClientMenuService;
        this.productTreeItem = productTreeItem;
        this.skuDtoVirtualList = new VirtualList<>();
        this.tierDto = tierDto;
        this.categoryDtos = categoryDtos;
        this.binders = new ArrayList<>();
        setHeaderTitle("Edit Product");

        VerticalLayout dialogLayout = createDialogLayout();
        dialogLayout.setPadding(false);
        dialogLayout.setSpacing(false);

        add(dialogLayout);

        Button saveButton = createSaveButton(this);
        Button cancelButton = new Button("Cancel",
                (ComponentEventListener<ClickEvent<Button>>) e -> ProductDialogEdit.this.close());
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

        setDataProvider(this.productTreeItem.getSkus());

        final HorizontalLayout horizontalLayout = getHorizontalLayout();
        horizontalLayout.setWidthFull();
        final VerticalLayout titleLayout = new VerticalLayout(horizontalLayout, setDialogLayout(this.skuDtoVirtualList));
        titleLayout.setPadding(false);

        skuDiv.add(titleLayout);
        return skuDiv;
    }

    private HorizontalLayout getHorizontalLayout() {
        Button buttonAdd = new Button("Add SKU", new Icon(VaadinIcon.PLUS), this::onButtonAddSkuEvent);
        return new HorizontalLayout(FlexComponent.Alignment.STRETCH, new Text("SKU name"), buttonAdd);
    }

    public void setDataProvider(List<SkuDto> skuDtos) {
        ListDataProvider<SkuDto> dataProvider = new ListDataProvider<>(skuDtos);
        this.skuDtoVirtualList.setDataProvider(dataProvider);
        this.skuDtoVirtualList.setRenderer(new ComponentRenderer<>(this::skusRender));
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

    private Button createSaveButton(Dialog dialog) {
        Button saveButton = new Button("Update", new ProductUpdateEventListener(this, restClientMenuService));
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
        return new SkuComponentRender(skuDto, this,
                this.asyncRestClientMenuService, this.restClientMenuService);
    }

    private void onButtonAddSkuEvent(ClickEvent<Button> buttonClickEvent) {
        if (buttonClickEvent.isFromClient()) {
            SkuDto skuDto = new SkuDto();
            skuDto.setId(newSkuTempId);
            this.productTreeItem.getSkus().add(skuDto);
            setDataProvider(this.productTreeItem.getSkus());
            newSkuTempId--;
        }
    }
}
