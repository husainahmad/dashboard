package com.harmoni.menu.dashboard.layout.organization.tier.menu;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.*;
import com.harmoni.menu.dashboard.event.tier.TierDeleteEventListener;
import com.harmoni.menu.dashboard.event.tier.TierMenuUpdateEventListener;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.layout.organization.tier.AbstractTierTreeListView;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.organization.tier.service.TreeLevel;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientBase.AsyncRestCallback;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
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
 * Vaadin tree-grid view listing menu tiers. Renders a {@link TreeGrid} of
 * {@link TierMenuTreeItem} nodes (tier roots with category children), each root
 * carrying the tier with checkboxes per child category, and opens a
 * {@link TierMenuForm} tab for add/edit of tiers.
 */
@Slf4j
public class TierMenuListView extends AbstractTierTreeListView<TierMenuTreeItem> {

    private final TreeGrid<TierMenuTreeItem> tierMenuTreeGrid = new TreeGrid<>(TierMenuTreeItem.class);
    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private transient List<CategoryDto> categoryDtos = new ArrayList<>();
    private final Set<Integer> expandedTierIds = new HashSet<>();

    public TierMenuListView(AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                            AsyncRestClientMenuService asyncRestClientMenuService,
                            AccessService accessService,
                            RestClientOrganizationService restClientOrganizationService) {
        super(asyncRestClientOrganizationService, accessService, restClientOrganizationService);
        this.asyncRestClientMenuService = asyncRestClientMenuService;
    }

    @Override
    protected TreeGrid<TierMenuTreeItem> treeGrid() {
        return tierMenuTreeGrid;
    }

    @Override
    protected String emptyStateText() {
        return Messages.get("grid.empty.tierMenus");
    }

    @Override
    protected String newTierLabel() {
        return Messages.get("tab.newTierMenu");
    }

    @Override
    protected String loadingErrorMessage() {
        return Messages.get("notification.tierMenu.loadFailed");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        renderLayout();
        refreshOnBroadcast(Set.of(BroadcastMessage.TIER_INSERT_SUCCESS,
                BroadcastMessage.TIER_UPDATED_SUCCESS,
                BroadcastMessage.TIER_DELETED_SUCCESS), this::fetchTier);
        loadBrands(asyncRestClientOrganizationService, accessService, () -> {
            if (brandFilter.getValue() != null && brandFilter.getValue().getId() != null) {
                brandDto.setId(brandFilter.getValue().getId());
            }
            fetchCategories();
        });
    }

    @Override
    protected void wireExpansionTracking() {
        tierMenuTreeGrid.addExpandListener(expandEvent -> expandEvent.getItems()
                .forEach(item -> {
                    if (item.getTreeLevel().equals(TreeLevel.ROOT)
                            && ObjectUtils.isNotEmpty(item.getTierDto())
                            && ObjectUtils.isNotEmpty(item.getTierDto().getId())) {
                        expandedTierIds.add(item.getTierDto().getId());
                    }
                }));
        tierMenuTreeGrid.addCollapseListener(collapseEvent -> collapseEvent.getItems()
                .forEach(item -> {
                    if (item.getTreeLevel().equals(TreeLevel.ROOT)
                            && ObjectUtils.isNotEmpty(item.getTierDto())
                            && ObjectUtils.isNotEmpty(item.getTierDto().getId())) {
                        expandedTierIds.remove(item.getTierDto().getId());
                    }
                }));
    }

    @Override
    protected void restoreExpansion() {
        if (ObjectUtils.isEmpty(expandedTierIds)) {
            return;
        }
        tierMenuTreeGrid.getTreeData().getRootItems().stream()
                .filter(rootItem -> rootItem.getTreeLevel().equals(TreeLevel.ROOT)
                        && ObjectUtils.isNotEmpty(rootItem.getTierDto())
                        && ObjectUtils.isNotEmpty(rootItem.getTierDto().getId())
                        && expandedTierIds.contains(rootItem.getTierDto().getId()))
                .forEach(tierMenuTreeGrid::expand);
    }

    /**
     * Builds the toolbar with a lazy name filter and a "New Tier Menu" button.
     *
     * @return the toolbar layout to place above the tree grid
     */
    public HorizontalLayout getToolbarComponent() {
        configureSearchFilter();

        Button addTierServiceButton = UiUtil.addButton(Messages.get("action.newTierMenu"), event -> addTier());

        configureBrandFilter(() -> {
            if (brandFilter.getValue().getId() != null) {
                brandDto.setId(brandFilter.getValue().getId());
                fetchCategories();
            }
        });
        HorizontalLayout toolbar = new HorizontalLayout(brandFilter, filterText, addTierServiceButton);
        toolbar.addClassName(Css.TOOLBAR);
        toolbar.setAlignItems(FlexComponent.Alignment.BASELINE);
        registerNewShortcut(this::addTier);
        return toolbar;
    }

    private void addTier() {
        TierDto tierDto = new TierDto();
        tierDto.setType(TierTypeDto.MENU);
        editTier(tierDto, FormAction.CREATE);
    }

    @Override
    protected void fetchTierData(AsyncRestCallback<Throwable> errorHandler) {
        asyncRestClientOrganizationService.getTierMenuByBrandAsync(this::operationFinished, errorHandler,
                brandDto.getId());
    }

