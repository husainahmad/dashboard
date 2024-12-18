package com.harmoni.menu.dashboard.event.tier;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.SubServiceDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.dto.TierServiceDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.exception.BrandHandler;
import com.harmoni.menu.dashboard.layout.organization.tier.TierForm;
import com.harmoni.menu.dashboard.layout.organization.tier.service.TierServiceTreeItem;
import com.harmoni.menu.dashboard.rest.data.RestAPIResponse;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;

import java.util.ArrayList;
import java.util.List;

public class TierSubServiceUpdateEventListener implements ComponentEventListener<ClickEvent<Button>>,
        BroadcastMessageService {

    private final TierForm tierForm;
    private final RestClientOrganizationService restClientOrganizationService;
    private final TreeGrid<TierServiceTreeItem> treeItemTreeGrid;
    private final List<TierServiceDto> tierServiceDtos = new ArrayList<>();
    private final transient TierServiceTreeItem tierServiceTreeItem;
    private final transient TierDto tierDto;
    public TierSubServiceUpdateEventListener(TierForm tierForm,
                                             RestClientOrganizationService restClientOrganizationService,
                                             TierDto tierDto,
                                             TreeGrid<TierServiceTreeItem> treeItemTreeGrid,
                                             TierServiceTreeItem tierServiceTreeItem) {
        this.tierForm = tierForm;
        this.restClientOrganizationService = restClientOrganizationService;
        this.treeItemTreeGrid = treeItemTreeGrid;
        this.tierServiceTreeItem = tierServiceTreeItem;
        this.tierDto = tierDto;
    }

    private void extractedPayload(TierServiceTreeItem tierServiceTreeItem) {
        TreeDataProvider<TierServiceTreeItem> dataProvider = (TreeDataProvider<TierServiceTreeItem>)
                this.treeItemTreeGrid.getDataProvider();
        TreeData<TierServiceTreeItem> treeItemTreeData =  dataProvider.getTreeData();

        treeItemTreeData.getChildren(tierServiceTreeItem).forEach(serviceTreeItem ->
            treeItemTreeData.getChildren(serviceTreeItem).forEach(lastServiceTreeItem -> {
                SubServiceDto subServiceDto = new SubServiceDto();
                subServiceDto.setId(lastServiceTreeItem.getSubServiceId());

                TierServiceDto tierServiceDto = new TierServiceDto();
                tierServiceDto.setTierDto(this.tierDto);
                tierServiceDto.setSubServiceDto(subServiceDto);
                tierServiceDto.setActive(lastServiceTreeItem.isActive());

                tierServiceDtos.add(tierServiceDto);
            }
        ));
    }

    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        extractedPayload(this.tierServiceTreeItem);

        restClientOrganizationService.updateTierService(this.tierDto, this.tierServiceDtos)
                .doOnError(error -> new BrandHandler(this.tierForm.getUi(),
                        "Error while updating Tier ".concat(error.getMessage())))
                .subscribe(this::accept);
    }

    private void accept(RestAPIResponse restAPIResponse) {
        broadcastMessage(BroadcastMessage.TIER_UPDATED_SUCCESS, restAPIResponse);
    }
}
