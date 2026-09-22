package com.harmoni.menu.dashboard.layout.menu.customization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.CustomizationDto;
import com.harmoni.menu.dashboard.dto.CustomizationOptionDto;
import com.harmoni.menu.dashboard.dto.CustomizationOptionTierPriceDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.dto.TierTypeDto;
import com.harmoni.menu.dashboard.event.customization.CustomizationSaveEventListener;
import com.harmoni.menu.dashboard.event.customization.CustomizationUpdateEventListener;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.layout.enums.SelectionType;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.organization.tier.service.TreeLevel;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tab form for creating or editing a {@link CustomizationDto}.
 *
 * <p>
 * Edits the customization header (name, description, selection type, required,
 * min/max selection) through a {@link Binder} and the options through a
 * {@link TreeGrid} where each root row is an option and its children the
 * per-tier prices. Price tiers are loaded for the owning brand, and a non-null
 * {@code editCustomization} pre-populates the fields and the option tree.
 * </p>
 *
 * <p>
 * Save and update are handled by {@link CustomizationSaveEventListener} and
 * {@link CustomizationUpdateEventListener}; on success the hosting tab is
 * closed through {@link TabManager}.
 * </p>
 */
@Slf4j
public class CustomizationForm extends VerticalLayout {

    private static final String ACTIVE = "Active";
    private static final String INACTIVE = "Inactive";

    /** Binder driving validation of the customization header fields. */
    @Getter
    private final Binder<CustomizationDto> customizationBinder = new Binder<>(CustomizationDto.class);

    // Form fields
    /** Customization name input. */
    @Getter private final TextField nameField = new TextField("Customization name");
    /** Free-text customization description input. */
    @Getter private final TextArea descriptionField = new TextArea("Description");
    /** Single / multiple selection type combo. */
    @Getter private final ComboBox<SelectionType> selectionType = new ComboBox<>("Selection Type");
    /** Whether the customization is mandatory. */
    @Getter private final Checkbox requiredField = new Checkbox("Required");
    /** Minimum number of options the user must select. */
    @Getter private final IntegerField minSelectionField = new IntegerField("Minimum Selection");
    /** Maximum number of options the user may select. */
    @Getter private final IntegerField maxSelectionField = new IntegerField("Maximum Selection");

    // Buttons
    private final Button addOptionButton = new Button("+ Add Option");
    private final Button deleteOptionButton = new Button(VaadinIcon.TRASH.create());
    private final Button saveButton = new Button("Save");
    private final Button updateButton = new Button("Update");
    private final Button closeButton = new Button("Cancel");

    // Option tree grid (mirrors the product SKU + tier price layout)
    private final TreeGrid<CustomizationOptionTreeItem> optionGrid = new TreeGrid<>(CustomizationOptionTreeItem.class);
    private final TreeData<CustomizationOptionTreeItem> treeData = new TreeData<>();
    private TreeDataProvider<CustomizationOptionTreeItem> treeDataProvider;

    /** Edited option names keyed by tree-item id. */
    @Getter
    private final Map<String, String> optionNames = new HashMap<>();
    /** Edited per-tier prices keyed by tree-item id. */
    @Getter
    private final Map<String, Double> tierPrices = new HashMap<>();
    /** Captured on attach; used to marshal UI updates onto the UI thread. */
    @Getter
    private UI ui;

    private final RestClientMenuService restClientMenuService;
    private final TabManager tabManager;
    private final Tab currentTab;
    private final Integer brandId;
    private final CustomizationDto editCustomization;

    private transient List<TierDto> tierDtos = new ArrayList<>();
    private boolean initialized;
    private boolean editDataPopulated;

