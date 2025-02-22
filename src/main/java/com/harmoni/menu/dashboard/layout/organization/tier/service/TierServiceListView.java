package com.harmoni.menu.dashboard.layout.organization.tier.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.*;
import com.harmoni.menu.dashboard.event.tier.TierServiceDeleteEventListener;
import com.harmoni.menu.dashboard.event.tier.TierSubServiceUpdateEventListener;
import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.*;
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
@Route(value = "tier-service", layout = MainLayout.class)
@PageTitle("Tier | POSHarmoni")
@Slf4j
public class TierServiceListView extends VerticalLayout {

    Registration broadcasterRegistration;
    private final TreeGrid<TierServiceTreeItem> tierServiceTreeGrid = new TreeGrid<>(TierServiceTreeItem.class);
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final TextField filterText = new TextField();
    @Getter
    private UI ui;
    private final RestClientOrganizationService restClientOrganizationService;
    private TierServiceForm tierForm;
    private transient List<ServiceDto> serviceDtos = new ArrayList<>();
    private transient List<BrandDto> brandDtos;
    @Getter
    @Setter
    private transient BrandDto brandDto = new BrandDto();
    private static final Integer TEMP_BRAND_ID = 1;
    private Button[] buttonUpdates;
    private Button[] buttonEdits;
    private Button[] buttonDeletes;
    private final Map<String, Checkbox> checkBoxes = new HashMap<>();

