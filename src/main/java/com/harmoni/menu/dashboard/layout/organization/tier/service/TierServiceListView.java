package com.harmoni.menu.dashboard.layout.organization.tier.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ServiceDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.dto.TierTypeDto;
import com.harmoni.menu.dashboard.event.tier.TierServiceDeleteEventListener;
import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
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
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

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
    private BrandDto brandDto = new BrandDto();

    private final Integer TEMP_BRAND_ID = 1;

    public TierServiceListView(@Autowired AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                               @Autowired RestClientOrganizationService restClientOrganizationService) {
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
        this.restClientOrganizationService = restClientOrganizationService;

        brandDto.setId(TEMP_BRAND_ID);
        addClassName("list-view");
        setSizeFull();
        configureGrid();

        configureForm();
        add(getToolbar(), getContent());

        fetchBrands();
        fetchTier();
        fetchService();

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
                if (org.apache.commons.lang3.ObjectUtils.isNotEmpty(broadcastMessage)
                        && org.apache.commons.lang3.ObjectUtils.isNotEmpty(broadcastMessage.getType())
                        && (broadcastMessage.getType().equals(BroadcastMessage.TIER_INSERT_SUCCESS) ||
                            broadcastMessage.getType().equals(BroadcastMessage.TIER_UPDATED_SUCCESS))) {
                        fetchTier();
                    }

            } catch (JsonProcessingException e) {
                log.error("Broadcast Handler Error", e);
            }
        });
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

        tierServiceTreeGrid.addCollapseListener(event -> event.getItems().forEach(tierServiceTreeItem ->
                log.debug("item collapse {}", tierServiceTreeItem)));

        tierServiceTreeGrid.addExpandListener(event -> {
            if (event.isFromClient()) {
                event.getItems().forEach(serviceTreeItem -> {
                    log.debug("item expand {}", serviceTreeItem);

                    if (serviceTreeItem.getTreeLevel().equals(TreeLevel.PARENT)) {

                    }
                });
            }
        });

        tierServiceTreeGrid.addComponentColumn(this::applyCheckbox).setHeader("Selected");
        tierServiceTreeGrid.addComponentColumn(this::applyButton).setHeader("Action");
        tierServiceTreeGrid.getColumns().forEach(productDtoColumn -> productDtoColumn.setAutoWidth(true));
    }

    private void editTier(TierDto tierDto) {
        tierForm.setBrandDtos(brandDtos);
        tierForm.getBrandBox().setItems(brandDtos);

        if (tierDto == null) {
            closeEditor();
        } else {
            tierDto.setBrandId(brandDto.getId());
            tierForm.setTierDto(tierDto);
            tierForm.restructureButton(FormAction.CREATE);
            tierForm.setVisible(true);
            addClassName("editing");
        }
    }

    private void configureForm() {
        tierForm = new TierServiceForm(this.asyncRestClientOrganizationService,
                                this.restClientOrganizationService);
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
        addTierServiceButton.addClickListener(e -> addTier());
        HorizontalLayout toolbar = new HorizontalLayout(filterText, addTierServiceButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private void addTier() {
        TierDto tierDto = new TierDto();
        tierDto.setType(TierTypeDto.SERVICE);
        editTier(tierDto);
    }

    private void fetchTier() {
        asyncRestClientOrganizationService.getAllTierByBrandAsync(result -> {
            TreeData<TierServiceTreeItem> tierServiceTreeItemTreeData = new TreeData<>();

            result.forEach(tierServiceDto -> {
                TierServiceTreeItem tierServiceTreeItem = TierServiceTreeItem.builder()
                        .id(tierServiceDto.getId().toString())
                        .name(tierServiceDto.getName())
                        .subServiceName("")
                        .serviceName("")
                        .treeLevel(TreeLevel.ROOT)
                        .build();
                tierServiceTreeItemTreeData.addItem(null, tierServiceTreeItem);
                extractedServiceName(tierServiceTreeItemTreeData, tierServiceTreeItem);
            });

            ui.access(() -> tierServiceTreeGrid.setTreeData(tierServiceTreeItemTreeData));

        }, brandDto.getId(), TierTypeDto.SERVICE);
    }

    private void extractedServiceName(TreeData<TierServiceTreeItem> tierServiceTreeItemTreeData,
                                      TierServiceTreeItem tierServiceTreeItem) {

        serviceDtos.forEach(serviceDto -> {

            TierServiceTreeItem tServiceTreeItem = TierServiceTreeItem.builder()
                    .id(tierServiceTreeItem.getId().concat("-").concat(String.valueOf(serviceDto.getId())))
                    .name(serviceDto.getName())
                    .treeLevel(TreeLevel.PARENT)
                    .tierServiceTreeItemParent(tierServiceTreeItem)
                    .build();

            tierServiceTreeItemTreeData.addItem(tierServiceTreeItem, tServiceTreeItem);

            serviceDto.getSubServices().forEach(subServiceDto -> {
                TierServiceTreeItem tSubServiceTreeItem = TierServiceTreeItem.builder()
                        .id(tierServiceTreeItem.getId().concat("-").concat(String.valueOf(serviceDto.getId()))
                                .concat("-").concat(String.valueOf(subServiceDto.getId())))
                        .name(subServiceDto.getName())
                        .tierServiceTreeItemParent(tServiceTreeItem)
                        .treeLevel(TreeLevel.CHILD)
                        .build();
                tierServiceTreeItemTreeData.addItem(tServiceTreeItem, tSubServiceTreeItem);
            });

        });

    }

    private void fetchService() {
        asyncRestClientOrganizationService.getAllServicesAsync(result -> serviceDtos = result);
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
        checkbox.addValueChangeListener(event -> tierServiceTreeItem.setActive(event.getValue()));

        return checkbox;
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
        Button button = new Button("Delete");
        button.addClickListener(new TierServiceDeleteEventListener(this,
                Integer.valueOf(tierServiceTreeItem.getId()),
                restClientOrganizationService));
        return button;
    }

    private Button applyButtonEdit(TierServiceTreeItem tierServiceTreeItem) {
        Button button = new Button("Edit");
        TierDto tierDto = new TierDto();
        tierDto.setId(Integer.parseInt(tierServiceTreeItem.getId()));
        tierDto.setName(tierServiceTreeItem.getName());
        button.addClickListener(buttonClickEvent -> editTier(tierDto));
        return button;
    }
}
