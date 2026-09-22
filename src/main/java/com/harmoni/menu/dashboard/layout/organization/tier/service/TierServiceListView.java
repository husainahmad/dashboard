package com.harmoni.menu.dashboard.layout.organization.tier.service;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.*;
import com.harmoni.menu.dashboard.event.tier.TierServiceDeleteEventListener;
import com.harmoni.menu.dashboard.event.tier.TierSubServiceUpdateEventListener;
import com.harmoni.menu.dashboard.layout.AbstractListView;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.GridSkeleton;
import com.harmoni.menu.dashboard.layout.util.LoadingBar;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Vaadin tree-grid view listing service tiers. Renders a {@link TreeGrid} of
 * {@link TierServiceTreeItem} nodes (tier roots with service parents and
 * sub-service children), each root carrying the tier with checkboxes per
 * sub-service, and opens a {@link TierServiceForm} tab for add/edit.
 */
@RequiredArgsConstructor
@Slf4j
public class TierServiceListView extends AbstractListView {

    private final TreeGrid<TierServiceTreeItem> tierServiceTreeGrid = new TreeGrid<>(TierServiceTreeItem.class);
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final AccessService accessService;
    private final RestClientOrganizationService restClientOrganizationService;
    private transient List<ServiceDto> serviceDtos = new ArrayList<>();
    @Getter
    @Setter
    private transient BrandDto brandDto = new BrandDto();
    private final LoadingBar loadingBar = new LoadingBar();
    private final GridSkeleton gridSkeleton = new GridSkeleton(8);
    private Button[] buttonEdits;
    private Button[] buttonDeletes;
    private final Map<String, Checkbox> checkBoxes = new HashMap<>();
    private final Set<String> expandedServiceIds = new HashSet<>();
    private final transient Map<Integer, Date> lastSavedByTier = new HashMap<>();

    private void renderLayout() {
        setSizeFull();
        setPadding(false);
        brandDto.setId(sessionBrandId(accessService));
        configureGrid();

        add(loadingBar, gridSlot(tierServiceTreeGrid, gridSkeleton));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        refreshOnBroadcast(Set.of(BroadcastMessage.TIER_INSERT_SUCCESS,
                BroadcastMessage.TIER_UPDATED_SUCCESS,
                BroadcastMessage.TIER_DELETED_SUCCESS), this::fetchTier);
        renderLayout();
        fetchService();
    }

    private void configureGrid() {
        tierServiceTreeGrid.setSizeFull();
        tierServiceTreeGrid.removeAllColumns();
        tierServiceTreeGrid.setEmptyStateText("No service tiers yet \u2014 click \u201CNew Tier Service\u201D to add one.");

        tierServiceTreeGrid.addHierarchyColumn(TierServiceTreeItem::getName).setHeader("Tier Name");
        tierServiceTreeGrid.addComponentColumn(this::applyCheckbox).setHeader("Active");
        tierServiceTreeGrid.addComponentColumn(this::applyButton).setHeader("Action");
        tierServiceTreeGrid.getColumns().forEach(productDtoColumn -> productDtoColumn.setAutoWidth(true));
        tierServiceTreeGrid.addExpandListener(expandEvent -> expandEvent.getItems()
                .forEach(item -> {
                    if (item != null && ObjectUtils.isNotEmpty(item.getId())) {
                        expandedServiceIds.add(item.getId());
                    }
                }));
        tierServiceTreeGrid.addCollapseListener(collapseEvent -> collapseEvent.getItems()
                .forEach(item -> {
                    if (item != null && ObjectUtils.isNotEmpty(item.getId())) {
                        expandedServiceIds.remove(item.getId());
                    }
                }));
    }

