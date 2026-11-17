package com.harmoni.menu.dashboard.layout.menu.customization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.CustomizationDto;
import com.harmoni.menu.dashboard.dto.CustomizationOptionDto;
import com.harmoni.menu.dashboard.dto.CustomizationOptionTierPriceDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.dto.TierTypeDto;
import com.harmoni.menu.dashboard.event.customization.CustomizationPersistEventListener;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.layout.enums.SelectionType;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.organization.tier.service.TreeLevel;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
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
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import com.harmoni.menu.dashboard.layout.util.Css;

/**
 * Tab form for creating or editing a {@link CustomizationDto}.
 *
 * <p>
 * Edits the customization header (name, description, selection type, required,
 * allow-quantity, min/max selection) through a {@link Binder} and the options
 * through a {@link TreeGrid} where each root row is an option and its children
 * the per-tier prices. Price tiers are loaded for the owning brand, and a
 * non-null {@code editCustomization} pre-populates the fields and the option
 * tree.
 * </p>
 *
 * <p>
 * Save and update are handled by {@link CustomizationPersistEventListener}; on
 * success the hosting tab is closed through {@link TabManager}.
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
    @Getter private final TextField nameField = new TextField(Messages.get(Messages.Keys.LABEL_FIELD_CUSTOMIZATION_NAME));
    /** Free-text customization description input. */
    @Getter private final TextArea descriptionField = new TextArea(Messages.get(Messages.Keys.LABEL_DESCRIPTION));
    /** Single / multiple selection type combo. */
    @Getter private final ComboBox<SelectionType> selectionType = new ComboBox<>(Messages.get(Messages.Keys.LABEL_SELECTION_TYPE));
    /** Whether the customization is mandatory. */
    @Getter private final Checkbox requiredField = new Checkbox(Messages.get(Messages.Keys.LABEL_REQUIRED));
    /** Whether an option may be ordered with a quantity greater than one. */
    @Getter private final Checkbox allowQuantityField = new Checkbox(Messages.get(Messages.Keys.LABEL_ALLOW_QUANTITY));
    /** Minimum number of options the user must select. */
    @Getter private final IntegerField minSelectionField = new IntegerField(Messages.get("label.minSelection"));
    /** Maximum number of options the user may select. */
    @Getter private final IntegerField maxSelectionField = new IntegerField(Messages.get("label.maxSelection"));

    // Buttons
    private final Button addOptionButton = new Button(Messages.get("action.addOption"));
    private final Button deleteOptionButton = new Button(VaadinIcon.TRASH.create());
    private final Button saveButton = new Button(Messages.get(Messages.Keys.ACTION_SAVE));
    private final Button updateButton = new Button(Messages.get(Messages.Keys.ACTION_UPDATE));
    private final Button closeButton = new Button(Messages.get(Messages.Keys.ACTION_CANCEL));
    private final Span dirtyLabel = new Span(Messages.get("customization.unsaved"));

    // Option tree grid (mirrors the product SKU + tier price layout)
    private final TreeGrid<CustomizationOptionTreeItem> optionGrid = new TreeGrid<>(CustomizationOptionTreeItem.class);
    private final TreeData<CustomizationOptionTreeItem> treeData = new TreeData<>();
    private TreeDataProvider<CustomizationOptionTreeItem> treeDataProvider;

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
    private boolean dirty;
    private boolean loading;

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
        addClassName("customization-form");
        if (tabManager != null) {
            tabManager.setCloseGuard(currentTab, tab -> {
                if (!dirty) {
                    return true;
                }
                requestClose();
                return false;
            });
        }
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
            loading = true;
            customizationBinder.readBean(editCustomization);
            loading = false;
        }
        loadPriceTiers();
    }

    private void configureBinder() {
        customizationBinder.forField(nameField)
                .asRequired(Messages.get("validation.customization.name.required"))
                .bind(CustomizationDto::getName, CustomizationDto::setName);

        customizationBinder.forField(selectionType)
                .asRequired(Messages.get("validation.selectionType.required"))
                .bind(CustomizationDto::getSelectionType, CustomizationDto::setSelectionType);

        customizationBinder.forField(descriptionField)
                .bind(CustomizationDto::getDescription, CustomizationDto::setDescription);

        customizationBinder.forField(requiredField)
                .bind(CustomizationDto::getRequired, CustomizationDto::setRequired);

        customizationBinder.forField(allowQuantityField)
                .bind(CustomizationDto::getAllowQuantity, CustomizationDto::setAllowQuantity);

        customizationBinder.forField(minSelectionField)
                .bind(CustomizationDto::getMinimumSelection, CustomizationDto::setMinimumSelection);

        customizationBinder.forField(maxSelectionField)
                .withValidator(maximum -> minSelectionField.getValue() == null
                                || maximum == null
                                || maximum >= minSelectionField.getValue(),
                        Messages.get("validation.maxSelection"))
                .bind(CustomizationDto::getMaximumSelection, CustomizationDto::setMaximumSelection);
    }

    private void configureForm() {
        setSizeFull();

        nameField.setPlaceholder(Messages.get(Messages.Keys.LABEL_FIELD_CUSTOMIZATION_NAME));
        nameField.setClearButtonVisible(true);

        descriptionField.setWidthFull();
        descriptionField.setPlaceholder(Messages.get("placeholder.customization.description"));

        selectionType.setItems(SelectionType.values());
        selectionType.setItemLabelGenerator(SelectionType::getLabel);

        minSelectionField.setMin(0);
        minSelectionField.setValue(0);
        maxSelectionField.setMin(0);
        maxSelectionField.setValue(0);

        nameField.addValueChangeListener(event -> markDirty());
        descriptionField.addValueChangeListener(event -> markDirty());
        selectionType.addValueChangeListener(event -> markDirty());
        requiredField.addValueChangeListener(event -> markDirty());
        allowQuantityField.addValueChangeListener(event -> markDirty());
        minSelectionField.addValueChangeListener(event -> markDirty());
        maxSelectionField.addValueChangeListener(event -> markDirty());

        dirtyLabel.addClassName("customization-dirty-label");
        dirtyLabel.setVisible(false);
        saveButton.setEnabled(false);
        updateButton.setEnabled(false);

        FormLayout formLayout = new FormLayout();
        formLayout.add(nameField, descriptionField);
        formLayout.add(selectionType, requiredField);
        formLayout.add(allowQuantityField, new Span());
        formLayout.add(minSelectionField, maxSelectionField);
        formLayout.setColspan(descriptionField, 2);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep(Css.DIALOG_WIDTH, 2)
        );

        Span optionsTitle = new Span(Messages.get("label.options"));
        optionsTitle.getStyle().set(Css.FONT_WEIGHT, "600");

        deleteOptionButton.setEnabled(false);
        deleteOptionButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_ICON,
                ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
        deleteOptionButton.setTooltipText(Messages.get("tooltip.deleteOption"));

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
                showNotification(Messages.get("notification.customization.addOptionFirst"), NotificationVariant.LUMO_ERROR);
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
        String name = last.getName();
        return name != null && !name.trim().isEmpty();
    }

    private void configureOptionGrid() {
        optionGrid.setWidthFull();
        optionGrid.setHeight("320px");
        optionGrid.removeAllColumns();

        optionGrid.addComponentHierarchyColumn(this::applyNameField)
                .setHeader(Messages.get(Messages.Keys.GRID_HEADER_NAME))
                .setAutoWidth(true);

        optionGrid.addColumn(CustomizationOptionTreeItem::getTierName)
                .setHeader(Messages.get(Messages.Keys.LABEL_TIER))
                .setAutoWidth(true);

        optionGrid.addComponentColumn(this::applyPriceNumberField)
                .setHeader(Messages.get(Messages.Keys.LABEL_PRICE))
                .setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.END);

        optionGrid.addComponentColumn(this::applyActiveBox)
                .setHeader(Messages.get(Messages.Keys.GRID_HEADER_ACTIVE))
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
        field.setValue(item.getName() == null ? "" : item.getName());
        field.setPlaceholder(Messages.get("label.optionName"));
        field.addValueChangeListener(changeEvent -> {
            item.setName(changeEvent.getValue());
            markDirty();
        });
        return field;
    }

    private Component applyPriceNumberField(CustomizationOptionTreeItem item) {
        NumberField numberField = new NumberField();
        numberField.addClassName("money-field");
        numberField.setValue(item.getPrice() == null ? 0.0 : item.getPrice());
        numberField.setMin(0);
        numberField.setStep(0.01);
        numberField.addValueChangeListener(changeEvent -> {
            item.setPrice(changeEvent.getValue() == null ? 0.0 : changeEvent.getValue());
            markDirty();
        });
        return numberField;
    }

    private Component applyActiveBox(CustomizationOptionTreeItem item) {
        boolean root = TreeLevel.ROOT.equals(item.getTreeLevel());
        Checkbox checkbox = new Checkbox();
        checkbox.setValue(isActive(parentOf(item)));
        if (!root) {
            checkbox.setEnabled(false);
        } else {
            checkbox.addValueChangeListener(changeEvent -> {
                item.setStatus(changeEvent.getValue() ? ACTIVE : INACTIVE);
                markDirty();
            });
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
        treeData.removeItem(root);
        optionGrid.deselectAll();
        deleteOptionButton.setEnabled(false);
        treeDataProvider.refreshAll();
        expandAllRoots();
        markDirty();
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
            UiUtil.safeAccess(ui, () -> {
                ensureEditData();
                UiUtil.show(Messages.get("notification.tierPrice.loadFailed"),
                        NotificationVariant.LUMO_ERROR, 3000);
            });
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
            loading = true;
            populateEditCustomization();
            loading = false;
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
                String childId = root.getId().concat("-tier-").concat(String.valueOf(i.incrementAndGet()));
                treeData.addItems(root, newTierChild(root, tier, price, childId));
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

        treeData.addItem(null, root);

        if (firstTier != null) {
            AtomicInteger i = new AtomicInteger();
            tierDtos.stream().skip(1).forEach(tier -> {
                String childId = rootId.concat("-").concat(String.valueOf(i.getAndIncrement()));
                treeData.addItems(root, newTierChild(root, tier, getPriceByTier(option, tier.getId()), childId));
            });
        }
        treeDataProvider.refreshAll();
        expandAllRoots();
        markDirty();
    }

    private CustomizationOptionTreeItem newTierChild(CustomizationOptionTreeItem root, TierDto tier,
                                                     Double price, String childId) {
        return CustomizationOptionTreeItem.builder()
                .id(childId)
                .optionId(root.getOptionId())
                .tierId(tier.getId())
                .tierName(tier.getName())
                .price(price)
                .treeLevel(TreeLevel.PARENT)
                .build();
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
        List<CustomizationOptionDto> options = editCustomization.getCustomizationOptions();
        if (!ObjectUtils.isEmpty(options)) {
            options.forEach(this::addOptionRow);
        }
        treeDataProvider.refreshAll();
        expandAllRoots();
    }

    /**
     * Whether any root option is still missing a name. Callers validate this
     * before {@link #buildOptions()} so the builder itself never fails.
     *
     * @return {@code true} when a root option name is null or blank
     */
    public boolean hasBlankOptionName() {
        return treeData.getRootItems().stream()
                .anyMatch(root -> root.getName() == null || root.getName().trim().isEmpty());
    }

    /**
     * Assembles the current option grid into a list of option DTOs with their
     * tier prices, ready for persistence. Assumes
     * {@link #hasBlankOptionName()} has been checked.
     *
     * @return the collected options
     */
    public List<CustomizationOptionDto> buildOptions() {
        List<CustomizationOptionDto> options = new ArrayList<>();
        for (CustomizationOptionTreeItem root : new ArrayList<>(treeData.getRootItems())) {
            CustomizationOptionDto option = CustomizationOptionDto.builder()
                    .id(root.getOptionId())
                    .name(root.getName() == null ? "" : root.getName().trim())
                    .status(root.getStatus() != null ? root.getStatus() : ACTIVE)
                    .build();

            List<CustomizationOptionTierPriceDto> tierPriceDtos = new ArrayList<>();
            if (root.getTierId() != null) {
                tierPriceDtos.add(CustomizationOptionTierPriceDto.builder()
                        .tierId(root.getTierId())
                        .price(priceOf(root))
                        .build());
            }
            treeData.getChildren(root).forEach(child ->
                    tierPriceDtos.add(CustomizationOptionTierPriceDto.builder()
                            .tierId(child.getTierId())
                            .price(priceOf(child))
                            .build()));
            option.setTierPrices(tierPriceDtos);
            options.add(option);
        }
        return options;
    }

    private double priceOf(CustomizationOptionTreeItem item) {
        return item.getPrice() == null ? 0.0 : item.getPrice();
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
                new CustomizationPersistEventListener(this, restClientMenuService, false)
        );
        updateButton.addClickListener(
                new CustomizationPersistEventListener(this, restClientMenuService, true)
        );
        closeButton.addClickListener(event -> requestClose());
    }

    private void markDirty() {
        if (loading || dirty) {
            return;
        }
        dirty = true;
        dirtyLabel.setVisible(true);
        saveButton.setEnabled(true);
        updateButton.setEnabled(true);
    }

    private void requestClose() {
        if (!dirty) {
            closeTab();
            return;
        }
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(Messages.get("dialog.unsaved.title"));
        dialog.setText(Messages.get("dialog.unsaved.text"));
        dialog.setCancelable(true);
        dialog.setConfirmText(Messages.get("action.discard"));
        dialog.setCancelText(Messages.get(Messages.Keys.ACTION_CANCEL));
        dialog.addConfirmListener(event -> closeTab());
        dialog.open();
    }

    private HorizontalLayout createButtonsLayout() {
        HorizontalLayout horizontalLayout = new HorizontalLayout(
                dirtyLabel, closeButton, saveButton, updateButton);
        horizontalLayout.addClassName("customization-form-actions");
        horizontalLayout.setWidthFull();
        horizontalLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        horizontalLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        horizontalLayout.setFlexGrow(1, dirtyLabel);
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
            UiUtil.success(Messages.get("notification.customization.saved"));
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
            UiUtil.success(Messages.get(Messages.Keys.NOTIFICATION_CUSTOMIZATION_UPDATED));
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
        UiUtil.safeAccess(ui, () -> UiUtil.error(Messages.get("notification.customization.saveFailed")));
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
        saveButton.setEnabled(dirty);
        updateButton.setEnabled(dirty);
    }
}