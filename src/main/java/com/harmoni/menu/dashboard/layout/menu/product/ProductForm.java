package com.harmoni.menu.dashboard.layout.menu.product;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.*;
import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.menu.ProductFormLayout;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.organization.tier.service.TreeLevel;
import com.harmoni.menu.dashboard.util.ObjectUtil;
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
    private final TreeGrid<SkuTreeItem> skuDtoGrid = new TreeGrid<>(SkuTreeItem.class);
    private final TreeData<SkuTreeItem> skuTreeItemTreeData = new TreeData<>();
    private final TreeDataProvider<SkuTreeItem> skuDataProvider;

    @Getter
    ComboBox<CategoryDto> categoryBox = new ComboBox<>();
    private final Button saveButton = new Button("Save");
    private final Button  closeButton = new Button("Cancel");

    private transient List<TierDto> tierDtos;
    private transient final BrandDto brandDto;
    private Map<String, String> skuNames = new HashMap<>();
    private Map<String, String> skuDescs = new HashMap<>();
    private Map<String, Double> skuTierPrices = new HashMap<>();

    public ProductForm(BrandDto brandDto,
                       List<CategoryDto> categoryDtos,
                       List<TierDto> tierDtos) {

        this.brandDto = brandDto;
        this.tierDtos = tierDtos;

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
        addButton.addClickListener(buttonClickEvent -> populateSkuTreeItemTreeData());

        setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1, ResponsiveStep.LabelsPosition.ASIDE));

        binder.bindInstanceFields(this);

    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        setBroadcasterRegistration(Broadcaster.register(this::acceptNotification));
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
        if (!(this.getParent().orElseThrow() instanceof Tab tab)) {
            return;
        }

        if ((!(tab.getParent().orElseThrow() instanceof TabSheet tabSheet))) {
            return;
        }

        Component content = tabSheet.getSelectedTab();
        if (content != null && content.getElement().getParent() != null) {
            content.getElement().removeFromParent();
        }

        tabSheet.setSelectedIndex(0);
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
        saveButton.addClickListener(buttonClickEvent -> {
            if (binder.validate().isOk()) {
                ProductDto productDto = populatePayload();
            }

        });
        closeButton.addClickShortcut(Key.ESCAPE);

        closeButton.addClickListener(buttonClickEvent -> removeFromSheet());
        HorizontalLayout toolbar = new HorizontalLayout( saveButton, closeButton);
        toolbar.addClassName("toolbar");
        toolbar.setAlignItems(FlexComponent.Alignment.BASELINE);

        return toolbar;
    }

    private ProductDto populatePayload() {
        ProductDto productDto = new ProductDto();
        productDto.setCategoryId(categoryBox.getValue().getId());
        productDto.setName(productNameField.getValue());
        productDto.setDescription(productDescTextArea.getValue());

        List<SkuDto> skuDtos = new ArrayList<>();

        skuDtoGrid.getTreeData().getRootItems().forEach(skuTreeItem -> {
            SkuDto skuDto = new SkuDto();
            skuDto.setName(skuTreeItem.getName());
            skuDto.setActive(true);

            SkuTierPriceDto skuTierPriceDto = new SkuTierPriceDto();
            skuTierPriceDto.setTierId(skuTreeItem.getTierId());
            skuDto.setSkuTierPriceDto(skuTierPriceDto);
            skuTierPriceDto.setPrice(skuTreeItem.getPrice());
            skuDtos.add(skuDto);
        });

        return productDto;
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

    private void acceptNotification(String message) {
        try {
            BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
            if (ObjectUtils.isNotEmpty(broadcastMessage) && ObjectUtils.isNotEmpty(broadcastMessage.getType())) {
                showBroadcastMessage(broadcastMessage);
            }
        } catch (JsonProcessingException e) {
            log.error("Broadcast Handler Error", e);
        }
    }

    @Override
    public void showBroadcastMessage(BroadcastMessage broadcastMessage) {
        if (broadcastMessage.getType().equals(BroadcastMessage.PRODUCT_INSERT_SUCCESS)) {
            removeFromSheet();
        }
        super.showBroadcastMessage(broadcastMessage);
    }

    private Button applyButtonDelete(SkuTreeItem skuTreeItem) {
        if (skuTreeItem.getTreeLevel().equals(TreeLevel.ROOT)) {
            Button button = new Button("Delete");
            button.addClickListener(buttonClickEvent -> {
                if (skuDtoGrid.getTreeData().getRootItems().size() == 1) {
                    showErrorDialog("Delete rejected!. Product should have one SKU!!");
                    return;
                }
                skuDataProvider.getTreeData().removeItem(skuTreeItem);
                skuDataProvider.refreshAll();
            });
            return button;
        }
        return null;
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
}