    /**
     * Loads the brands for the current user, then opens a tab containing a
     * {@link TierServiceForm} for the given tier. Brands are resolved before
     * the tab opens so the brand combo box is populated synchronously on attach
     * and the tier's brand can be pre-selected.
     *
     * @param tierDto the tier to edit, or a new empty one to create
     * @param action  whether the tab is in create or edit mode
     */
    private void editTier(TierDto tierDto, FormAction action) {
        if (tierDto.getBrandId() == null) {
            tierDto.setBrandId(brandDto.getId());
        }
        loadingBar.start();
        asyncRestClientOrganizationService.getAllBrandAsync(brands ->
                UiUtil.safeAccess(ui, () -> {
                    loadingBar.stop();
                    if (!(this.getParent().orElseThrow() instanceof TabSheet tabSheet)) {
                        return;
                    }
                    TabManager tabManager = new TabManager(tabSheet);
                    String tabLabel = action == FormAction.EDIT && ObjectUtils.isNotEmpty(tierDto.getName())
                            ? "Edit ".concat(tierDto.getName()) : "New Tier Service";
                    tabManager.addOrSelect(tabLabel, tab -> new TierServiceForm(restClientOrganizationService,
                            asyncRestClientOrganizationService, tabManager, tab, action, tierDto, brands));
                }));
    }

    /**
     * Builds the toolbar with a lazy name filter and a "New Tier Service"
     * button.
     *
     * @return the toolbar layout to place above the tree grid
     */
    public HorizontalLayout getToolbarComponent() {
        configureSearchFilter();

        Button addTierServiceButton = UiUtil.addButton("New Tier Service", event -> addTier());

        HorizontalLayout toolbar = new HorizontalLayout(filterText, addTierServiceButton);
        toolbar.addClassName("toolbar");
        registerNewShortcut(this::addTier);
        return toolbar;
    }

    private void addTier() {
        TierDto tierDto = new TierDto();
        tierDto.setType(TierTypeDto.SERVICE);
        editTier(tierDto, FormAction.CREATE);
    }

    private void fetchTier() {
        gridSkeleton.show();
        asyncRestClientOrganizationService.getTierServiceByBrandAsync(this::operationFinished,
                error -> UiUtil.safeAccess(ui, () -> {
                    gridSkeleton.hide();
                    UiUtil.errorWithRetry("Couldn't load service tiers", this::fetchTier);
                }), brandDto.getId());
    }

    private void fetchService() {
        asyncRestClientOrganizationService.getAllServicesAsync(result -> {
            serviceDtos = result;
            fetchTier();
        }, error -> UiUtil.safeAccess(ui, () -> UiUtil.errorWithRetry("Couldn't load services", this::fetchService)));
    }

    private Component applyCheckbox(TierServiceTreeItem tierServiceTreeItem) {
        if (tierServiceTreeItem.getTreeLevel().equals(TreeLevel.ROOT)
                || tierServiceTreeItem.getTreeLevel().equals(TreeLevel.PARENT)) {
            return null;
        }

        Checkbox checkbox = new Checkbox(tierServiceTreeItem.isActive());
        VerticalLayout cell = new VerticalLayout();
        cell.setPadding(false);
        cell.setSpacing(false);
        Span status = new Span();
        status.setText(UiUtil.tierSavedText(lastSavedByTier.get(getTierDto(tierServiceTreeItem).getId())));
        status.addClassName("tier-saved-at");
        status.setVisible(status.getText() != null);
        final boolean[] rollingBack = {false};
        checkbox.addValueChangeListener(event -> {
            if (rollingBack[0]) {
                return;
            }
            boolean previous = event.getOldValue();
            tierServiceTreeItem.setActive(event.getValue());
            TierServiceTreeItem rootItem = tierServiceTreeItem.getTierServiceTreeItemParent() != null
                    ? tierServiceTreeItem.getTierServiceTreeItemParent().getTierServiceTreeItemParent() : null;
            if (ObjectUtils.isNotEmpty(rootItem)) {
                Integer tierId = getTierDto(rootItem).getId();
                Date priorSaved = lastSavedByTier.get(tierId);
                lastSavedByTier.put(tierId, new Date());
                checkbox.setEnabled(false);
                status.removeClassName("tier-saved-at");
                status.setText("Saving\u2026");
                status.addClassName("tier-saving");
                status.setVisible(true);
                new TierSubServiceUpdateEventListener(ui, restClientOrganizationService,
                        tierServiceTreeGrid, rootItem, getTierDto(rootItem)).execute(() -> {
                            rollingBack[0] = true;
                            checkbox.setEnabled(true);
                            if (priorSaved != null) {
                                lastSavedByTier.put(tierId, priorSaved);
                            } else {
                                lastSavedByTier.remove(tierId);
                            }
                            status.removeClassName("tier-saving");
                            String savedText = UiUtil.tierSavedText(priorSaved);
                            status.setText(savedText);
                            if (savedText == null) {
                                status.setVisible(false);
                            } else {
                                status.addClassName("tier-saved-at");
                            }
                            checkbox.setValue(previous);
                            rollingBack[0] = false;
                        });
            }
        });

        checkBoxes.put(tierServiceTreeItem.getId(), checkbox);

        cell.add(checkbox, status);
        return cell;
    }

