package com.harmoni.menu.dashboard.layout.organization.tier.menu;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.*;
import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.organization.tier.TierForm;
import com.harmoni.menu.dashboard.layout.organization.tier.service.TreeLevel;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
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
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

@Route(value = "tier-menu", layout = MainLayout.class)
@PageTitle("Tier | POSHarmoni")
@Slf4j
public class TierMenuListView extends VerticalLayout {

    Registration broadcasterRegistration;
    private final TreeGrid<TierMenuTreeItem> tierMenuTreeGrid = new TreeGrid<>(TierMenuTreeItem.class);
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final AsyncRestClientMenuService asyncRestClientMenuService;

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
    private static final Integer TEMP_BRAND_ID = 1;
    private Button[] buttonUpdates;
    private Button[] buttonEdits;
    private Button[] buttonDeletes;
    private final Map<String, Checkbox> checkBoxes = new HashMap<>();

    public TierMenuListView(@Autowired AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                            @Autowired RestClientOrganizationService restClientOrganizationService,
                            @Autowired AsyncRestClientMenuService asyncRestClientMenuService) {
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
        this.restClientOrganizationService = restClientOrganizationService;
        this.asyncRestClientMenuService = asyncRestClientMenuService;

        brandDto.setId(TEMP_BRAND_ID);
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
        broadcasterRegistration = Broadcaster.register(message -> {

            try {
                BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
                if (ObjectUtil.isNotEmpty(broadcastMessage)
                        && ObjectUtil.isNotEmpty(broadcastMessage.getType())
                        && (broadcastMessage.getType().equals(BroadcastMessage.TIER_INSERT_SUCCESS) ||
                            broadcastMessage.getType().equals(BroadcastMessage.TIER_UPDATED_SUCCESS))) {
                        fetchTier();
                    }

            } catch (JsonProcessingException e) {
                log.error("Broadcast Handler Error", e);
            }
        });

        fetchBrands();
        fetchTier();
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
        addTierServiceButton.addClickListener(e -> addTier());

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
        asyncRestClientOrganizationService.getAllTierByBrandAsync(this::operationFinished, brandDto.getId(),
                TierTypeDto.MENU);
    }

    private void extractedCategoryName(TreeData<TierMenuTreeItem> tierMenuTreeItemTreeData,
                                       TierMenuTreeItem tierMenuTreeItem, TierDto tierDto) {

        if (ObjectUtils.isNotEmpty(categoryDtos)) {
            for (CategoryDto categoryDto: categoryDtos) {
                TierMenuTreeItem childItem = TierMenuTreeItem.builder()
                        .id(UUID.randomUUID().toString())
                        .tierId(tierDto.getId())
                        .name(categoryDto.getName())
                        .categoryId(categoryDto.getId())
                        .categoryName(categoryDto.getName())
                        .treeLevel(TreeLevel.CHILD)
                        .build();
                tierMenuTreeItemTreeData.addItem(tierMenuTreeItem, childItem);
            }
        }
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
        checkbox.addValueChangeListener(event -> tierMenuTreeItem.setActive(event.getValue()));

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
//        buttonDeletes[tierMenuTreeItem.getRootIndex()].addClickListener(new TierServiceDeleteEventListener(this,
//                Integer.valueOf(tierMenuTreeItem.getId()),
//                restClientOrganizationService));
        return buttonDeletes[tierMenuTreeItem.getRootIndex()];
    }

    private Button applyButtonEdit(TierMenuTreeItem tierMenuTreeItem) {
        buttonEdits[tierMenuTreeItem.getRootIndex()] = new Button("Edit Name");

        buttonEdits[tierMenuTreeItem.getRootIndex()]
                .addClickListener(buttonClickEvent -> editTier(getTierDto(tierMenuTreeItem), FormAction.EDIT));
        return buttonEdits[tierMenuTreeItem.getRootIndex()];
    }

    private Button applyButtonUpdate(TierMenuTreeItem tierMenuTreeItem) {
        buttonUpdates[tierMenuTreeItem.getRootIndex()] = new Button("Update");
        buttonUpdates[tierMenuTreeItem.getRootIndex()].setEnabled(false);

//        buttonUpdates[tierMenuTreeItem.getRootIndex()].addClickListener(new TierSubServiceUpdateEventListener(tierForm,
//                restClientOrganizationService, getTierDto(tierMenuTreeItem),
//                tierMenuTreeGrid, tierMenuTreeItem));

        return buttonUpdates[tierMenuTreeItem.getRootIndex()];
    }

    private static TierDto getTierDto(TierMenuTreeItem tierMenuTreeItem) {
        TierDto tierDto = new TierDto();
        tierDto.setId(tierMenuTreeItem.getTierId());
        tierDto.setType(TierTypeDto.MENU);
        tierDto.setName(tierMenuTreeItem.getName());
        return tierDto;
    }

    private void operationFinished(List<TierDto> result) {
        buttonUpdates = new Button[result.size()];
        buttonEdits = new Button[result.size()];
        buttonDeletes = new Button[result.size()];
        populateGridValues(result);

    }

    private void populateGridValues(List<TierDto> result) {
        TreeData<TierMenuTreeItem> tierMenuTreeItemTreeData = new TreeData<>();
        AtomicInteger rootIndex = new AtomicInteger();

        result.forEach(tierMenuDto -> {
            TierMenuTreeItem tierMenuTreeItem = TierMenuTreeItem.builder()
                    .rootIndex(rootIndex.getAndIncrement())
                    .tierId(tierMenuDto.getId())
                    .id(UUID.randomUUID().toString())
                    .name(tierMenuDto.getName())
                    .treeLevel(TreeLevel.ROOT)
                    .build();
            tierMenuTreeItemTreeData.addItem(null, tierMenuTreeItem);
            extractedCategoryName(tierMenuTreeItemTreeData, tierMenuTreeItem, tierMenuDto);
        });

        ui.access(() -> {
            tierMenuTreeGrid.getTreeData().clear();
            tierMenuTreeGrid.setTreeData(tierMenuTreeItemTreeData);
        });

    }
}
