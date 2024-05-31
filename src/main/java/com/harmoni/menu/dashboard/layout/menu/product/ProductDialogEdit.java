package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.dto.*;
import com.harmoni.menu.dashboard.layout.enums.ProductItemAction;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.rest.data.RestClientMenuService;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.ErrorHandlerUtil;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.util.ObjectUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Route("product-dialog-edit")
public class ProductDialogEdit extends Dialog {
    private final static Logger log = LoggerFactory.getLogger(ProductListView.class);

    @Getter
    private TextField productNameField;
    @Getter
    ComboBox<CategoryDto> categoryBox;
    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientMenuService restClientMenuService;
    private UI ui;

    private final TabSheet tabSheet = new TabSheet();

    private final ProductTreeItem productTreeItem;
    private final List<SkuDto> skuDtos = new ArrayList<>();
    private final VirtualList<SkuDto> skuDtoVirtualList;
    private final TierDto tierDto;
    private final List<CategoryDto> categoryDtos;
    private List<Binder<ProductBinderBean>> binders;
    private int newSkuTempId = -1;
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
        tabSheet.addSelectedChangeListener(selectedChangeEvent -> {

        });
        return new VerticalLayout(tabSheet);
    }

    private Div productLayout() {
        Div productDiv = new Div();
        productNameField = new TextField("Product name");
        productNameField.setValue(this.productTreeItem.getName());

        categoryBox = new ComboBox<>("Category");
        categoryBox.setItemLabelGenerator(CategoryDto::getName);
        categoryBox.addValueChangeListener(valueChangeEvent -> {
        });
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

    private void setDataProvider(List<SkuDto> skuDtos) {
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
        Button saveButton = new Button("Update", this::onButtonUpdateEvent);
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
        nameField.setValue(ObjectUtils.isEmpty(skuDto.getName()) ? "" : skuDto.getName());

        final VerticalLayout verticalLayout = new VerticalLayout();
        verticalLayout.setPadding(false);

        final HorizontalLayout layout = new HorizontalLayout(nameField, verticalLayout);
        layout.setPadding(false);

        fetchPriceBySku(ObjectUtils.isEmpty(skuDto.getId()) ? -1 : skuDto.getId(), verticalLayout, nameField);

        layout.setFlexGrow(2);
        return layout;
    }

    private void fetchPriceBySku(Integer skuId, VerticalLayout verticalLayout, TextField skuNameField) {

        HorizontalLayout horizontalLayout = new HorizontalLayout();

        final BeanValidationBinder<ProductBinderBean> binder = getBinder(skuId);

        binder.forField(skuNameField)
                .withValidator(value -> value.length() > 2,
                        "Name must contain at least three characters")
                .bind(ProductBinderBean::getSkuName, ProductBinderBean::setSkuName);

        final ProductBinderBean productBinderBean = !ObjectUtils.isEmpty(binder.getBean()) ? binder.getBean()
                                                    : ProductBinderBean.builder()
                .skuId(skuId)
                .skuName(skuNameField.getValue())
                .tierId(this.tierDto.getId())
                .price(0.0)
                .build();

        final TextField text = new TextField();
        text.setValue(tierDto.getName());
        text.setEnabled(false);

        final NumberField priceField = new NumberField();

        binder.forField(priceField)
                .bind(ProductBinderBean::getPrice, ProductBinderBean::setPrice);

        binder.setBean(productBinderBean);

        Button buttonDelete = getButtonDelete(binder);

        horizontalLayout.add(text, priceField, buttonDelete);

        ui.access(() -> {
            verticalLayout.add(horizontalLayout);
        });

        if (skuId.equals(productTreeItem.getSkus().getLast().getId())) {
            fetchPrice(this.productTreeItem.getSkus());
        }
    }

    private BeanValidationBinder<ProductBinderBean> getBinder(Integer skuId) {
        BeanValidationBinder<ProductBinderBean> binder = new BeanValidationBinder<>(
                ProductBinderBean.class);

        AtomicBoolean isAlready = new AtomicBoolean(false);

        binders.forEach(productBinderBeanBinder -> {
            if (productBinderBeanBinder.getBean().getSkuId().equals(skuId)) {
                binder.setBean(productBinderBeanBinder.getBean());
                isAlready.set(true);
            }
        });

        if (!isAlready.get()) {
            binders.add(binder);
        }

        return binder;
    }

    private void fetchPrice(List<SkuDto> skus) {
        List<Integer> skuIds = new ArrayList<>();
        for (SkuDto skuDto : skus) {
            skuIds.add(skuDto.getId());
        }
        asyncRestClientMenuService.getDetailSkuTierPriceAsync(result -> {
            for (SkuTierPriceDto skuTierPriceDto : result) {
                accept(skuTierPriceDto, this.binders, this.ui);
            }
        }, skuIds, tierDto.getId());
    }

    private void onButtonUpdateEvent(ClickEvent<Button> e) {
        boolean[] isAllValid = new boolean[binders.size()];

        int i = 0;
        for (Binder<ProductBinderBean> productBinderBeanBinder : binders) {
            BinderValidationStatus<ProductBinderBean> binderBeanBinderValidationStatus =
                    productBinderBeanBinder.validate();
            if (binderBeanBinderValidationStatus.isOk() && isSkuNameEqualsTo(productBinderBeanBinder, i, binders)) {
                isAllValid[i] = true;
            }
            i++;
        }

        for (i = 0; i < isAllValid.length; i++) {
            if (!isAllValid[i]) {
                return;
            }
        }

        List<ProductSkuFormDto> skuFormDtos = new ArrayList<>();
        binders.forEach(productBeanBinder -> skuFormDtos.add(ProductSkuFormDto.builder()
                .id(productBeanBinder.getBean().getSkuId())
                .name(productBeanBinder.getBean().getSkuName())
                .tierPrice(ProductSkuTierPriceFormDto.builder()
                        .id(productBeanBinder.getBean().getTierId())
                        .price(productBeanBinder.getBean().getPrice())
                        .build())
                .build()));

        final ProductFormDto productFormDto = ProductFormDto.builder()
                .id(this.productTreeItem.getProductId())
                .name(productNameField.getValue())
                .categoryId(categoryBox.getValue().getId())
                .skus(skuFormDtos)
                .build();

        restClientMenuService.saveProductBulk(productFormDto).subscribe(restAPIResponse -> {
            if (restAPIResponse.getHttpStatus() == HttpStatus.OK.value()) {
                ui.access(this::close);
            }
        });

    }

    private boolean isSkuNameEqualsTo(Binder<ProductBinderBean> productBinderBean, int index,
                                      List<Binder<ProductBinderBean>> currentBinder) {
        int i = 0;
        for (Binder<ProductBinderBean> productBinderBeanBinder : currentBinder) {

            if (!ObjectUtils.isEmpty(productBinderBeanBinder.getBean())
                    && productBinderBeanBinder.getBean().getSkuName().equals(productBinderBean.getBean().getSkuName())
                    && i!=index) {
                return false;
            }
            i++;
        }
        return true;
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

    private Button getButtonDelete(BeanValidationBinder<ProductBinderBean> binder) {
        return new Button(new Icon(VaadinIcon.MINUS), buttonClickEvent -> {
            if (!ObjectUtils.isEmpty(this.productTreeItem.getSkus())) {
                this.productTreeItem.getSkus().forEach(skuDto -> {
                    if (skuDto.getId().equals(binder.getBean().getSkuId())) {
                        this.productTreeItem.getSkus().remove(skuDto);
                        binders.remove(binder);
                        setDataProvider(this.productTreeItem.getSkus());
                    }
                });
            }
        });
    }

    private void accept(SkuTierPriceDto skuTierPriceDto, List<Binder<ProductBinderBean>> currentBinder, UI currentUi) {
        for (Binder<ProductBinderBean> productBinder : currentBinder) {

            if (productBinder.getBean().getSkuId().equals(skuTierPriceDto.getSkuId())) {
                productBinder.getFields().forEach(hasValue -> {
                    if (hasValue instanceof NumberField numberField) {
                        currentUi.access(() -> {
                            numberField.setValue(skuTierPriceDto.getPrice());
                        });
                    }
                });
            }
        }
    }
}
