package com.harmoni.menu.dashboard.layout.menu.customization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.CustomizationDto;
import com.harmoni.menu.dashboard.dto.CustomizationOptionDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.dto.TierTypeDto;
import com.harmoni.menu.dashboard.event.customization.CustomizationDeleteEventListener;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.exception.BusinessBadRequestException;
import com.harmoni.menu.dashboard.layout.enums.CustomizationItemType;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.LoadingBar;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.treegrid.ExpandEvent;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.Registration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

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
public class CustomizationListView extends VerticalLayout implements BroadcastMessageService {

    static final String TAB_LABEL_LIST = "All Customizations";
    static final String TAB_LABEL_NEW = "New Customization";
    static final String TAB_LABEL_EDIT = "Edit Customization";

    private static final int PAGE_SIZE = 15;
    private static final long SEARCH_TIMEOUT_MS = 400;

    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final RestClientMenuService restClientMenuService;
    private final AccessService accessService;

    private final TreeGrid<CustomizationTreeItem> customizationGrid = new TreeGrid<>(CustomizationTreeItem.class);
    private final TreeData<CustomizationTreeItem> treeData = new TreeData<>();
    private final TextField filterText = new TextField();
    private final ComboBox<BrandDto> brandDtoComboBox = new ComboBox<>();
    private final ComboBox<TierDto> tierDtoComboBox = new ComboBox<>();
    private final Text pageInfoText = new Text("");
    private final LoadingBar loadingBar = new LoadingBar();
    private final Button previousButton = new Button("Previous");
    private final Button nextButton = new Button("Next");
    private final AtomicInteger requestGeneration = new AtomicInteger();

