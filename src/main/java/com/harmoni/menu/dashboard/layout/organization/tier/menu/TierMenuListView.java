package com.harmoni.menu.dashboard.layout.organization.tier.menu;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.*;
import com.harmoni.menu.dashboard.event.tier.TierDeleteEventListener;
import com.harmoni.menu.dashboard.event.tier.TierMenuUpdateEventListener;
import com.harmoni.menu.dashboard.layout.AbstractListView;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.organization.tier.service.TreeLevel;
import com.harmoni.menu.dashboard.layout.util.GridSkeleton;
import com.harmoni.menu.dashboard.layout.util.LoadingBar;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
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
 * Vaadin tree-grid view listing menu tiers. Renders a {@link TreeGrid} of
 * {@link TierMenuTreeItem} nodes (tier roots with category children), each root
 * carrying the tier with checkboxes per child category, and opens a
 * {@link TierMenuForm} tab for add/edit of tiers.
 */
@RequiredArgsConstructor
@Slf4j
public class TierMenuListView extends AbstractListView {

    private final TreeGrid<TierMenuTreeItem> tierMenuTreeGrid = new TreeGrid<>(TierMenuTreeItem.class);
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final AccessService accessService;

    private final RestClientOrganizationService restClientOrganizationService;
    private transient List<CategoryDto> categoryDtos = new ArrayList<>();
    @Getter
    @Setter
    private transient BrandDto brandDto = new BrandDto();
    private final LoadingBar loadingBar = new LoadingBar();
    private final GridSkeleton gridSkeleton = new GridSkeleton(8);

    private final transient Map<Integer, Date> lastSavedByTier = new HashMap<>();

    private Button[] buttonEdits;
    private Button[] buttonDeletes;
    private final Map<String, Checkbox> checkBoxes = new HashMap<>();
    private final Set<Integer> expandedTierIds = new HashSet<>();

