package com.harmoni.menu.dashboard.event.tier;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.*;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.exception.BrandHandler;
import com.harmoni.menu.dashboard.layout.organization.tier.menu.TierMenuTreeItem;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
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

/**
 * Called when the user clicks Update on the tier menu tree grid: reads the
 * selected/active categories from the tree, posts the tier-menu mappings and
 * broadcasts the result.
 */
@RequiredArgsConstructor
public class TierMenuUpdateEventListener implements ComponentEventListener<ClickEvent<Button>>,
        BroadcastMessageService {

    private final UI ui;
    private final RestClientOrganizationService restClientOrganizationService;
    private final TreeGrid<TierMenuTreeItem> treeItemTreeGrid;
    private final transient TierMenuTreeItem tierMenuTreeItem;
    private final transient TierDto tierDto;

    private List<TierMenuDto> extractedPayload(TierMenuTreeItem tierMenuTreeItem) {
        TreeDataProvider<TierMenuTreeItem> dataProvider = (TreeDataProvider<TierMenuTreeItem>)
                this.treeItemTreeGrid.getDataProvider();
        TreeData<TierMenuTreeItem> treeItemTreeData =  dataProvider.getTreeData();

        List<TierMenuDto> tierMenuDtos = new ArrayList<>();

        treeItemTreeData.getChildren(tierMenuTreeItem).forEach(menuTreeItem ->
                tierMenuDtos.add(extractedChild(menuTreeItem)));
        return tierMenuDtos;
    }

    private TierMenuDto extractedChild(TierMenuTreeItem menuTreeItem) {
        CategoryDto categoryDto = new CategoryDto();
        categoryDto.setId(menuTreeItem.getCategoryDto().getId());
        categoryDto.setBrandId(this.tierDto.getBrandId());
        TierMenuDto tierServiceDto = new TierMenuDto();
        tierServiceDto.setTierDto(this.tierDto);

        tierServiceDto.setCategoryDto(categoryDto);
        tierServiceDto.setActive(menuTreeItem.isActive());
        return tierServiceDto;
    }

    /**
     * Extracts the tier-menu payload from the tree grid and posts it via the
     * REST API, broadcasting the result on success.
     *
     * @param buttonClickEvent the click event that triggered the listener
     */
    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        execute(() -> {
        });
    }

    /**
     * Posts the tier-menu payload read from the tree grid, rolling the given
     * callback back on failure. Used to save immediately from the row toggles
     * without a dedicated Update button.
     *
     * @param rollback the action to run when the update fails (e.g. reverting
     *                 the toggled checkbox)
     */
    public void execute(Runnable rollback) {
        restClientOrganizationService.updateTierMenu(this.tierDto, extractedPayload(this.tierMenuTreeItem))
                .doOnError(error -> {
                    new BrandHandler(this.ui, "Error while updating Tier ".concat(error.getMessage()));
                    rollback.run();
                })
                .subscribe(this::accept);
    }

    private void accept(RestAPIResponse restAPIResponse) {
        broadcastMessage(BroadcastMessage.TIER_UPDATED_SUCCESS, restAPIResponse);
    }
}