    /**
     * Creates the customization form bound to the given services and tab context.
     *
     * @param restClientMenuService REST client for tier/brand lookups and persistence
     * @param tabManager            manages tab add/close inside the hosting tab sheet
     * @param currentTab            the tab this form is shown in
     * @param brandId               brand owning the customization, or {@code null} to resolve one
     * @param editCustomization     the customization being edited, or {@code null} for a new one
     */
    public CustomizationForm(RestClientMenuService restClientMenuService, TabManager tabManager, Tab currentTab,
                             Integer brandId, CustomizationDto editCustomization) {
        this.restClientMenuService = restClientMenuService;
        this.tabManager = tabManager;
        this.currentTab = currentTab;
        this.brandId = brandId;
        this.editCustomization = editCustomization;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        if (initialized) {
            return;
        }
        initialized = true;
        configureBinder();
        configureForm();
        configureOptionGrid();
        configureButtons();
        if (editCustomization != null) {
            customizationBinder.readBean(editCustomization);
        }
        loadPriceTiers();
    }

    private void configureBinder() {
        customizationBinder.forField(nameField)
                .asRequired("Customization name is required")
                .bind(CustomizationDto::getName, CustomizationDto::setName);

        customizationBinder.forField(selectionType)
                .asRequired("Selection type is required")
                .bind(CustomizationDto::getSelectionType, CustomizationDto::setSelectionType);

        customizationBinder.forField(descriptionField)
                .bind(CustomizationDto::getDescription, CustomizationDto::setDescription);

        customizationBinder.forField(requiredField)
                .bind(CustomizationDto::getRequired, CustomizationDto::setRequired);

        customizationBinder.forField(minSelectionField)
                .bind(CustomizationDto::getMinimumSelection, CustomizationDto::setMinimumSelection);

        customizationBinder.forField(maxSelectionField)
                .withValidator(maximum -> minSelectionField.getValue() == null
                                || maximum == null
                                || maximum >= minSelectionField.getValue(),
                        "Maximum selection must be equal to or greater than minimum")
                .bind(CustomizationDto::getMaximumSelection, CustomizationDto::setMaximumSelection);
    }

