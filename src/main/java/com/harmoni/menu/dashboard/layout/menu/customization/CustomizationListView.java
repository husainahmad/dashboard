package com.harmoni.menu.dashboard.layout.menu.customization;

import com.fasterxml.jackson.core.type.TypeReference;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.layout.AbstractListView;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.CustomizationDto;
import com.harmoni.menu.dashboard.dto.CustomizationOptionDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.dto.TierTypeDto;
import com.harmoni.menu.dashboard.event.customization.CustomizationDeleteEventListener;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.layout.enums.CustomizationItemType;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.GridSkeleton;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.treegrid.ExpandEvent;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.data.value.ValueChangeMode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import com.harmoni.menu.dashboard.layout.util.Css;

/**
 * The "All Customizations" tree view inside {@link CustomizationTabs}.
 *
 * <p>
 * Displays customizations as a lazy-loaded, paginated {@link TreeGrid} whose
 * rows expand into options and per-tier prices, driven by a brand and tier
 * selector plus a name filter. Adding or editing a customization opens a
 * {@link CustomizationForm} in a new tab via {@link TabManager}; deletion is
 * delegated to {@link CustomizationDeleteEventListener}. The grid refreshes
 * itself on insert, update and delete broadcasts.
 * </p>
 */
@RequiredArgsConstructor
@Slf4j
public class CustomizationListView extends AbstractListView implements BroadcastMessageService {

    private static final int PAGE_SIZE = 15;
    private static final long SEARCH_TIMEOUT_MS = 400;

    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final RestClientMenuService restClientMenuService;
    private final AccessService accessService;

    private final TreeGrid<CustomizationTreeItem> customizationGrid = new TreeGrid<>(CustomizationTreeItem.class);
    private final TreeData<CustomizationTreeItem> treeData = new TreeData<>();
    private final ComboBox<BrandDto> brandDtoComboBox = new ComboBox<>();
    private final ComboBox<TierDto> tierDtoComboBox = new ComboBox<>();
    private final GridSkeleton gridSkeleton = new GridSkeleton(PAGE_SIZE);
    private final AtomicInteger requestGeneration = new AtomicInteger();

