package com.harmoni.menu.dashboard.layout.menu.product;

import com.fasterxml.jackson.core.type.TypeReference;
import com.harmoni.menu.dashboard.dto.*;
import com.harmoni.menu.dashboard.event.product.ProductSaveEventListener;
import com.harmoni.menu.dashboard.event.product.ProductUpdateEventListener;
import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.menu.ProductFormLayout;
import com.harmoni.menu.dashboard.layout.organization.tier.service.TreeLevel;
import com.harmoni.menu.dashboard.rest.data.RestClientMenuService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Route;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Route(value = "product-form", layout = MainLayout.class)
@Slf4j
public class ProductForm extends ProductFormLayout {

    @Getter
    BeanValidationBinder<ProductDto> binder = new BeanValidationBinder<>(ProductDto.class);
    @Getter
    TextField productNameField = new TextField();
    @Getter
    TextArea productDescTextArea = new TextArea();
    @Getter
    private final TreeGrid<SkuTreeItem> skuDtoGrid = new TreeGrid<>(SkuTreeItem.class);
    private final TreeData<SkuTreeItem> skuTreeItemTreeData = new TreeData<>();
    private TreeDataProvider<SkuTreeItem> skuDataProvider;

    @Getter
    ComboBox<CategoryDto> categoryBox = new ComboBox<>();
    private final Button saveButton = new Button("Save");
    private final Button updateButton = new Button("Update");
    private final Button  closeButton = new Button("Cancel");

    private transient List<TierDto> tierDtos;
    private final transient BrandDto brandDto;
    @Getter
    private Map<String, String> skuNames = new HashMap<>();
    @Getter
    private Map<String, String> skuDescs = new HashMap<>();
    @Getter
    private Map<String, Double> skuTierPrices = new HashMap<>();
    private final RestClientMenuService restClientMenuService;
    private final Tab productTab;
    private final ProductTreeItem productTreeItem;
    @Getter
    private transient ProductDto productDto;
    private final transient List<CategoryDto> categoryDtos;

    public ProductForm(RestClientMenuService restClientMenuService,
                       BrandDto brandDto,
                       List<CategoryDto> categoryDtos,
                       List<TierDto> tierDtos, Tab productTab, ProductTreeItem productTreeItem) {

        this.restClientMenuService = restClientMenuService;
        this.brandDto = brandDto;
        this.categoryDtos = categoryDtos;
        this.tierDtos = tierDtos;
        this.productTab = productTab;
        this.productTreeItem = productTreeItem;
    }

    private void renderLayout() {

        categoryBox.setLabel("Category");
        categoryBox.setItems(categoryDtos);
        categoryBox.setValue(categoryDtos.getLast());
        categoryBox.setItemLabelGenerator(CategoryDto::getName);

        add(categoryBox);

        productNameField.setLabel("Product name");
        productNameField.setPlaceholder("Enter Product name...");
        productNameField.setClearButtonVisible(true);
        productNameField.setValueChangeMode(ValueChangeMode.LAZY);
        add(productNameField);

        productDescTextArea.setLabel("Description");
        productDescTextArea.setPlaceholder("Enter Description");
        productNameField.setValueChangeMode(ValueChangeMode.LAZY);

        add(productDescTextArea);

        setSizeFull();

        skuDataProvider = new TreeDataProvider<>(skuTreeItemTreeData);

        skuDtoGrid.setDataProvider(skuDataProvider);
        skuDtoGrid.setSelectionMode(Grid.SelectionMode.NONE);

        populateSkuTreeItemTreeData(null);

        configureGrid();
        addValidation();

        Button addButton = new Button("Add SKU");
        add(getToolbar(addButton), getContent(skuDtoGrid), getButtonBar());
        addButton.addClickListener(this::onButtonAddEvent);

        setResponsiveSteps(new ResponsiveStep("0", 1, ResponsiveStep.LabelsPosition.ASIDE));

        binder.bindInstanceFields(this);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        renderLayout();
        fetchProduct();
    }

