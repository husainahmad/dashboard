package com.harmoni.menu.dashboard.layout.organization.tier.service;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.*;
import com.harmoni.menu.dashboard.event.tier.TierServiceDeleteEventListener;
import com.harmoni.menu.dashboard.event.tier.TierSubServiceUpdateEventListener;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.layout.organization.tier.AbstractTierTreeListView;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientBase.AsyncRestCallback;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import com.harmoni.menu.dashboard.layout.util.Css;

/**
 * Vaadin tree-grid view listing service tiers. Renders a {@link TreeGrid} of
 * {@link TierServiceTreeItem} nodes (tier roots with service parents and
 * sub-service children), each root carrying the tier with checkboxes per
 * sub-service, and opens a {@link TierServiceForm} tab for add/edit.
 */
@Slf4j
public class TierServiceListView extends AbstractTierTreeListView<TierServiceTreeItem> {

    private final TreeGrid<TierServiceTreeItem> tierServiceTreeGrid = new TreeGrid<>(TierServiceTreeItem.class);
    private transient List<ServiceDto> serviceDtos = new ArrayList<>();
    private final Set<String> expandedServiceIds = new HashSet<>();

    public TierServiceListView(AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                               AccessService accessService,
                               RestClientOrganizationService restClientOrganizationService) {
        super(asyncRestClientOrganizationService, accessService, restClientOrganizationService);
    }

    @Override
    protected TreeGrid<TierServiceTreeItem> treeGrid() {
        return tierServiceTreeGrid;
    }

    @Override
    protected String emptyStateText() {
        return Messages.get("grid.empty.tierServices");
    }

    @Override
    protected String newTierLabel() {
        return Messages.get("tab.newTierService");
    }

    @Override
    protected String loadingErrorMessage() {
        return Messages.get("notification.tierService.loadFailed");
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

    @Override
    protected void wireExpansionTracking() {
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

    @Override
    protected void restoreExpansion() {
        if (ObjectUtils.isEmpty(expandedServiceIds)) {
            return;
        }
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

    /**
     * Builds the toolbar with a lazy name filter and a "New Tier Service"
     * button.
     *
     * @return the toolbar layout to place above the tree grid
     */
    public HorizontalLayout getToolbarComponent() {
        configureSearchFilter();

        Button addTierServiceButton = UiUtil.addButton(Messages.get("action.newTierService"), event -> addTier());

        HorizontalLayout toolbar = new HorizontalLayout(filterText, addTierServiceButton);
        toolbar.addClassName(Css.TOOLBAR);
        registerNewShortcut(this::addTier);
        return toolbar;
    }

    private void addTier() {
        TierDto tierDto = new TierDto();
        tierDto.setType(TierTypeDto.SERVICE);
        editTier(tierDto, FormAction.CREATE);
    }

    @Override
    protected void fetchTierData(AsyncRestCallback<Throwable> errorHandler) {
        asyncRestClientOrganizationService.getTierServiceByBrandAsync(this::operationFinished, errorHandler,
                brandDto.getId());
    }

    private void fetchService() {
        asyncRestClientOrganizationService.getAllServicesAsync(result -> {
            serviceDtos = result;
            fetchTier();
        }, error -> UiUtil.safeAccess(ui, () -> UiUtil.errorWithRetry(Messages.get(Messages.Keys.NOTIFICATION_SERVICE_LOAD_FAILED), this::fetchService)));
    }

    @Override
    protected TierServiceTreeItem tierRoot(TierServiceTreeItem item) {
        return item.getTierServiceTreeItemParent() != null ? item.getTierServiceTreeItemParent().getTierServiceTreeItemParent() : null;
    }

    @Override
    protected TierDto tierDtoOf(TierServiceTreeItem item) {
        TierDto tierDto = new TierDto();
        tierDto.setId(Integer.parseInt(item.getId()));
        tierDto.setType(TierTypeDto.SERVICE);
        tierDto.setName(item.getName());
        return tierDto;
    }

    @Override
    protected void saveCheckbox(TierServiceTreeItem item, TierServiceTreeItem rootItem, Runnable rollback) {
        new TierSubServiceUpdateEventListener(ui, restClientOrganizationService, tierServiceTreeGrid,
                rootItem, tierDtoOf(rootItem)).execute(rollback);
    }

    @Override
    protected Button deleteButtonFor(TierServiceTreeItem item) {
        return UiUtil.deleteButton(new TierServiceDeleteEventListener(
                Integer.valueOf(item.getId()), restClientOrganizationService, ui));
    }

    @Override
    protected Component openForm(RestClientOrganizationService sync,
                                 AsyncRestClientOrganizationService async, TabManager tabManager,
                                 Tab currentTab, FormAction formAction, TierDto tierDto,
                                 List<BrandDto> brands) {
        return new TierServiceForm(sync, async, tabManager, currentTab, formAction, tierDto, brands);
    }

    private void operationFinished(List<TierServiceDto> result) {
        TreeData<TierServiceTreeItem> treeData = new TreeData<>();

        Map<TierDto, List<TierServiceDto>> tierGroup = result.stream().collect(
                Collectors.groupingBy(TierServiceDto::getTierDto));

        buttonEdits = new Button[tierGroup.size()];
        buttonDeletes = new Button[tierGroup.size()];

        AtomicInteger rootIndex = new AtomicInteger();

        tierGroup.forEach((tierDto, tierServiceDtos) -> {
            TierServiceTreeItem tierServiceTreeItem = getTreeItem(rootIndex.getAndIncrement(),
                    tierDto.getId().toString(), tierDto.getName(), null, null, false, TreeLevel.ROOT);

            treeData.addItem(null, tierServiceTreeItem);
            extractedServiceName(treeData, tierServiceTreeItem, tierServiceDtos);
        });

        finishLoad(treeData);
    }

    private void extractedServiceName(TreeData<TierServiceTreeItem> treeData,
                                      TierServiceTreeItem tierServiceTreeItem, List<TierServiceDto> tierServiceDtos) {
        serviceDtos.parallelStream().forEach(serviceDto -> {
            TierServiceTreeItem tServiceTreeItem = getTierServiceTreeItem(tierServiceTreeItem, serviceDto);
            treeData.addItem(tierServiceTreeItem, tServiceTreeItem);
            extractedSubServiceName(treeData, tierServiceTreeItem, tierServiceDtos, serviceDto, tServiceTreeItem);
        });
    }

    private static void extractedSubServiceName(TreeData<TierServiceTreeItem> treeData,
                                                TierServiceTreeItem tierServiceTreeItem, List<TierServiceDto> tierServiceDtos,
                                                ServiceDto serviceDto, TierServiceTreeItem tServiceTreeItem) {
        serviceDto.getSubServices().forEach(subServiceDto -> treeData.addItem(tServiceTreeItem,
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
                subServiceDto.getName(), subServiceDto.getId(), tServiceTreeItem,
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