    private void configureForm() {
        setSizeFull();

        nameField.setPlaceholder("Customization name");
        nameField.setClearButtonVisible(true);

        descriptionField.setWidthFull();
        descriptionField.setPlaceholder("Additional customization description");

        selectionType.setItems(SelectionType.values());
        selectionType.setItemLabelGenerator(SelectionType::getLabel);

        minSelectionField.setMin(0);
        minSelectionField.setValue(0);
        maxSelectionField.setMin(0);
        maxSelectionField.setValue(0);

        FormLayout formLayout = new FormLayout();
        formLayout.add(nameField, descriptionField);
        formLayout.add(selectionType, requiredField);
        formLayout.add(minSelectionField, maxSelectionField);
        formLayout.setColspan(descriptionField, 2);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2)
        );

        Span optionsTitle = new Span("OPTIONS");
        optionsTitle.getStyle().set("font-weight", "600");

        deleteOptionButton.setEnabled(false);
        deleteOptionButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_ICON,
                ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        deleteOptionButton.setTooltipText("Delete selected option");

        HorizontalLayout optionsHeader = new HorizontalLayout(optionsTitle, deleteOptionButton, addOptionButton);
        optionsHeader.setWidthFull();
        optionsHeader.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        optionsHeader.setAlignItems(FlexComponent.Alignment.CENTER);

        add(
                formLayout,
                optionsHeader,
                optionGrid,
                createButtonsLayout()
        );

        addOptionButton.addClickListener(e -> {
            if (!canAddNewRow()) {
                showNotification("Please fill in the previous option before adding another.", NotificationVariant.LUMO_ERROR);
                return;
            }
            addOptionRow(CustomizationOptionDto.builder().status(ACTIVE).build());
        });

        deleteOptionButton.addClickListener(e -> onDeleteOption(selectedRootOption()));
        optionGrid.addSelectionListener(event -> deleteOptionButton.setEnabled(!event.getAllSelectedItems().isEmpty()));

        setFlexGrow(1, optionGrid);
    }

    private boolean canAddNewRow() {
        if (treeData.getRootItems().isEmpty()) {
            return true;
        }
        CustomizationOptionTreeItem last = treeData.getRootItems().getLast();
        String name = optionNames.getOrDefault(last.getId(), last.getName());
        return name != null && !name.trim().isEmpty();
    }

    private void configureOptionGrid() {
        optionGrid.setWidthFull();
        optionGrid.setHeight("320px");
        optionGrid.removeAllColumns();

        optionGrid.addComponentHierarchyColumn(this::applyNameField)
                .setHeader("Name")
                .setAutoWidth(true);

        optionGrid.addColumn(CustomizationOptionTreeItem::getTierName)
                .setHeader("Tier")
                .setAutoWidth(true);

        optionGrid.addComponentColumn(this::applyPriceNumberField)
                .setHeader("Price")
                .setAutoWidth(true);

        optionGrid.addComponentColumn(this::applyActiveBox)
                .setHeader("Active")
                .setAutoWidth(true);

        optionGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        treeDataProvider = new TreeDataProvider<>(treeData);
        optionGrid.setDataProvider(treeDataProvider);
    }

    private Component applyNameField(CustomizationOptionTreeItem item) {
        if (!TreeLevel.ROOT.equals(item.getTreeLevel())) {
            return new Span("");
        }
        TextField field = new TextField();
        field.setValue(Optional.ofNullable(optionNames.get(item.getId())).orElse(item.getName()));
        field.setPlaceholder("Option name");
        field.addValueChangeListener(changeEvent ->
                optionNames.put(item.getId(),
                        changeEvent.getValue() == null ? "" : changeEvent.getValue().trim()));
        return field;
    }

    private Component applyPriceNumberField(CustomizationOptionTreeItem item) {
        NumberField numberField = new NumberField();
        numberField.setValue(Optional.ofNullable(tierPrices.get(item.getId()))
                .orElse(item.getPrice() != null ? item.getPrice() : 0.0));
        numberField.setMin(0);
        numberField.setStep(0.01);
        numberField.addValueChangeListener(changeEvent ->
                tierPrices.put(item.getId(), changeEvent.getValue() == null ? 0.0 : changeEvent.getValue()));
        tierPrices.putIfAbsent(item.getId(), numberField.getValue());
        return numberField;
    }

    private Component applyActiveBox(CustomizationOptionTreeItem item) {
        boolean root = TreeLevel.ROOT.equals(item.getTreeLevel());
        Checkbox checkbox = new Checkbox();
        checkbox.setValue(isActive(parentOf(item)));
        if (!root) {
            checkbox.setEnabled(false);
        } else {
            checkbox.addValueChangeListener(changeEvent ->
                    item.setStatus(changeEvent.getValue() ? ACTIVE : INACTIVE));
        }
        return checkbox;
    }

    private boolean isActive(CustomizationOptionTreeItem item) {
        return item == null || !INACTIVE.equalsIgnoreCase(item.getStatus());
    }

    private CustomizationOptionTreeItem parentOf(CustomizationOptionTreeItem item) {
        if (item == null || TreeLevel.ROOT.equals(item.getTreeLevel())) {
            return item;
        }
        for (CustomizationOptionTreeItem root : treeData.getRootItems()) {
            if (treeData.getChildren(root).contains(item)) {
                return root;
            }
        }
        return item;
    }

    private CustomizationOptionTreeItem selectedRootOption() {
        return optionGrid.getSelectedItems().stream()
                .map(this::parentOf)
                .findFirst()
                .orElse(null);
    }

    private void onDeleteOption(CustomizationOptionTreeItem root) {
        if (root == null) {
            return;
        }
        treeData.getChildren(root).forEach(child -> tierPrices.remove(child.getId()));
        treeData.removeItem(root);
        optionNames.remove(root.getId());
        tierPrices.remove(root.getId());
        optionGrid.deselectAll();
        deleteOptionButton.setEnabled(false);
        treeDataProvider.refreshAll();
        expandAllRoots();
    }

    private void loadPriceTiers() {
        if (brandId != null) {
            fetchPriceTiers(brandId);
            return;
        }
        restClientMenuService.getAllBrand()
                .subscribe(this::acceptBrandsForTiers, this::handleTierLoadFailure);
    }

    private void acceptBrandsForTiers(RestAPIResponse response) {
        if (ui == null) {
            return;
        }
        UiUtil.safeAccess(ui, () -> {
            if (ObjectUtils.isEmpty(response.getData())) {
                ensureEditData();
                return;
            }
            List<BrandDto> brandDtos = ObjectUtil.convertObjectToObject(response.getData(), new TypeReference<>() {
            });
            if (brandDtos.isEmpty()) {
                ensureEditData();
                return;
            }
            fetchPriceTiers(brandDtos.getFirst().getId());
        });
    }

    private void fetchPriceTiers(Integer resolvedBrandId) {
        if (resolvedBrandId == null) {
            handleTierLoadFailure(null);
            return;
        }
        restClientMenuService.getAllTierByBrand(resolvedBrandId, TierTypeDto.PRICE.toString())
                .subscribe(this::acceptTiers, this::handleTierLoadFailure);
    }

    private void handleTierLoadFailure(Throwable error) {
        log.error("Failed to load price tiers", error);
        if (ui != null) {
            UiUtil.safeAccess(ui, this::ensureEditData);
        }
    }

    private void acceptTiers(RestAPIResponse response) {
        if (ui == null) {
            return;
        }
        UiUtil.safeAccess(ui, () -> {
            if (!ObjectUtils.isEmpty(response.getData())) {
                tierDtos = ObjectUtil.convertObjectToObject(response.getData(), new TypeReference<>() {
                });
            }
            ensureEditData();
            backfillTierChildren();
            treeDataProvider.refreshAll();
            expandAllRoots();
        });
    }

    private void ensureEditData() {
        if (editCustomization != null && !editDataPopulated) {
            editDataPopulated = true;
            populateEditCustomization();
        }
    }

    private void backfillTierChildren() {
        if (tierDtos.isEmpty()) {
            return;
        }
        for (CustomizationOptionTreeItem root : new ArrayList<>(treeData.getRootItems())) {
            CustomizationOptionDto source = sourceOption(root);

            if (root.getTierId() == null) {
                TierDto firstTier = tierDtos.getFirst();
                Double price = source != null ? getPriceByTier(source, firstTier.getId()) : 0.0;
                root.setTierId(firstTier.getId());
                root.setTierName(firstTier.getName());
                root.setPrice(price);
                tierPrices.put(root.getId(), price);
            }

            Set<Integer> presentTiers = new HashSet<>();
            if (root.getTierId() != null) {
                presentTiers.add(root.getTierId());
            }
            treeData.getChildren(root).forEach(child -> presentTiers.add(child.getTierId()));

            AtomicInteger i = new AtomicInteger();
            tierDtos.forEach(tier -> {
                if (presentTiers.contains(tier.getId())) {
                    return;
                }
                Double price = source != null ? getPriceByTier(source, tier.getId()) : 0.0;
                CustomizationOptionTreeItem child = CustomizationOptionTreeItem.builder()
                        .id(root.getId().concat("-tier-").concat(String.valueOf(i.incrementAndGet())))
                        .optionId(root.getOptionId())
                        .tierId(tier.getId())
                        .tierName(tier.getName())
                        .price(price)
                        .treeLevel(TreeLevel.PARENT)
                        .build();
                treeData.addItems(root, child);
                tierPrices.putIfAbsent(child.getId(), price);
            });
        }
    }

    private CustomizationOptionDto sourceOption(CustomizationOptionTreeItem root) {
        if (editCustomization == null || ObjectUtils.isEmpty(editCustomization.getCustomizationOptions())
                || root.getOptionId() == null) {
            return null;
        }
        return editCustomization.getCustomizationOptions().stream()
                .filter(option -> root.getOptionId().equals(option.getId()))
                .findFirst()
                .orElse(null);
    }

    private void addOptionRow(CustomizationOptionDto option) {
        TierDto firstTier = tierDtos.isEmpty() ? null : tierDtos.getFirst();
        String rootId = UUID.randomUUID().toString();

        CustomizationOptionTreeItem root = CustomizationOptionTreeItem.builder()
                .id(rootId)
                .optionId(option != null ? option.getId() : null)
                .name(option != null && option.getName() != null ? option.getName() : "")
                .status(option != null && option.getStatus() != null ? option.getStatus() : ACTIVE)
                .tierId(firstTier != null ? firstTier.getId() : null)
                .tierName(firstTier != null ? firstTier.getName() : "")
                .price(firstTier != null ? getPriceByTier(option, firstTier.getId()) : 0.0)
                .treeLevel(TreeLevel.ROOT)
                .build();

        optionNames.put(rootId, root.getName());
        treeData.addItem(null, root);

        if (firstTier != null) {
            AtomicInteger i = new AtomicInteger();
            tierDtos.stream().skip(1).forEach(tier -> {
                CustomizationOptionTreeItem child = CustomizationOptionTreeItem.builder()
                        .id(rootId.concat("-").concat(String.valueOf(i.getAndIncrement())))
                        .optionId(root.getOptionId())
                        .tierId(tier.getId())
                        .tierName(tier.getName())
                        .price(getPriceByTier(option, tier.getId()))
                        .treeLevel(TreeLevel.PARENT)
                        .build();
                treeData.addItems(root, child);
                tierPrices.putIfAbsent(child.getId(), child.getPrice());
            });
        }
        tierPrices.putIfAbsent(root.getId(), root.getPrice());
        treeDataProvider.refreshAll();
        expandAllRoots();
    }

    private void expandAllRoots() {
        treeData.getRootItems().forEach(optionGrid::expand);
    }

    private Double getPriceByTier(CustomizationOptionDto option, Integer tierId) {
        if (option == null || option.getTierPrices() == null) {
            return 0.0;
        }
        return option.getTierPrices().stream()
                .filter(tierPrice -> tierId.equals(tierPrice.getTierId()))
                .map(CustomizationOptionTierPriceDto::getPrice)
                .findFirst()
                .orElse(0.0);
    }

    private void populateEditCustomization() {
        treeData.clear();
        optionNames.clear();
        tierPrices.clear();
        List<CustomizationOptionDto> options = editCustomization.getCustomizationOptions();
        if (!ObjectUtils.isEmpty(options)) {
            options.forEach(this::addOptionRow);
        }
        treeDataProvider.refreshAll();
        expandAllRoots();
    }

    /**
     * Assembles the current option grid into a list of option DTOs with their
     * tier prices, ready for persistence.
     *
     * @return the collected options, or {@code null} if any root option has no name
     */
    public List<CustomizationOptionDto> buildOptions() {
        List<CustomizationOptionDto> options = new ArrayList<>();
        if (treeData.getRootItems().isEmpty()) {
            return options;
        }
        for (CustomizationOptionTreeItem root : new ArrayList<>(treeData.getRootItems())) {
            String name = optionNames.getOrDefault(root.getId(), root.getName());
            if (name == null || name.trim().isEmpty()) {
                return null;
            }

            CustomizationOptionDto option = CustomizationOptionDto.builder()
                    .id(root.getOptionId())
                    .name(name)
                    .status(root.getStatus() != null ? root.getStatus() : ACTIVE)
                    .build();

            List<CustomizationOptionTierPriceDto> tierPriceDtos = new ArrayList<>();
            if (root.getTierId() != null) {
                tierPriceDtos.add(CustomizationOptionTierPriceDto.builder()
                        .tierId(root.getTierId())
                        .price(getTreeItemPrice(root))
                        .build());
            }
            treeData.getChildren(root).forEach(child ->
                    tierPriceDtos.add(CustomizationOptionTierPriceDto.builder()
                            .tierId(child.getTierId())
                            .price(getTreeItemPrice(child))
                            .build()));
            option.setTierPrices(tierPriceDtos);
            options.add(option);
        }
        return options;
    }

    private Double getTreeItemPrice(CustomizationOptionTreeItem item) {
        Double price = tierPrices.get(item.getId());
        return price != null ? price : (item.getPrice() != null ? item.getPrice() : 0.0);
    }

    /**
     * Returns the id of the customization under edit.
     *
     * @return the customization id, or {@code null} when creating a new one
     */
    public Integer getCustomizationId() {
        return editCustomization == null ? null : editCustomization.getId();
    }

    private void configureButtons() {
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        saveButton.addClickShortcut(Key.ENTER);
        updateButton.addClickShortcut(Key.ENTER);
        closeButton.addClickShortcut(Key.ESCAPE);
        saveButton.addClickListener(
                new CustomizationSaveEventListener(this, restClientMenuService)
        );
        updateButton.addClickListener(
                new CustomizationUpdateEventListener(this, restClientMenuService)
        );
        closeButton.addClickListener(event -> closeTab());
    }

    private HorizontalLayout createButtonsLayout() {
        HorizontalLayout horizontalLayout = new HorizontalLayout(closeButton, saveButton, updateButton);
        horizontalLayout.setPadding(true);
        return horizontalLayout;
    }

    private void closeTab() {
        if (tabManager != null && currentTab != null) {
            tabManager.closeAndSelectFirst(currentTab);
        }
    }

    /**
     * Invoked by the save listener after a successful create: shows a success
     * notification and closes the tab.
     */
    public void onSaveSuccess() {
        if (ui == null) {
            return;
        }
        UiUtil.safeAccess(ui, () -> {
            UiUtil.success("Customization saved successfully");
            closeTab();
        });
    }

    /**
     * Invoked by the update listener after a successful update: shows a success
     * notification and closes the tab.
     */
    public void onUpdateSuccess() {
        if (ui == null) {
            return;
        }
        UiUtil.safeAccess(ui, () -> {
            UiUtil.success("Customization updated successfully");
            closeTab();
        });
    }

    /**
     * Invoked by the save/update listeners on failure: shows an error notification.
     *
     * @param error the persistence failure
     */
    public void onSaveError(Throwable error) {
        if (ui == null) {
            return;
        }
        UiUtil.safeAccess(ui, () -> UiUtil.error("Unable to save customization"));
    }

    /**
     * Shows a transient notification with the given variant on the UI thread.
     *
     * @param text    the message to display
     * @param variant the notification styling variant
     */
    public void showNotification(String text, NotificationVariant variant) {
        if (ui == null) {
            return;
        }
        UiUtil.safeAccess(ui, () -> UiUtil.show(text, variant, 3000));
    }

    /**
     * Validates all bound fields via the binder.
     *
     * @return {@code true} when the form is valid
     */
    public boolean validate() {
        return customizationBinder.validate().isOk();
    }

    /**
     * Writes the current field values into a fresh {@link CustomizationDto}.
     *
     * @return the populated DTO, filled only when the fields are valid
     */
    public CustomizationDto getValue() {
        CustomizationDto dto = CustomizationDto.builder().build();
        customizationBinder.writeBeanIfValid(dto);
        return dto;
    }

    /**
     * Toggles the Save / Update buttons for the given action; Cancel is always visible.
     *
     * @param formAction the action driving which buttons are shown
     */
    public void restructureButton(FormAction formAction) {
        if (Objects.requireNonNull(formAction) == FormAction.CREATE) {
            saveButton.setVisible(true);
            updateButton.setVisible(false);
        } else if (formAction == FormAction.EDIT) {
            saveButton.setVisible(false);
            updateButton.setVisible(true);
        }
        closeButton.setVisible(true);
    }
}