    private void fetchProduct() {
        if (ObjectUtils.isNotEmpty(productTreeItem)) {
            this.restClientMenuService.getProduct(productTreeItem.getProductId())
                .subscribe(restAPIResponse -> {
                    if (ObjectUtils.isNotEmpty(restAPIResponse.getData())) {
                        productDto = ObjectUtil.convertObjectToObject(restAPIResponse.getData(), new TypeReference<>() {
                        });

                        skuDataProvider.getTreeData().clear();

                        getUi().access(() -> {
                            categoryBox.setValue(productDto.getCategoryDto());
                            productNameField.setValue(productDto.getName());
                            productDescTextArea.setValue(productDto.getDescription()==null ? "" : productDto.getDescription());
                            productDto.getSkuDtos().forEach(this::populateSkuTreeItemTreeData);
                        });
                    }
                });
        }
    }

    private void configureGrid() {
        skuDtoGrid.removeAllColumns();
        skuDtoGrid.addComponentHierarchyColumn(this::applySkuNameTextField).setHeader("Name");
        skuDtoGrid.addComponentColumn(this::applySkuDescTextArea).setHeader("Description");
        skuDtoGrid.addColumn(SkuTreeItem::getTierName).setHeader("Tier");
        skuDtoGrid.addComponentColumn(this::applySkuPriceNumberField).setHeader("Price");
        skuDtoGrid.addComponentColumn(this::applyButtonDelete);
    }

    private void populateSkuTreeItemTreeData(SkuDto skuDto) {

        TierDto firstTierDto = tierDtos.getFirst();
        if (firstTierDto==null) return;

        SkuTreeItem rootSkuTreeItem = SkuTreeItem.builder()
                .id(String.valueOf(UUID.randomUUID()))
                .skuId(Optional.ofNullable(skuDto)
                        .map(SkuDto::getId)
                        .orElse(null))
                .skuName(Optional.ofNullable(skuDto)
                        .map(SkuDto::getName)
                        .orElse(""))
                .skuDesc(Optional.ofNullable(skuDto)
                        .map(SkuDto::getDescription)
                        .orElse(""))
                .tierId(firstTierDto.getId())
                .tierName(firstTierDto.getName())
                .price(getPriceBySkuAndTier(skuDto, firstTierDto))
                .treeLevel(TreeLevel.ROOT)
                .build();

        skuNames.put(rootSkuTreeItem.getId(), rootSkuTreeItem.getSkuName());

        skuTreeItemTreeData.addItem(null, rootSkuTreeItem);

        AtomicInteger i = new AtomicInteger();
        tierDtos.forEach(tierDto -> {
            if (!tierDto.equals(tierDtos.getFirst())) {
                SkuTreeItem skuTreeItemTier = getChildSkuTreeItem(skuDto, tierDto, i);
                skuTreeItemTreeData.addItems(rootSkuTreeItem, skuTreeItemTier);
            }
        });

        skuDataProvider.refreshItem(rootSkuTreeItem, true);
        skuDataProvider.refreshAll();
    }

    private static SkuTreeItem getChildSkuTreeItem(SkuDto skuDto, TierDto tierDto, AtomicInteger i) {
        return SkuTreeItem.builder()
            .id(UUID.randomUUID().toString()
                    .concat(String.valueOf(i.getAndIncrement())))
            .skuId(Optional.ofNullable(skuDto)
                    .map(SkuDto::getId)
                    .orElse(null))
            .tierName(tierDto.getName())
            .tierId(tierDto.getId())
            .price(getPriceBySkuAndTier(skuDto, tierDto))
            .treeLevel(TreeLevel.PARENT)
            .build();
    }

    private static Double getPriceBySkuAndTier(SkuDto skuDto, TierDto tierDto) {
        if (skuDto == null || tierDto == null || skuDto.getSkuTierPriceDtos() == null) {
            return 0.0;
        }

        return skuDto.getSkuTierPriceDtos().stream()
                .filter(skuTierPriceDto -> tierDto.getId().equals(skuTierPriceDto.getTierId()))
                .map(SkuTierPriceDto::getPrice)
                .findFirst()
                .orElse(0.0);
    }

    public void removeFromSheet() {
        getUi().access(() -> {
            if (!(this.getParent().orElseThrow() instanceof TabSheet tabSheet)) {
                return;
            }
            tabSheet.remove(productTab);
        });
    }

