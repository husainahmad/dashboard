package com.harmoni.menu.dashboard.event.tier;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.SubServiceDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.dto.TierSubServiceDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.exception.BrandHandler;
import com.harmoni.menu.dashboard.layout.organization.tier.service.TierServiceTreeItem;
import com.harmoni.menu.dashboard.rest.data.RestAPIResponse;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import com.vaadin.flow.data.provider.hierarchy.TreeDataProvider;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
public class TierMenuUpdateEventListener implements ComponentEventListener<ClickEvent<Button>>,
        BroadcastMessageService {

    private final UI ui;
    private final RestClientOrganizationService restClientOrganizationService;
    private final TreeGrid<TierServiceTreeItem> treeItemTreeGrid;
    private final List<TierSubServiceDto> tierServiceDtos = new ArrayList<>();
    private final transient TierServiceTreeItem tierServiceTreeItem;
    private final transient TierDto tierDto;

    private void extractedPayload(TierServiceTreeItem tierServiceTreeItem) {
        TreeDataProvider<TierServiceTreeItem> dataProvider = (TreeDataProvider<TierServiceTreeItem>)
                this.treeItemTreeGrid.getDataProvider();
        TreeData<TierServiceTreeItem> treeItemTreeData =  dataProvider.getTreeData();

        treeItemTreeData.getChildren(tierServiceTreeItem).forEach(serviceTreeItem ->
            treeItemTreeData.getChildren(serviceTreeItem).forEach(lastServiceTreeItem -> {
                SubServiceDto subServiceDto = new SubServiceDto();
                subServiceDto.setId(lastServiceTreeItem.getSubServiceId());

                TierSubServiceDto tierServiceDto = new TierSubServiceDto();
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
                .doOnError(error -> new BrandHandler(this.ui,
                        "Error while updating Tier ".concat(error.getMessage())))
                .subscribe(this::accept);
    }

    private void accept(RestAPIResponse restAPIResponse) {
        broadcastMessage(BroadcastMessage.TIER_UPDATED_SUCCESS, restAPIResponse);
    }
}