    private Component applyButton(TierServiceTreeItem tierServiceTreeItem) {
        if (tierServiceTreeItem.getTreeLevel().equals(TreeLevel.ROOT)) {
            HorizontalLayout layout = new HorizontalLayout();
            layout.add(applyButtonEdit(tierServiceTreeItem));
            layout.add(applyButtonDelete(tierServiceTreeItem));
            return layout;
        }
        return null;
    }

    private Button applyButtonDelete(TierServiceTreeItem tierServiceTreeItem) {
        buttonDeletes[tierServiceTreeItem.getRootIndex()] = UiUtil.deleteButton(
                new TierServiceDeleteEventListener(
                        Integer.valueOf(tierServiceTreeItem.getId()),
                        restClientOrganizationService, ui));
        return buttonDeletes[tierServiceTreeItem.getRootIndex()];
    }

    private Button applyButtonEdit(TierServiceTreeItem tierServiceTreeItem) {
        buttonEdits[tierServiceTreeItem.getRootIndex()] = UiUtil.editButton("Edit Name",
                event -> editTier(getTierDto(tierServiceTreeItem), FormAction.EDIT));
        return buttonEdits[tierServiceTreeItem.getRootIndex()];
    }

    private static TierDto getTierDto(TierServiceTreeItem tierServiceTreeItem) {
        TierDto tierDto = new TierDto();
        tierDto.setId(Integer.parseInt(tierServiceTreeItem.getId()));
        tierDto.setType(TierTypeDto.SERVICE);
        tierDto.setName(tierServiceTreeItem.getName());
        return tierDto;
    }

    private void operationFinished(List<TierServiceDto> result) {
        TreeData<TierServiceTreeItem> tierServiceTreeItemTreeData = new TreeData<>();

        Map<TierDto, List<TierServiceDto>> tierGroup = result.stream().collect(
                Collectors.groupingBy(TierServiceDto::getTierDto));

        buttonEdits = new Button[tierGroup.size()];
        buttonDeletes = new Button[tierGroup.size()];

        AtomicInteger rootIndex = new AtomicInteger();

        tierGroup.forEach((tierDto, tierServiceDtos) -> {
            TierServiceTreeItem tierServiceTreeItem = getTreeItem(rootIndex.getAndIncrement(),
                    tierDto.getId().toString(), tierDto.getName(), null, null, false, TreeLevel.ROOT);

            tierServiceTreeItemTreeData.addItem(null, tierServiceTreeItem);
            extractedServiceName(tierServiceTreeItemTreeData, tierServiceTreeItem, tierServiceDtos);
        });

        UiUtil.safeAccess(ui, () -> {
            gridSkeleton.hide();
            tierServiceTreeGrid.setTreeData(tierServiceTreeItemTreeData);
            if (ObjectUtils.isNotEmpty(expandedServiceIds)) {
                TreeData<TierServiceTreeItem> treeData = tierServiceTreeGrid.getTreeData();
                List<TierServiceTreeItem> items = new ArrayList<>();
                treeData.getRootItems().forEach(rootItem -> {
                    items.add(rootItem);
                    treeData.getChildren(rootItem).forEach(parentItem -> {
                        items.add(parentItem);
                        items.addAll(treeData.getChildren(parentItem));
                    });
                });
                items.stream()
                        .filter(item -> expandedServiceIds.contains(item.getId()))
                        .forEach(tierServiceTreeGrid::expand);
            }
        });
    }