    private void renderLayout() {
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
                            broadcastMessage.getType().equals(BroadcastMessage.TIER_UPDATED_SUCCESS) ||
                        broadcastMessage.getType().equals(BroadcastMessage.TIER_DELETED_SUCCESS))) {
                        fetchTier();
                    }

            } catch (JsonProcessingException e) {
                log.error("Broadcast Handler Error", e);
            }
        });
        renderLayout();
        fetchBrands();
        fetchService();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    private void configureGrid() {
        tierServiceTreeGrid.setSizeFull();
        tierServiceTreeGrid.removeAllColumns();

        tierServiceTreeGrid.addHierarchyColumn(TierServiceTreeItem::getName).setHeader("Tier Name");
        tierServiceTreeGrid.addComponentColumn(this::applyCheckbox).setHeader("Selected");
        tierServiceTreeGrid.addComponentColumn(this::applyButton).setHeader("Action");
        tierServiceTreeGrid.getColumns().forEach(productDtoColumn -> productDtoColumn.setAutoWidth(true));
        tierServiceTreeGrid.addExpandListener(expandEvent -> {
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
        tierForm = new TierServiceForm(this.restClientOrganizationService, this.asyncRestClientOrganizationService);
        tierForm.setWidth("25em");
    }

    private HorizontalLayout getContent() {
        HorizontalLayout content = new HorizontalLayout(tierServiceTreeGrid, tierForm);
        content.setFlexGrow(2, tierServiceTreeGrid);
        content.setFlexGrow(1, tierForm);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
    }

    private HorizontalLayout getToolbar() {
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);

        Button addTierServiceButton = new Button("Add Tier Service");
        addTierServiceButton.addClickListener(_ -> addTier());

        HorizontalLayout toolbar = new HorizontalLayout(filterText, addTierServiceButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private void addTier() {
        TierDto tierDto = new TierDto();
        tierDto.setType(TierTypeDto.SERVICE);
        editTier(tierDto, FormAction.CREATE);
    }

    private void fetchTier() {
        asyncRestClientOrganizationService.getTierServiceByBrandAsync(this::operationFinished, brandDto.getId());
    }

    private void fetchService() {
        asyncRestClientOrganizationService.getAllServicesAsync(result -> {
            serviceDtos = result;
            fetchTier();
        });
    }

    private void fetchBrands() {
        asyncRestClientOrganizationService.getAllBrandAsync(result -> brandDtos = result);
    }

    private Checkbox applyCheckbox(TierServiceTreeItem tierServiceTreeItem) {
        if (tierServiceTreeItem.getTreeLevel().equals(TreeLevel.ROOT)
                || tierServiceTreeItem.getTreeLevel().equals(TreeLevel.PARENT)) {
            return null;
        }

        Checkbox checkbox = new Checkbox(tierServiceTreeItem.isActive());
        checkbox.addValueChangeListener(event -> {
            if (ObjectUtils.isNotEmpty(tierServiceTreeItem.getTierServiceTreeItemParent()) &&
                    ObjectUtils.isNotEmpty(tierServiceTreeItem.getTierServiceTreeItemParent().getTierServiceTreeItemParent())) {
                buttonUpdates[tierServiceTreeItem.getTierServiceTreeItemParent()
                        .getTierServiceTreeItemParent().getRootIndex()].setEnabled(true);
            }
            tierServiceTreeItem.setActive(event.getValue());
        });

        checkBoxes.put(tierServiceTreeItem.getId(), checkbox);

        return checkbox;
    }

    private Component applyButton(TierServiceTreeItem tierServiceTreeItem) {
        if (tierServiceTreeItem.getTreeLevel().equals(TreeLevel.ROOT)) {
            HorizontalLayout layout = new HorizontalLayout();
            layout.add(applyButtonUpdate(tierServiceTreeItem));
            layout.add(applyButtonEdit(tierServiceTreeItem));
            layout.add(applyButtonDelete(tierServiceTreeItem));
            return layout;
        }
        return null;
    }

    private Button applyButtonDelete(TierServiceTreeItem tierServiceTreeItem) {
        buttonDeletes[tierServiceTreeItem.getRootIndex()] = new Button("Delete");
        buttonDeletes[tierServiceTreeItem.getRootIndex()].addClickListener(new TierServiceDeleteEventListener(
                Integer.valueOf(tierServiceTreeItem.getId()),
                restClientOrganizationService, this.getUi()));
        return buttonDeletes[tierServiceTreeItem.getRootIndex()];
    }

    private Button applyButtonEdit(TierServiceTreeItem tierServiceTreeItem) {
        buttonEdits[tierServiceTreeItem.getRootIndex()] = new Button("Edit Name");

        buttonEdits[tierServiceTreeItem.getRootIndex()]
                .addClickListener(_ -> editTier(getTierDto(tierServiceTreeItem), FormAction.EDIT));
        return buttonEdits[tierServiceTreeItem.getRootIndex()];
    }

    private Button applyButtonUpdate(TierServiceTreeItem tierServiceTreeItem) {
        buttonUpdates[tierServiceTreeItem.getRootIndex()] = new Button("Update");
        buttonUpdates[tierServiceTreeItem.getRootIndex()].setEnabled(false);

        buttonUpdates[tierServiceTreeItem.getRootIndex()].addClickListener(new TierSubServiceUpdateEventListener(tierForm.getUi(),
                restClientOrganizationService, tierServiceTreeGrid, tierServiceTreeItem, getTierDto(tierServiceTreeItem)));

        return buttonUpdates[tierServiceTreeItem.getRootIndex()];
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

        buttonUpdates = new Button[tierGroup.size()];
        buttonEdits = new Button[tierGroup.size()];
        buttonDeletes = new Button[tierGroup.size()];

        AtomicInteger rootIndex = new AtomicInteger();

        tierGroup.forEach((tierDto, tierServiceDtos) -> {
            TierServiceTreeItem tierServiceTreeItem = getTreeItem(rootIndex.getAndIncrement(),
                    tierDto.getId().toString(), tierDto.getName(), null, null, false, TreeLevel.ROOT);

            tierServiceTreeItemTreeData.addItem(null, tierServiceTreeItem);
            extractedServiceName(tierServiceTreeItemTreeData, tierServiceTreeItem, tierServiceDtos);
        });

        ui.access(() -> tierServiceTreeGrid.setTreeData(tierServiceTreeItemTreeData));
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