    private transient List<BrandDto> brandDtos = new ArrayList<>();
    private transient List<TierDto> tierDtos = new ArrayList<>();
    private transient TierDto tempTier;
    private final AtomicInteger tierRequestGeneration = new AtomicInteger();
    private transient int pendingTierGeneration;
    private TreeDataProvider<CustomizationTreeItem> treeDataProvider;
    private final Map<Integer, String> tierIdToName = new HashMap<>();

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        refreshOnBroadcast(Set.of(BroadcastMessage.CUSTOMIZATION_INSERT_SUCCESS,
                BroadcastMessage.CUSTOMIZATION_UPDATED_SUCCESS,
                BroadcastMessage.CUSTOMIZATION_DELETE_SUCCESS), this::fetchCustomizations);
        buildLayout();
    }

    private void buildLayout() {
        setSizeFull();
        setPadding(false);

        configureGrid();
        configureSearch();
        configureBrandSelector();

        VerticalLayout browsePanel = new VerticalLayout(getContent(), getPaginationFooter());
        browsePanel.setSizeFull();
        browsePanel.setPadding(false);
        browsePanel.setSpacing(false);

        add(browsePanel);
        setFlexGrow(1, browsePanel);

        fetchBrands();
    }

    private void configureGrid() {
        customizationGrid.setSizeFull();
        customizationGrid.setEmptyStateText(Messages.get("grid.empty.customizationList"));
        customizationGrid.removeAllColumns();
        customizationGrid.addComponentHierarchyColumn(this::applyNameLabel).setHeader(Messages.get(Messages.Keys.GRID_HEADER_NAME)).setAutoWidth(true);
        customizationGrid.addColumn(item -> item.getSelectionTypeLabel() == null ? "-" : item.getSelectionTypeLabel())
                .setHeader(Messages.get(Messages.Keys.GRID_HEADER_TYPE)).setAutoWidth(true);
        customizationGrid.addColumn(item -> item.getTierName() == null ? "-" : item.getTierName())
                .setHeader(Messages.get("grid.header.tier")).setAutoWidth(true);
        customizationGrid.addColumn(item -> item.getPrice() == null ? "-"
                        : String.format("%,.2f", item.getPrice())).setHeader(Messages.get(Messages.Keys.GRID_HEADER_PRICE)).setAutoWidth(true);
        customizationGrid.addColumn(item -> item.getOptionStatus() == null ? "-" : item.getOptionStatus())
                .setHeader(Messages.get(Messages.Keys.GRID_HEADER_ACTIVE)).setAutoWidth(true);
        customizationGrid.addComponentColumn(this::applyActionButtons).setHeader(Messages.get("grid.header.actions")).setAutoWidth(true);
        customizationGrid.getColumns().forEach(column -> column.setResizable(true));
        customizationGrid.addExpandListener(this::onComponentEventExpandListener);
        treeDataProvider = new TreeDataProvider<>(treeData);
        customizationGrid.setDataProvider(treeDataProvider);
    }

    private Span applyNameLabel(CustomizationTreeItem item) {
        Span label = new Span(item.getName());
        if (CustomizationItemType.CUSTOMIZATION.equals(item.getType())) {
            label.getElement().getStyle().set(Css.FONT_WEIGHT, "bold");
        }
        return label;
    }

    private HorizontalLayout applyActionButtons(CustomizationTreeItem item) {
        if (!CustomizationItemType.CUSTOMIZATION.equals(item.getType())) {
            return new HorizontalLayout(new Span());
        }
        Button editButton = UiUtil.editButton(Messages.get(Messages.Keys.ACTION_EDIT), event -> onEditCustomization(item));
        Button deleteButton = UiUtil.deleteButton(
                event -> new CustomizationDeleteEventListener(item.getCustomizationDto(), restClientMenuService).onComponentEvent(event));
        HorizontalLayout actions = new HorizontalLayout(editButton, deleteButton);
        actions.setSpacing(false);
        return actions;
    }

    private void configureSearch() {
        filterText.setLabel(Messages.get(Messages.Keys.LABEL_SEARCH));
        configureSearchFilter();
        filterText.addValueChangeListener(change -> {
            if (change.isFromClient()) {
                currentPage = 1;
                fetchCustomizations();
            }
        });
    }

    private void configureBrandSelector() {
        brandDtoComboBox.setLabel(Messages.get(Messages.Keys.LABEL_BRAND));
        brandDtoComboBox.setPlaceholder(Messages.get("placeholder.selectBrand"));
        brandDtoComboBox.setClearButtonVisible(true);
        brandDtoComboBox.setItemLabelGenerator(BrandDto::getName);
        brandDtoComboBox.setItems(Collections.emptyList());
        brandDtoComboBox.addValueChangeListener(change -> {
            if (!change.isFromClient()) {
                return;
            }
            if (change.getValue() == null) {
                if (!brandDtos.isEmpty()) {
                    brandDtoComboBox.setValue(brandDtos.getFirst());
                }
                return;
            }
            currentPage = 1;
            fetchTiersForBrand(change.getValue().getId());
            fetchCustomizations();
        });
    }

    /**
     * Builds the list toolbar: brand and tier selectors, the name filter and the
     * "New Customization" button.
     *
     * @return the toolbar row for this list view
     */
    public HorizontalLayout getToolbarComponent() {
        configureTierSelector();
        Button addButton = UiUtil.addButton(Messages.get("action.newCustomization"), this::onAddCustomizationListener);
        HorizontalLayout toolbar = new HorizontalLayout(brandDtoComboBox, tierDtoComboBox, filterText, addButton);
        toolbar.addClassName(Css.TOOLBAR);
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.BASELINE);
        return toolbar;
    }

    private void configureTierSelector() {
        tierDtoComboBox.setLabel(Messages.get(Messages.Keys.LABEL_TIER));
        tierDtoComboBox.setPlaceholder(Messages.get("placeholder.selectTier"));
        tierDtoComboBox.setClearButtonVisible(true);
        tierDtoComboBox.setItemLabelGenerator(TierDto::getName);
        tierDtoComboBox.setItems(Collections.emptyList());
        tierDtoComboBox.addValueChangeListener(change -> {
            if (change.isFromClient()) {
                currentPage = 1;
                fetchCustomizations();
            }
        });
    }

    private TierDto getTempTierDto() {
        if (tempTier == null) {
            tempTier = new TierDto();
            tempTier.setId(-1);
            tempTier.setName(Messages.get(Messages.Keys.LABEL_ALL));
        }
        return tempTier;
    }

    private Integer selectedTierId() {
        TierDto tier = tierDtoComboBox.getValue();
        return tier == null || tier.getId() == null || tier.getId() == -1 ? null : tier.getId();
    }

    private HorizontalLayout getContent() {
        return gridSlot(customizationGrid, gridSkeleton);
    }

    private HorizontalLayout getPaginationFooter() {
        return paginationFooter(() -> {
            if (currentPage > 1) {
                currentPage--;
                fetchCustomizations();
            }
        }, () -> {
            if (currentPage < totalPages) {
                currentPage++;
                fetchCustomizations();
            }
        });
    }

    private void onAddCustomizationListener(ClickEvent<Button> event) {
        if (!(this.getParent().orElseThrow() instanceof TabSheet tabSheet)) {
            return;
        }
        TabManager tabManager = new TabManager(tabSheet);
        tabManager.addOrSelect(Messages.get("tab.customizationNew"), tab -> {
            CustomizationForm form = new CustomizationForm(restClientMenuService, tabManager, tab,
                    currentBrandId(), null);
            form.restructureButton(FormAction.CREATE);
            return form;
        });
    }

    private void onEditCustomization(CustomizationTreeItem item) {
        if (!(this.getParent().orElseThrow() instanceof TabSheet tabSheet)) {
            return;
        }
        restClientMenuService.getCustomizationById(item.getCustomizationId())
                .subscribe(response -> {
                    if (ObjectUtils.isEmpty(response.getData())) {
                        return;
                    }
                    CustomizationDto customization = ObjectUtil.convertValueToObject(
                            response.getData(), CustomizationDto.class);
                    UiUtil.safeAccess(ui, () -> {
                        TabManager tabManager = new TabManager(tabSheet);
                        String tabLabel = customization.getName() == null || customization.getName().isBlank()
                                ? Messages.get("tab.customizationEdit") : Messages.get(Messages.Keys.ACTION_EDIT_NAME, customization.getName());
                        tabManager.addOrSelect(tabLabel, tab -> {
                            CustomizationForm form = new CustomizationForm(restClientMenuService, tabManager, tab,
                                    currentBrandId(), customization);
                            form.restructureButton(FormAction.EDIT);
                            return form;
                        });
                    });
                }, error -> log.error("Failed to load customization detail", error));
    }

    private void onComponentEventExpandListener(ExpandEvent<CustomizationTreeItem, TreeGrid<CustomizationTreeItem>> event) {
        event.getItems().forEach(item -> {
            if (!CustomizationItemType.CUSTOMIZATION.equals(item.getType())
                    || !treeData.getChildren(item).isEmpty()) {
                return;
            }
            restClientMenuService.getCustomizationById(item.getCustomizationId())
                    .subscribe(response -> {
                        if (ObjectUtils.isEmpty(response.getData()) || ui == null) {
                            return;
                        }
                        CustomizationDto customization = ObjectUtil.convertValueToObject(
                                response.getData(), CustomizationDto.class);
                        UiUtil.safeAccess(ui, () -> {
                            appendChildren(item, customization);
                            treeDataProvider.refreshAll();
                            customizationGrid.expand(item);
                        });
                    }, error -> log.error("Failed to load customization options", error));
        });
    }

    private CustomizationTreeItem toRootItem(CustomizationDto customization) {
        return CustomizationTreeItem.builder()
                .id("CUSTOMIZATION|%d".formatted(customization.getId()))
                .name(customization.getName())
                .type(CustomizationItemType.CUSTOMIZATION)
                .customizationId(customization.getId())
                .selectionTypeLabel(customization.getSelectionType() != null
                        ? customization.getSelectionType().getLabel() : null)
                .customizationDto(customization)
                .build();
    }

    private void appendChildren(CustomizationTreeItem root, CustomizationDto customization) {
        if (ObjectUtils.isEmpty(customization.getCustomizationOptions())) {
            return;
        }
        customization.getCustomizationOptions().forEach(option -> {
            CustomizationTreeItem optionItem = CustomizationTreeItem.builder()
                    .id("%s|OPTION|%d".formatted(root.getId(), option.getId()))
                    .name(option.getName())
                    .type(CustomizationItemType.OPTION)
                    .customizationId(customization.getId())
                    .optionId(option.getId())
                    .optionStatus(option.getStatus())
                    .build();
            treeData.addItem(root, optionItem);
            if (!ObjectUtils.isEmpty(option.getTierPrices())) {
                Integer filterTierId = selectedTierId();
                option.getTierPrices().stream()
                        .filter(tierPrice -> filterTierId == null || filterTierId.equals(tierPrice.getTierId()))
                        .forEach(tierPrice -> treeData.addItem(optionItem,
                                CustomizationTreeItem.builder()
                                        .id("%s|TIER|%d".formatted(optionItem.getId(), tierPrice.getTierId()))
                                        .name("")
                                        .type(CustomizationItemType.TIER_PRICE)
                                        .customizationId(customization.getId())
                                        .optionId(option.getId())
                                        .tierId(tierPrice.getTierId())
                                        .tierName(tierIdToName.getOrDefault(tierPrice.getTierId(), ""))
                                        .price(tierPrice.getPrice())
                                        .build()));
            }
        });
    }

    private Integer currentBrandId() {
        BrandDto brand = brandDtoComboBox.getValue();
        if (brand != null) {
            return brand.getId();
        }
        if (brandDtos.isEmpty()) {
            return null;
        }
        return defaultBrand().getId();
    }

    private void fetchBrands() {
        restClientMenuService.getAllBrand()
                .subscribe(this::acceptBrands, error -> log.error("Failed to load brands", error));
    }

    private void acceptBrands(RestAPIResponse response) {
        if (ObjectUtils.isEmpty(response.getData())) {
            return;
        }
        brandDtos = ObjectUtil.convertObjectToObject(response.getData(), new TypeReference<>() {
        });
        if (brandDtos.isEmpty() || ui == null) {
            return;
        }
        UiUtil.safeAccess(ui, () -> {
            BrandDto previous = brandDtoComboBox.getValue();
            brandDtoComboBox.setItems(brandDtos);
            brandDtoComboBox.setValue(previous == null
                    ? brandDtos.getFirst()
                    : brandDtos.stream()
                            .filter(brand -> Objects.equals(previous.getId(), brand.getId()))
                            .findFirst()
                            .orElse(brandDtos.getFirst()));
            fetchTiersForBrand(brandDtoComboBox.getValue().getId());
            fetchCustomizations();
        });
    }

    private void fetchTiersForBrand(Integer brandId) {
        if (brandId == null) {
            return;
        }
        pendingTierGeneration = tierRequestGeneration.incrementAndGet();
        restClientMenuService.getAllTierByBrand(brandId, TierTypeDto.PRICE.toString())
                .subscribe(this::acceptTiers, error -> log.error("Failed to load price tiers", error));
    }

    private void acceptTiers(RestAPIResponse response) {
        if (ObjectUtils.isEmpty(response.getData()) || ui == null) {
            return;
        }
        int generation = pendingTierGeneration;
        UiUtil.safeAccess(ui, () -> {
            if (generation != pendingTierGeneration) {
                return;
            }
            tierDtos = new ArrayList<>();
            tierDtos.add(getTempTierDto());
            tierDtos.addAll(ObjectUtil.convertObjectToObject(response.getData(), new TypeReference<>() {
            }));
            tierIdToName.clear();
            tierDtos.forEach(tier -> {
                if (tier.getId() != null && tier.getId() != -1) {
                    tierIdToName.put(tier.getId(), tier.getName());
                }
            });
            TierDto previous = tierDtoComboBox.getValue();
            tierDtoComboBox.setItems(tierDtos);
            tierDtoComboBox.setValue(previous != null && tierDtos.stream().anyMatch(t -> t.getId().equals(previous.getId()))
                    ? previous : getTempTierDto());
        });
    }

    private BrandDto defaultBrand() {
        try {
            Integer brandId = accessService.getUserDetail().getStoreDto().getChainDto().getBrandId();
            return brandDtos.stream()
                    .filter(brand -> brandId != null && brandId.equals(brand.getId()))
                    .findFirst()
                    .orElse(brandDtos.getFirst());
        } catch (NullPointerException e) {
            return brandDtos.getFirst();
        }
    }

    private void fetchCustomizations() {
        if (ui == null) {
            return;
        }
        UiUtil.safeAccess(ui, () -> {
            BrandDto brand = brandDtoComboBox.getValue();
            if (brand == null) {
                treeData.clear();
                treeDataProvider.refreshAll();
                totalPages = 0;
                updatePagination();
                return;
            }
            int generation = requestGeneration.incrementAndGet();
            gridSkeleton.show();
            asyncRestClientMenuService.getAllCustomizationAsync(
                    result -> UiUtil.safeAccess(ui, () -> {
                        if (generation != requestGeneration.get()) {
                            return;
                        }
                        gridSkeleton.hide();
                        applyCustomizations(result);
                    }),
                    error -> UiUtil.safeAccess(ui, () -> {
                        if (generation != requestGeneration.get()) {
                            return;
                        }
                        gridSkeleton.hide();
                        UiUtil.errorWithRetry(Messages.get(Messages.Keys.NOTIFICATION_CUSTOMIZATION_LOAD_FAILED), this::fetchCustomizations);
                    }),
                    brand.getId(), currentPage, PAGE_SIZE, normalizeSearch(filterText.getValue()));
        });
    }

    private void applyCustomizations(Map<String, Object> result) {
        Object data = result.get("data");
        treeData.clear();
        if (data instanceof List<?> list && !list.isEmpty()) {
            list.forEach(object -> {
                CustomizationDto customization = ObjectUtil.convertValueToObject(object, CustomizationDto.class);
                CustomizationTreeItem root = toRootItem(customization);
                treeData.addItem(null, root);
                if (!ObjectUtils.isEmpty(customization.getCustomizationOptions())) {
                    appendChildren(root, customization);
                }
            });
            totalPages = result.get("page") == null ? 0 : Integer.parseInt(result.get("page").toString());
        } else {
            totalPages = 0;
        }
        treeDataProvider.refreshAll();
        updatePagination();
    }

    private String normalizeSearch(String value) {
        return value == null ? "" : value.trim();
    }
}
