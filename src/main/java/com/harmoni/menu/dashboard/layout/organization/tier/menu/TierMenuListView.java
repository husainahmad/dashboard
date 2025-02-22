package com.harmoni.menu.dashboard.layout.organization.tier.menu;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.*;
import com.harmoni.menu.dashboard.event.tier.TierDeleteEventListener;
import com.harmoni.menu.dashboard.event.tier.TierMenuUpdateEventListener;
import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.organization.tier.TierForm;
import com.harmoni.menu.dashboard.layout.organization.tier.service.TreeLevel;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Route(value = "tier-menu", layout = MainLayout.class)
@PageTitle("Tier | POSHarmoni")
@Slf4j
public class TierMenuListView extends VerticalLayout {

    Registration broadcasterRegistration;
    private final TreeGrid<TierMenuTreeItem> tierMenuTreeGrid = new TreeGrid<>(TierMenuTreeItem.class);
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final AccessService accessService;

    private final TextField filterText = new TextField();
    @Getter
    private UI ui;
    private final RestClientOrganizationService restClientOrganizationService;
    private TierForm tierForm;
    private transient List<CategoryDto> categoryDtos = new ArrayList<>();
    private transient List<BrandDto> brandDtos;
    @Getter
    @Setter
    private transient BrandDto brandDto = new BrandDto();

    private Button[] buttonUpdates;
    private Button[] buttonEdits;
    private Button[] buttonDeletes;
    private final Map<String, Checkbox> checkBoxes = new HashMap<>();

    private void renderLayout() {
        brandDto.setId(accessService.getUserDetail().getStoreDto().getChainDto().getBrandId());
        addClassName("list-view");
        setSizeFull();
        configureGrid();

        configureForm();
        add(getToolbar(), getContent());
        closeEditor();
    }