    private transient List<BrandDto> brandDtos = new ArrayList<>();
    private transient List<TierDto> tierDtos = new ArrayList<>();
    private transient TierDto tempTier;
    private final AtomicInteger tierRequestGeneration = new AtomicInteger();
    private transient int pendingTierGeneration;
    private TreeDataProvider<CustomizationTreeItem> treeDataProvider;
    private final Map<Integer, String> tierIdToName = new HashMap<>();
    private Registration broadcasterRegistration;
    private transient UI ui;
    private int totalPages;
    private int currentPage = 1;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        ui = attachEvent.getUI();
        if (broadcasterRegistration == null) {
            broadcasterRegistration = Broadcaster.register(this::acceptNotification);
        }
        buildLayout();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (broadcasterRegistration != null) {
            broadcasterRegistration.remove();
            broadcasterRegistration = null;
        }
    }

    private void buildLayout() {
        setSizeFull();
        setPadding(false);

        configureGrid();
        configureSearch();
        configureBrandSelector();
        configurePagination();

        VerticalLayout browsePanel = new VerticalLayout(loadingBar, getContent(), getPaginationFooter());
        browsePanel.setSizeFull();
        browsePanel.setPadding(false);
        browsePanel.setSpacing(false);

        add(browsePanel);
        setFlexGrow(1, browsePanel);

        fetchBrands();
    }

    private void configureGrid() {
        customizationGrid.setSizeFull();
        customizationGrid.setEmptyStateText(UiUtil.NO_RECORDS);
        customizationGrid.removeAllColumns();
        customizationGrid.addComponentHierarchyColumn(this::applyNameLabel).setHeader("Name").setAutoWidth(true);
        customizationGrid.addColumn(item -> item.getSelectionTypeLabel() == null ? "-" : item.getSelectionTypeLabel())
                .setHeader("Type").setAutoWidth(true);
        customizationGrid.addColumn(item -> item.getTierName() == null ? "-" : item.getTierName())
                .setHeader("Tier").setAutoWidth(true);
        customizationGrid.addColumn(item -> item.getPrice() == null ? "-"
                        : String.format("%,.2f", item.getPrice())).setHeader("Price").setAutoWidth(true);
        customizationGrid.addColumn(item -> item.getOptionStatus() == null ? "-" : item.getOptionStatus())
                .setHeader("Active").setAutoWidth(true);
        customizationGrid.addComponentColumn(this::applyActionButtons).setHeader("Actions").setAutoWidth(true);
        customizationGrid.getColumns().forEach(column -> column.setResizable(true));
        customizationGrid.addExpandListener(this::onComponentEventExpandListener);
        treeDataProvider = new TreeDataProvider<>(treeData);
        customizationGrid.setDataProvider(treeDataProvider);
    }

    private Span applyNameLabel(CustomizationTreeItem item) {
        Span label = new Span(item.getName());
        if (CustomizationItemType.CUSTOMIZATION.equals(item.getType())) {
            label.getElement().getStyle().set("font-weight", "bold");
        }
        return label;
    }

    private HorizontalLayout applyActionButtons(CustomizationTreeItem item) {
        if (!CustomizationItemType.CUSTOMIZATION.equals(item.getType())) {
            return new HorizontalLayout(new Span());
        }
        Button editButton = UiUtil.editButton("Edit", event -> onEditCustomization(item));
        Button deleteButton = UiUtil.deleteButton(
                event -> new CustomizationDeleteEventListener(item.getCustomizationDto(), restClientMenuService).onComponentEvent(event));
        HorizontalLayout actions = new HorizontalLayout(editButton, deleteButton);
        actions.setSpacing(false);
        return actions;
    }

    private void configureSearch() {
        filterText.setLabel("Search");
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setPrefixComponent(VaadinIcon.SEARCH.create());
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(change -> {
            if (change.isFromClient()) {
                currentPage = 1;
                fetchCustomizations();
            }
        });
    }

    private void configureBrandSelector() {
        brandDtoComboBox.setLabel("Brand");
        brandDtoComboBox.setPlaceholder("Select brand");
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

    private void configurePagination() {
        previousButton.addClickListener(event -> {
            if (currentPage > 1) {
                currentPage--;
                fetchCustomizations();
            }
        });
        nextButton.addClickListener(event -> {
            if (currentPage < totalPages) {
                currentPage++;
                fetchCustomizations();
            }
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
        Button addButton = UiUtil.addButton("New Customization", this::onAddCustomizationListener);
        HorizontalLayout toolbar = new HorizontalLayout(brandDtoComboBox, tierDtoComboBox, filterText, addButton);
        toolbar.addClassName("toolbar");
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.BASELINE);
        return toolbar;
    }

    private void configureTierSelector() {
        tierDtoComboBox.setLabel("Tier");
        tierDtoComboBox.setPlaceholder("Select tier");
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
            tempTier.setName("All");
        }
        return tempTier;
    }

    private Integer selectedTierId() {
        TierDto tier = tierDtoComboBox.getValue();
        return tier == null || tier.getId() == null || tier.getId() == -1 ? null : tier.getId();
    }

    private VerticalLayout getContent() {
        VerticalLayout content = new VerticalLayout(customizationGrid);
        content.setSizeFull();
        content.addClassNames("content");
        content.setFlexGrow(1, customizationGrid);
        return content;
    }

    private HorizontalLayout getPaginationFooter() {
        HorizontalLayout footer = new HorizontalLayout(previousButton, pageInfoText, nextButton);
        footer.addClassName("pagination");
        footer.setWidthFull();
        footer.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        return footer;
    }

    private void updatePaginationState() {
        pageInfoText.setText("Page " + currentPage + " of " + Math.max(totalPages, 1));
        previousButton.setEnabled(currentPage > 1);
        nextButton.setEnabled(currentPage < totalPages);
    }

    private void onAddCustomizationListener(ClickEvent<Button> event) {
        if (!(this.getParent().orElseThrow() instanceof TabSheet tabSheet)) {
            return;
        }
        TabManager tabManager = new TabManager(tabSheet);
        tabManager.addOrSelect(TAB_LABEL_NEW, tab -> {
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
                    ui.access(() -> {
                        TabManager tabManager = new TabManager(tabSheet);
                        String tabLabel = customization.getName() == null || customization.getName().isBlank()
                                ? TAB_LABEL_EDIT : "Edit ".concat(customization.getName());
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
                        ui.access(() -> {
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
        ui.access(() -> {
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
        ui.access(() -> {
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
        ui.access(() -> {
            BrandDto brand = brandDtoComboBox.getValue();
            if (brand == null) {
                treeData.clear();
                treeDataProvider.refreshAll();
                totalPages = 0;
                updatePaginationState();
                return;
            }
            int generation = requestGeneration.incrementAndGet();
            loadingBar.start();
            asyncRestClientMenuService.getAllCustomizationAsync(
                    result -> ui.access(() -> {
                        if (generation != requestGeneration.get()) {
                            return;
                        }
                        loadingBar.stop();
                        applyCustomizations(result);
                    }),
                    error -> ui.access(() -> {
                        if (generation != requestGeneration.get()) {
                            return;
                        }
                        loadingBar.stop();
                        handleLoadError(error);
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
        updatePaginationState();
    }

    private void handleLoadError(Throwable error) {
        log.error("Failed to load customizations", error);
        if (!(error instanceof BusinessBadRequestException)) {
            UiUtil.error("Unable to load customizations");
        }
    }

    private String normalizeSearch(String value) {
        return value == null ? "" : value.trim();
    }

    private void acceptNotification(String message) {
        try {
            BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
            if (broadcastMessage != null
                    && (BroadcastMessage.CUSTOMIZATION_INSERT_SUCCESS.equals(broadcastMessage.getType())
                    || BroadcastMessage.CUSTOMIZATION_UPDATED_SUCCESS.equals(broadcastMessage.getType())
                    || BroadcastMessage.CUSTOMIZATION_DELETE_SUCCESS.equals(broadcastMessage.getType()))) {
                fetchCustomizations();
            }
        } catch (JsonProcessingException e) {
            log.error("Broadcast handler error", e);
        }
    }
}
