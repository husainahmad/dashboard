package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.dto.*;
import com.harmoni.menu.dashboard.event.product.ProductSaveEventListener;
import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.menu.ProductFormLayout;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.organization.tier.service.TreeLevel;
import com.harmoni.menu.dashboard.rest.data.RestClientMenuService;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
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
    private final TreeDataProvider<SkuTreeItem> skuDataProvider;

    @Getter
    ComboBox<CategoryDto> categoryBox = new ComboBox<>();
    private final Button saveButton = new Button("Save");
    private final Button  closeButton = new Button("Cancel");

    private transient List<TierDto> tierDtos;
    private final transient BrandDto brandDto;
    @Getter
    private Map<String, String> skuNames = new HashMap<>();
    @Getter
    private Map<String, String> skuDescs = new HashMap<>();
    @Getter
    private Map<String, Double> skuTierPrices = new HashMap<>();
    private RestClientMenuService restClientMenuService;
    private final Tab productTab;

    public ProductForm(RestClientMenuService restClientMenuService,
                       BrandDto brandDto,
                       List<CategoryDto> categoryDtos,
                       List<TierDto> tierDtos, Tab productTab) {

        this.restClientMenuService = restClientMenuService;
        this.brandDto = brandDto;
        this.tierDtos = tierDtos;
        this.productTab = productTab;

        categoryBox.setLabel("Category");
        categoryBox.setItems(categoryDtos);
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

        populateSkuTreeItemTreeData();

        configureGrid();
        addValidation();

        Button addButton = new Button("Add SKU");
        add(getToolbar(addButton), getContent(skuDtoGrid), getButtonBar());
        addButton.addClickListener(this::onButtonAddEvent);

        setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1, ResponsiveStep.LabelsPosition.ASIDE));

        binder.bindInstanceFields(this);

    }

    private void configureGrid() {
        skuDtoGrid.removeAllColumns();
        skuDtoGrid.addComponentHierarchyColumn(this::applySkuNameTextField).setHeader("Name");
        skuDtoGrid.addComponentColumn(this::applySkuDescTextArea).setHeader("Description");
        skuDtoGrid.addColumn(SkuTreeItem::getTierName).setHeader("Tier");
        skuDtoGrid.addComponentColumn(this::applySkuPriceNumberField).setHeader("Price");
        skuDtoGrid.addComponentColumn(this::applyButtonDelete);
    }

    private void populateSkuTreeItemTreeData() {

        TierDto firstTierDto = tierDtos.getFirst();
        if (firstTierDto==null) return;

        SkuTreeItem rootSkuTreeItem = SkuTreeItem.builder()
                .id(String.valueOf(UUID.randomUUID()))
                .name("")
                .desc("")
                .tierId(firstTierDto.getId())
                .tierName(firstTierDto.getName())
                .price(0.0)
                .treeLevel(TreeLevel.ROOT)
                .build();

        skuTreeItemTreeData.addItem(null, rootSkuTreeItem);

        AtomicInteger i = new AtomicInteger();
        tierDtos.forEach(tierDto -> {
            if (!tierDto.equals(tierDtos.getFirst())) {
                SkuTreeItem skuTreeItemTier = SkuTreeItem.builder()
                        .id(UUID.randomUUID().toString()
                                .concat(String.valueOf(i.getAndIncrement())))
                        .tierName(tierDto.getName())
                        .tierId(tierDto.getId())
                        .price(0.0)
                        .treeLevel(TreeLevel.PARENT)
                        .build();
                skuTreeItemTreeData.addItems(rootSkuTreeItem, skuTreeItemTier);
            }
        });
        skuDataProvider.refreshItem(rootSkuTreeItem, true);
        skuDataProvider.refreshAll();
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
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        saveButton.addClickShortcut(Key.ENTER);
        saveButton.addClickListener(new ProductSaveEventListener(this, restClientMenuService));
        closeButton.addClickShortcut(Key.ESCAPE);

        closeButton.addClickListener(this::onButtonClose);
        HorizontalLayout toolbar = new HorizontalLayout( saveButton, closeButton);
        toolbar.addClassName("toolbar");
        toolbar.setAlignItems(FlexComponent.Alignment.BASELINE);

        return toolbar;
    }

    public void restructureButton(FormAction formAction) {
        if (Objects.requireNonNull(formAction) == FormAction.CREATE) {
            saveButton.setVisible(true);
            closeButton.setVisible(true);
        } else if (formAction == FormAction.EDIT) {
            saveButton.setVisible(false);
            closeButton.setVisible(true);
        }
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
            textField.setValue(skuNames.get(skuTreeItem.getId()) != null ? skuNames.get(skuTreeItem.getId()) :
                    skuTreeItem.getName());
            textField.addValueChangeListener(changeEvent ->
                    skuNames.put(skuTreeItem.getId(), changeEvent.getValue()));
            return textField;
        }
        return null;
    }

    private TextArea applySkuDescTextArea(SkuTreeItem skuTreeItem) {
        if (skuTreeItem.getTreeLevel().equals(TreeLevel.ROOT)) {
            TextArea textArea = new TextArea();
            textArea.setValue(skuDescs.get(skuTreeItem.getId()) != null ? skuDescs.get(skuTreeItem.getId()) :
                    skuTreeItem.getDesc());
            textArea.addValueChangeListener(changeEvent -> skuDescs.put(skuTreeItem.getId(), changeEvent.getValue()));
            return textArea;
        }
        return null;
    }

    private NumberField applySkuPriceNumberField(SkuTreeItem skuTreeItem) {
        NumberField numberField = new NumberField();
        numberField.setValue(skuTierPrices.get(skuTreeItem.getId()) != null ? skuTierPrices.get(skuTreeItem.getId()) :
                skuTreeItem.getPrice());
        numberField.addValueChangeListener(changeEvent ->
                skuTierPrices.put(skuTreeItem.getId(), changeEvent.getValue()));
        return numberField;
    }

    private void onButtonClose(ClickEvent<Button> buttonClickEvent) {
        removeFromSheet();
    }

    private void onButtonAddEvent(ClickEvent<Button> buttonClickEvent) {
        populateSkuTreeItemTreeData();
    }
}