    private void closeEditor() {
        tierForm.setVisible(false);
        removeClassName("editing");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        ui = attachEvent.getUI();
        renderLayout();
        broadcasterRegistration = Broadcaster.register(message -> {
            try {
                BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
                if (ObjectUtil.isNotEmpty(broadcastMessage)
                        && ObjectUtil.isNotEmpty(broadcastMessage.getType())
                        && (broadcastMessage.getType().equals(BroadcastMessage.TIER_INSERT_SUCCESS) ||
                            broadcastMessage.getType().equals(BroadcastMessage.TIER_UPDATED_SUCCESS) ||
                        broadcastMessage.getType().equals(BroadcastMessage.TIER_DELETED_SUCCESS))) {
                        fetchTier();
                    }

            } catch (JsonProcessingException e) {
                log.error("Broadcast Handler Error", e);
            }
        });
        fetchBrands();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    private void configureGrid() {
        tierMenuTreeGrid.setSizeFull();
        tierMenuTreeGrid.removeAllColumns();

        tierMenuTreeGrid.addHierarchyColumn(TierMenuTreeItem::getName).setHeader("Tier Name");
        tierMenuTreeGrid.addComponentColumn(this::applyCheckbox).setHeader("Selected");
        tierMenuTreeGrid.addComponentColumn(this::applyButton).setHeader("Action");
        tierMenuTreeGrid.getColumns().forEach(productDtoColumn -> productDtoColumn.setAutoWidth(true));
        tierMenuTreeGrid.addExpandListener(expandEvent -> {
            log.debug("expanded {} ", expandEvent);
            log.debug("expanded checkbox {} ", checkBoxes);
        });
    }

    private void editTier(TierDto tierDto, FormAction action) {
        tierForm.setBrandDtos(brandDtos);
        tierForm.getBrandBox().setItems(brandDtos);

        if (tierDto == null) {
            closeEditor();
        } else {
            tierDto.setBrandId(brandDto.getId());
            tierForm.changeTierDto(tierDto);
            tierForm.restructureButton(action);
            tierForm.setVisible(true);
            addClassName("editing");
        }
    }

    private void configureForm() {
        tierForm = new TierMenuForm(this.restClientOrganizationService, this.asyncRestClientOrganizationService);
        tierForm.setWidth("25em");
    }

    private HorizontalLayout getContent() {
        HorizontalLayout content = new HorizontalLayout(tierMenuTreeGrid, tierForm);
        content.setFlexGrow(2, tierMenuTreeGrid);
        content.setFlexGrow(1, tierForm);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
    }

    private HorizontalLayout getToolbar() {
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);

        Button addTierServiceButton = new Button("Add Tier Menu");
        addTierServiceButton.addClickListener(_ -> addTier());

        HorizontalLayout toolbar = new HorizontalLayout(filterText, addTierServiceButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private void addTier() {
        TierDto tierDto = new TierDto();
        tierDto.setType(TierTypeDto.MENU);
        editTier(tierDto, FormAction.CREATE);
    }

    private void fetchTier() {
        asyncRestClientOrganizationService.getTierMenuByBrandAsync(this::operationFinished, brandDto.getId());
    }

    private void fetchCategories() {
        asyncRestClientMenuService.getAllCategoryAsync(result -> {
            categoryDtos = result;
            fetchTier();
        }, 1);
    }

    private void fetchBrands() {
        asyncRestClientOrganizationService.getAllBrandAsync(result -> {
            brandDtos = result;
            fetchCategories();
        });
    }

    private Checkbox applyCheckbox(TierMenuTreeItem tierMenuTreeItem) {
        if (tierMenuTreeItem.getTreeLevel().equals(TreeLevel.ROOT)
                || tierMenuTreeItem.getTreeLevel().equals(TreeLevel.PARENT)) {
            return null;
        }

        Checkbox checkbox = new Checkbox(tierMenuTreeItem.isActive());
        checkbox.addValueChangeListener(event -> {
            tierMenuTreeItem.setActive(event.getValue());

            if (ObjectUtils.isNotEmpty(tierMenuTreeItem.getItemParent())) {
                buttonUpdates[tierMenuTreeItem.getItemParent().getRootIndex()].setEnabled(true);
            }
        });

        checkBoxes.put(tierMenuTreeItem.getId(), checkbox);

        return checkbox;
    }

    private Component applyButton(TierMenuTreeItem tierMenuTreeItem) {
        if (tierMenuTreeItem.getTreeLevel().equals(TreeLevel.ROOT)) {
            HorizontalLayout layout = new HorizontalLayout();
            layout.add(applyButtonUpdate(tierMenuTreeItem));
            layout.add(applyButtonEdit(tierMenuTreeItem));
            layout.add(applyButtonDelete(tierMenuTreeItem));
            return layout;
        }
        return null;
    }

    private Button applyButtonDelete(TierMenuTreeItem tierMenuTreeItem) {
        buttonDeletes[tierMenuTreeItem.getRootIndex()] = new Button("Delete");
        buttonDeletes[tierMenuTreeItem.getRootIndex()].addClickListener(new TierDeleteEventListener(this.ui,
                tierMenuTreeItem.getTierDto().getId(),
                restClientOrganizationService));
        return buttonDeletes[tierMenuTreeItem.getRootIndex()];
    }

    private Button applyButtonEdit(TierMenuTreeItem tierMenuTreeItem) {
        buttonEdits[tierMenuTreeItem.getRootIndex()] = new Button("Edit Name");

        buttonEdits[tierMenuTreeItem.getRootIndex()]
                .addClickListener(_ -> editTier(getTierDto(tierMenuTreeItem), FormAction.EDIT));
        return buttonEdits[tierMenuTreeItem.getRootIndex()];
    }

    private Button applyButtonUpdate(TierMenuTreeItem tierMenuTreeItem) {
        buttonUpdates[tierMenuTreeItem.getRootIndex()] = new Button("Update");
        buttonUpdates[tierMenuTreeItem.getRootIndex()].setEnabled(false);
        buttonUpdates[tierMenuTreeItem.getRootIndex()].addClickListener(new TierMenuUpdateEventListener(this.ui,
                restClientOrganizationService, tierMenuTreeGrid, tierMenuTreeItem, getTierDto(tierMenuTreeItem)));
        return buttonUpdates[tierMenuTreeItem.getRootIndex()];
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

        buttonUpdates = new Button[tierGroup.size()];
        buttonEdits = new Button[tierGroup.size()];
        buttonDeletes = new Button[tierGroup.size()];

        AtomicInteger rootIndex = new AtomicInteger();

        tierGroup.forEach((tierDto, tierMenuDtos) -> {
            TierMenuTreeItem tierMenuTreeItem = getTierMenuTreeItem(rootIndex.getAndIncrement(), tierDto,
                    tierDto.getName(), null, false, null, TreeLevel.ROOT);
            tierMenuTreeItemTreeData.addItem(null, tierMenuTreeItem);
            extractedCategoryName(tierMenuTreeItemTreeData, tierMenuTreeItem, tierMenuDtos);
        });

        ui.access(() -> tierMenuTreeGrid.setTreeData(tierMenuTreeItemTreeData));
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