    private void renderLayout() {
        setSizeFull();
        setPadding(false);
        brandDto.setId(accessService.getUserDetail().getStoreDto().getChainDto().getBrandId());
        configureGrid();
        add(loadingBar, gridSlot(tierMenuTreeGrid, gridSkeleton));
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

    private void configureGrid() {
        tierMenuTreeGrid.setSizeFull();
        tierMenuTreeGrid.removeAllColumns();
        tierMenuTreeGrid.setEmptyStateText("No menu tiers yet \u2014 click \u201CNew Tier Menu\u201D to add one.");

        tierMenuTreeGrid.addHierarchyColumn(TierMenuTreeItem::getName).setHeader("Tier Name");
        tierMenuTreeGrid.addComponentColumn(this::applyCheckbox).setHeader("Active");
        tierMenuTreeGrid.addComponentColumn(this::applyButton).setHeader("Action");
        tierMenuTreeGrid.getColumns().forEach(productDtoColumn -> productDtoColumn.setAutoWidth(true));
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

    /**
     * Loads the brands for the current user, then opens a tab containing a
     * {@link TierMenuForm} for the given tier. Brands are resolved before the
     * tab opens so the brand combo box is populated synchronously on attach and
     * the tier's brand can be pre-selected.
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
                            ? "Edit ".concat(tierDto.getName()) : "New Tier Menu";
                    tabManager.addOrSelect(tabLabel, tab -> new TierMenuForm(restClientOrganizationService,
                            asyncRestClientOrganizationService, tabManager, tab, action, tierDto, brands));
                }));
    }

    /**
     * Builds the toolbar with a lazy name filter and a "New Tier Menu" button.
     *
     * @return the toolbar layout to place above the tree grid
     */
    public HorizontalLayout getToolbarComponent() {
        configureSearchFilter();

        Button addTierServiceButton = UiUtil.addButton("New Tier Menu", event -> addTier());

        configureBrandFilter(() -> {
            if (brandFilter.getValue().getId() != null) {
                brandDto.setId(brandFilter.getValue().getId());
                fetchCategories();
            }
        });
        HorizontalLayout toolbar = new HorizontalLayout(brandFilter, filterText, addTierServiceButton);
        toolbar.addClassName("toolbar");
        toolbar.setAlignItems(FlexComponent.Alignment.BASELINE);
        registerNewShortcut(this::addTier);
        return toolbar;
    }

    private void addTier() {
        TierDto tierDto = new TierDto();
        tierDto.setType(TierTypeDto.MENU);
        editTier(tierDto, FormAction.CREATE);
    }

    private void fetchTier() {
        gridSkeleton.show();
        asyncRestClientOrganizationService.getTierMenuByBrandAsync(this::operationFinished,
                error -> UiUtil.safeAccess(ui, () -> {
                    gridSkeleton.hide();
                    UiUtil.errorWithRetry("Couldn't load menu tiers", this::fetchTier);
                }), brandDto.getId());
    }

    private void fetchCategories() {
        asyncRestClientMenuService.getAllCategoryAsync(result -> {
            categoryDtos = result;
            fetchTier();
        }, error -> UiUtil.safeAccess(ui, () -> UiUtil.errorWithRetry("Couldn't load categories",
                () -> loadBrands(asyncRestClientOrganizationService, accessService, this::fetchCategories))),
                brandDto.getId());
    }

    private Component applyCheckbox(TierMenuTreeItem tierMenuTreeItem) {
        if (tierMenuTreeItem.getTreeLevel().equals(TreeLevel.ROOT)
                || tierMenuTreeItem.getTreeLevel().equals(TreeLevel.PARENT)) {
            return null;
        }

        Checkbox checkbox = new Checkbox(tierMenuTreeItem.isActive());
        VerticalLayout cell = new VerticalLayout();
        cell.setPadding(false);
        cell.setSpacing(false);
        Span status = new Span();
        status.setText(UiUtil.tierSavedText(lastSavedByTier.get(getTierDto(tierMenuTreeItem).getId())));
        status.addClassName("tier-saved-at");
        status.setVisible(status.getText() != null);
        final boolean[] rollingBack = {false};
        checkbox.addValueChangeListener(event -> {
            if (rollingBack[0]) {
                return;
            }
            boolean previous = event.getOldValue();
            tierMenuTreeItem.setActive(event.getValue());
            TierMenuTreeItem rootItem = tierMenuTreeItem.getItemParent();
            if (ObjectUtils.isNotEmpty(rootItem)) {
                Integer tierId = getTierDto(rootItem).getId();
                Date priorSaved = lastSavedByTier.get(tierId);
                lastSavedByTier.put(tierId, new Date());
                checkbox.setEnabled(false);
                status.removeClassName("tier-saved-at");
                status.setText("Saving\u2026");
                status.addClassName("tier-saving");
                status.setVisible(true);
                new TierMenuUpdateEventListener(this.ui, restClientOrganizationService, tierMenuTreeGrid,
                        rootItem, getTierDto(rootItem)).execute(() -> {
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

        checkBoxes.put(tierMenuTreeItem.getId(), checkbox);

        cell.add(checkbox, status);
        return cell;
    }

    private Component applyButton(TierMenuTreeItem tierMenuTreeItem) {
        if (tierMenuTreeItem.getTreeLevel().equals(TreeLevel.ROOT)) {
            HorizontalLayout layout = new HorizontalLayout();
            layout.add(applyButtonEdit(tierMenuTreeItem));
            layout.add(applyButtonDelete(tierMenuTreeItem));
            return layout;
        }
        return null;
    }

    private Button applyButtonDelete(TierMenuTreeItem tierMenuTreeItem) {
        buttonDeletes[tierMenuTreeItem.getRootIndex()] = UiUtil.deleteButton(
                new TierDeleteEventListener(this.ui,
                        tierMenuTreeItem.getTierDto().getId(),
                        restClientOrganizationService));
        return buttonDeletes[tierMenuTreeItem.getRootIndex()];
    }

    private Button applyButtonEdit(TierMenuTreeItem tierMenuTreeItem) {
        buttonEdits[tierMenuTreeItem.getRootIndex()] = UiUtil.editButton("Edit Name",
                event -> editTier(getTierDto(tierMenuTreeItem), FormAction.EDIT));
        return buttonEdits[tierMenuTreeItem.getRootIndex()];
    }

    private static TierDto getTierDto(TierMenuTreeItem tierMenuTreeItem) {
        TierDto tierDto = new TierDto();
        tierDto.setId(tierMenuTreeItem.getTierDto().getId());
        tierDto.setBrandId(tierMenuTreeItem.getTierDto().getBrandId());
        tierDto.setType(TierTypeDto.MENU);
        tierDto.setName(tierMenuTreeItem.getName());
        return tierDto;
    }

    private void operationFinished(List<TierMenuDto> result) {
        TreeData<TierMenuTreeItem> tierMenuTreeItemTreeData = new TreeData<>();

        Map<TierDto, List<TierMenuDto>> tierGroup = result.stream().collect(
                Collectors.groupingBy(TierMenuDto::getTierDto));

        buttonEdits = new Button[tierGroup.size()];
        buttonDeletes = new Button[tierGroup.size()];

        AtomicInteger rootIndex = new AtomicInteger();

        tierGroup.forEach((tierDto, tierMenuDtos) -> {
            TierMenuTreeItem tierMenuTreeItem = getTierMenuTreeItem(rootIndex.getAndIncrement(), tierDto,
                    tierDto.getName(), null, false, null, TreeLevel.ROOT);
            tierMenuTreeItemTreeData.addItem(null, tierMenuTreeItem);
            extractedCategoryName(tierMenuTreeItemTreeData, tierMenuTreeItem, tierMenuDtos);
        });

        UiUtil.safeAccess(ui, () -> {
            gridSkeleton.hide();
            tierMenuTreeGrid.setTreeData(tierMenuTreeItemTreeData);
            if (ObjectUtils.isNotEmpty(expandedTierIds)) {
                tierMenuTreeGrid.getTreeData().getRootItems().stream()
                        .filter(rootItem -> rootItem.getTreeLevel().equals(TreeLevel.ROOT)
                                && ObjectUtils.isNotEmpty(rootItem.getTierDto())
                                && ObjectUtils.isNotEmpty(rootItem.getTierDto().getId())
                                && expandedTierIds.contains(rootItem.getTierDto().getId()))
                        .forEach(tierMenuTreeGrid::expand);
            }
        });
    }

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

    private void extractedCategoryName(TreeData<TierMenuTreeItem> tierMenuTreeItemTreeData,
                                       TierMenuTreeItem tierMenuTreeItem, List<TierMenuDto> tierMenuDtos) {
        for (CategoryDto categoryDto: categoryDtos) {
            TierMenuTreeItem childItem = getTierMenuTreeItem(null, tierMenuTreeItem.getTierDto(),
                    categoryDto.getName(), categoryDto,
                    false, tierMenuTreeItem, TreeLevel.CHILD);
            tierMenuDtos.forEach(tierMenuDto -> {
                if (ObjectUtils.isNotEmpty(tierMenuDto.getCategoryDto())
                        && tierMenuDto.getCategoryDto().getId().equals(categoryDto.getId())) {
                    childItem.setActive(tierMenuDto.getActive());
                }
            });
            tierMenuTreeItemTreeData.addItem(tierMenuTreeItem, childItem);
        }
    }
}