    private void extractedServiceName(TreeData<TierServiceTreeItem> tierServiceTreeItemTreeData,
                                      TierServiceTreeItem tierServiceTreeItem, List<TierServiceDto> tierServiceDtos) {
        serviceDtos.parallelStream().forEach(serviceDto -> {
            TierServiceTreeItem tServiceTreeItem = getTierServiceTreeItem(tierServiceTreeItem, serviceDto);
            tierServiceTreeItemTreeData.addItem(tierServiceTreeItem, tServiceTreeItem);
            extractedSubServiceName(tierServiceTreeItemTreeData, tierServiceTreeItem, tierServiceDtos,
                    serviceDto, tServiceTreeItem);
        });
    }

    private static void extractedSubServiceName(TreeData<TierServiceTreeItem> tierServiceTreeItemTreeData,
                                                TierServiceTreeItem tierServiceTreeItem, List<TierServiceDto> tierServiceDtos,
                                                ServiceDto serviceDto, TierServiceTreeItem tServiceTreeItem) {
        serviceDto.getSubServices().forEach(subServiceDto -> tierServiceTreeItemTreeData.addItem(tServiceTreeItem,
                getTierServiceTreeItemSubService(tierServiceTreeItem, serviceDto, tServiceTreeItem,
                        subServiceDto, getMatchTierSubService(tierServiceDtos, subServiceDto))));
    }

    private static TierServiceDto getMatchTierSubService(List<TierServiceDto> tierServiceDtos, SubServiceDto subServiceDto) {
        return tierServiceDtos.stream().filter(tierServiceDto -> ObjectUtils.isNotEmpty(tierServiceDto.getSubServiceDto()) &&
                subServiceDto.getId().equals(tierServiceDto.getSubServiceDto().getId())).findFirst().orElse(null);
    }

    private static TierServiceTreeItem getTierServiceTreeItemSubService(TierServiceTreeItem tierServiceTreeItem, ServiceDto serviceDto,
                                                                        TierServiceTreeItem tServiceTreeItem, SubServiceDto subServiceDto,
                                                                        TierServiceDto tierServiceDtoFound) {
        return getTreeItem(null,
                tierServiceTreeItem.getId()
                        .concat("-").concat(String.valueOf(serviceDto.getId()))
                        .concat("-").concat(String.valueOf(subServiceDto.getId())),
                subServiceDto.getName(),subServiceDto.getId(), tServiceTreeItem,
                (tierServiceDtoFound != null && tierServiceDtoFound.isActive()),
                TreeLevel.CHILD);
    }

    private static TierServiceTreeItem getTierServiceTreeItem(TierServiceTreeItem tierServiceTreeItem, ServiceDto serviceDto) {
        return getTreeItem(null,
                tierServiceTreeItem.getId()
                        .concat("-")
                        .concat(String.valueOf(serviceDto.getId())),
                serviceDto.getName(), null,
                tierServiceTreeItem,
                false,
                TreeLevel.PARENT);
    }

    private static TierServiceTreeItem getTreeItem(Integer rootIndex, String id, String name, Integer subServiceId,
                                                   TierServiceTreeItem parent, boolean isActive, TreeLevel level) {
        return TierServiceTreeItem.builder()
                .rootIndex(rootIndex)
                .id(id)
                .name(name)
                .subServiceId(subServiceId)
                .tierServiceTreeItemParent(parent)
                .active(isActive)
                .treeLevel(level)
                .build();
    }
}