    private void addValidation() {

        categoryBox.addValueChangeListener(changeEvent -> binder.validate());
        binder.forField(categoryBox)
                        .withValidator(value -> (value==null || value.getId() > 0), "Category not allow to be empty"
                        ).bind(ProductDto::getCategoryDto, ProductDto::setCategoryDto);
        productNameField.addValueChangeListener(
                (HasValue.ValueChangeListener<AbstractField
                        .ComponentValueChangeEvent<TextField, String>>) changeEvent -> binder.validate());

        binder.forField(productNameField)
                .withValidator(value -> value.length() > 2,
                        "Name must contain at least three characters")
                .bind(ProductDto::getName, ProductDto::setName);
    }

    private HorizontalLayout getButtonBar() {
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        updateButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        saveButton.addClickShortcut(Key.ENTER);
        updateButton.addClickShortcut(Key.ENTER);

        updateButton.addClickListener(new ProductUpdateEventListener(this, restClientMenuService));
        saveButton.addClickListener(new ProductSaveEventListener(this, restClientMenuService));
        closeButton.addClickShortcut(Key.ESCAPE);

        closeButton.addClickListener(this::onButtonClose);
        HorizontalLayout toolbar = new HorizontalLayout((this.productTreeItem != null ? updateButton : saveButton), closeButton);
        toolbar.addClassName("toolbar");
        toolbar.setAlignItems(FlexComponent.Alignment.BASELINE);

        return toolbar;
    }

    private Button applyButtonDelete(SkuTreeItem skuTreeItem) {
        if (skuTreeItem.getTreeLevel().equals(TreeLevel.ROOT)) {
            Button button = new Button("Delete");
            button.addClickListener(buttonClickEvent -> onDeleteSku(skuTreeItem));
            return button;
        }
        return null;
    }

    private void onDeleteSku(SkuTreeItem skuTreeItem) {
        if (skuDtoGrid.getTreeData().getRootItems().size() == 1) {
            showErrorDialog("Delete rejected!. Product should have one SKU!!");
            return;
        }
        skuDataProvider.getTreeData().removeItem(skuTreeItem);
        skuDataProvider.refreshAll();
    }

    private TextField applySkuNameTextField(SkuTreeItem skuTreeItem) {
        if (skuTreeItem.getTreeLevel().equals(TreeLevel.ROOT)) {
            TextField textField = new TextField();
            textField.setValue(Optional.ofNullable(skuNames.get(skuTreeItem.getId()))
                    .orElse(skuTreeItem.getSkuName()));
            textField.addValueChangeListener(changeEvent ->
                    skuNames.put(skuTreeItem.getId(), changeEvent.getValue()));
            return textField;
        }
        return null;
    }

    private TextArea applySkuDescTextArea(SkuTreeItem skuTreeItem) {
        if (skuTreeItem.getTreeLevel().equals(TreeLevel.ROOT)) {
            TextArea textArea = new TextArea();
            textArea.setValue(Optional.ofNullable(skuDescs.get(skuTreeItem.getId()))
                    .orElse(skuTreeItem.getSkuDesc()));
            textArea.addValueChangeListener(changeEvent -> skuDescs.put(skuTreeItem.getId(), changeEvent.getValue()));
            return textArea;
        }
        return null;
    }

    private NumberField applySkuPriceNumberField(SkuTreeItem skuTreeItem) {
        NumberField numberField = new NumberField();
        numberField.setValue(Optional.ofNullable(skuTierPrices.get(skuTreeItem.getId()))
                .orElse(skuTreeItem.getPrice()));
        numberField.addValueChangeListener(changeEvent ->
                skuTierPrices.put(skuTreeItem.getId(), changeEvent.getValue()));
        skuTierPrices.put(skuTreeItem.getId(), numberField.getValue());
        return numberField;
    }

    private void onButtonClose(ClickEvent<Button> buttonClickEvent) {
        removeFromSheet();
    }

    private void onButtonAddEvent(ClickEvent<Button> buttonClickEvent) {
        populateSkuTreeItemTreeData(null);
    }
}