    /**
     * Fetches all categories asynchronously and then fetches the tiers.
     * If fetching categories fails, it shows an error notification with a retry option.
     */
    private void fetchCategories() {
        asyncRestClientMenuService.getAllCategoryAsync(result -> {
            categoryDtos = result;
            fetchTier();
        }, error -> UiUtil.safeAccess(ui, () -> UiUtil.errorWithRetry(Messages.get(Messages.Keys.NOTIFICATION_CATEGORY_LOAD_FAILED),
                () -> loadBrands(asyncRestClientOrganizationService, accessService, this::fetchCategories))),
                brandDto.getId());
    }

    /**
     * Returns the parent of the given {@link TierMenuTreeItem}.
     *
     * @param item the tree item whose parent is to be returned
     * @return the parent tree item, or null if the item is a root
     */
    @Override
    protected TierMenuTreeItem tierRoot(TierMenuTreeItem item) {
        return item.getItemParent();
    }

    /**
     * Converts a {@link TierMenuTreeItem} to a {@link TierDto} for saving.
     *
     * @param item the tree item to convert
     * @return the corresponding TierDto
     */
    @Override
    protected TierDto tierDtoOf(TierMenuTreeItem item) {
        TierDto tierDto = new TierDto();
        tierDto.setId(item.getTierDto().getId());
        tierDto.setBrandId(item.getTierDto().getBrandId());
        tierDto.setType(TierTypeDto.MENU);
        tierDto.setName(item.getName());
        return tierDto;
    }

    @Override
    protected void saveCheckbox(TierMenuTreeItem item, TierMenuTreeItem rootItem, Runnable rollback) {
        new TierMenuUpdateEventListener(ui, restClientOrganizationService, tierMenuTreeGrid,
                rootItem, tierDtoOf(rootItem)).execute(rollback);
    }

    @Override
    protected Button deleteButtonFor(TierMenuTreeItem item) {
        return UiUtil.deleteButton(new TierDeleteEventListener(ui, item.getTierDto().getId(),
                restClientOrganizationService));
    }

    @Override
    protected Component openForm(RestClientOrganizationService sync,
                                 AsyncRestClientOrganizationService async, TabManager tabManager,
                                 Tab currentTab, FormAction formAction, TierDto tierDto,
                                 List<BrandDto> brands) {
        return new TierMenuForm(sync, async, tabManager, currentTab, formAction, tierDto, brands);
    }

    private void operationFinished(List<TierMenuDto> result) {
        TreeData<TierMenuTreeItem> treeData = new TreeData<>();

        Map<TierDto, List<TierMenuDto>> tierGroup = result.stream().collect(
                Collectors.groupingBy(TierMenuDto::getTierDto));

        buttonEdits = new Button[tierGroup.size()];
        buttonDeletes = new Button[tierGroup.size()];

        AtomicInteger rootIndex = new AtomicInteger();

        tierGroup.forEach((tierDto, tierMenuDtos) -> {
            TierMenuTreeItem tierMenuTreeItem = getTierMenuTreeItem(rootIndex.getAndIncrement(), tierDto,
                    tierDto.getName(), null, false, null, TreeLevel.ROOT);
            treeData.addItem(null, tierMenuTreeItem);
            extractedCategoryName(treeData, tierMenuTreeItem, tierMenuDtos);
        });

        finishLoad(treeData);
    }

    /**
     * Creates a new {@link TierMenuTreeItem} with the given parameters.
     *
     * @param rootIndex    the index of the root item
     * @param tierDto      the tier data transfer object
     * @param name         the name of the item
     * @param categoryDto  the category data transfer object (can be null for root items)
     * @param isActive     whether the item is active
     * @param parent       the parent tree item (null for root items)
     * @param treeLevel    the level of the tree (ROOT or CHILD)
     * @return a new TierMenuTreeItem instance
     */
    private static TierMenuTreeItem getTierMenuTreeItem(Integer rootIndex, TierDto tierDto, String name,
                                                        CategoryDto categoryDto,
                                                        boolean isActive,
                                                        TierMenuTreeItem parent,
                                                        TreeLevel treeLevel) {
        return TierMenuTreeItem.builder()
                .rootIndex(rootIndex)
                .tierDto(tierDto)
                .id(UUID.randomUUID().toString())
                .name(name)
                .categoryDto(categoryDto)
                .active(isActive)
                .itemParent(parent)
                .treeLevel(treeLevel)
                .build();
    }

    /**
     * Extracts category names from the given list of {@link CategoryDto} and adds them as child items
     * to the specified {@link TierMenuTreeItem} in the provided {@link TreeData}.
     *
     * @param treeData          the tree data structure to which child items will be added
     * @param tierMenuTreeItem  the parent tree item to which child items will be added
     * @param tierMenuDtos      the list of tier menu DTOs containing active status information
     */
    private void extractedCategoryName(TreeData<TierMenuTreeItem> treeData,
                                       TierMenuTreeItem tierMenuTreeItem, List<TierMenuDto> tierMenuDtos) {
        for (CategoryDto categoryDto : categoryDtos) {
            TierMenuTreeItem childItem = getTierMenuTreeItem(null, tierMenuTreeItem.getTierDto(),
                    categoryDto.getName(), categoryDto,
                    false, tierMenuTreeItem, TreeLevel.CHILD);
            tierMenuDtos.forEach(tierMenuDto -> {
                if (ObjectUtils.isNotEmpty(tierMenuDto.getCategoryDto())
                        && tierMenuDto.getCategoryDto().getId().equals(categoryDto.getId())) {
                    childItem.setActive(tierMenuDto.getActive());
                }
            });
            treeData.addItem(tierMenuTreeItem, childItem);
        }
    }
}