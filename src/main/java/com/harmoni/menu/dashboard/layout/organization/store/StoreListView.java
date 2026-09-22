package com.harmoni.menu.dashboard.layout.organization.store;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.dto.TierTypeDto;
import com.harmoni.menu.dashboard.event.store.StoreDeleteEventListener;
import com.harmoni.menu.dashboard.layout.AbstractListView;
import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.GridSkeleton;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Vaadin grid view listing the stores of the current user's chain with
 * pagination. Preloads the chains and tier lists (price, menu, service) into
 * {@code objectParams} for the {@link StoreForm}, opens add/edit forms in tabs
 * via {@link TabManager}, and refreshes on BROADCAST insert/update.
 */
@RequiredArgsConstructor
@Route(value = "store-list", layout = MainLayout.class)
@PageTitle("Store | POSHarmoni")
@Slf4j
public class StoreListView extends AbstractListView {

    static final String LIST_CHAIN = "LIST_CHAIN";
    static final String LIST_TIER_PRICE = "LIST_TIER_PRICE";
    static final String LIST_TIER_MENU = "LIST_TIER_MENU";
    static final String LIST_TIER_SERVICE = "LIST_TIER_SERVICE";

    private final Grid<StoreDto> storeDtoGrid = new Grid<>(StoreDto.class);
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final AccessService accessService;

    final transient Map<String, Object> objectParams = new HashMap<>();
    private final GridSkeleton gridSkeleton = new GridSkeleton(10);

    private void renderLayout() {
        setSizeFull();
        setPadding(false);
        configureGrid();
        add(getContent(), getPaginationFooter());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        refreshOnBroadcast(Set.of(BroadcastMessage.STORE_INSERT_SUCCESS,
                BroadcastMessage.STORE_UPDATED_SUCCESS), this::fetchStores);
        renderLayout();
        fetchStores();
        fetchChains();
        fetchTierPrices();
        fetchTierMenus();
        fetchTierServices();
    }

    private void configureGrid() {
        storeDtoGrid.setSizeFull();
        storeDtoGrid.removeAllColumns();
        storeDtoGrid.setEmptyStateText("No stores yet \u2014 click \u201CNew Store\u201D to add one.");
        storeDtoGrid.addColumn(StoreDto::getName).setHeader("Name");
        storeDtoGrid.addColumn(StoreDto::getAddress).setHeader("Address");
        storeDtoGrid.addColumn("chainDto.name").setHeader("Chain");
        storeDtoGrid.addComponentColumn(this::applyGroupButton).setHeader("Action");
        storeDtoGrid.getColumns().forEach(storeDtoColumn -> storeDtoColumn.setAutoWidth(true));
    }

    private Component applyGroupButton(StoreDto storeDto) {
        HorizontalLayout horizontalLayout = new HorizontalLayout();
        horizontalLayout.add(UiUtil.editButton(event -> showAddEditStore(storeDto,
                "Edit ".concat(storeDto.getName()), FormAction.EDIT)));
        horizontalLayout.add(UiUtil.deleteButton(
                new StoreDeleteEventListener(storeDto, this.restClientOrganizationService)));
        return horizontalLayout;
    }

    private void showAddEditStore(StoreDto storeDto, String title, FormAction action) {
        storeDtoGrid.asSingleSelect().clear();
        if (!(getParent().orElseThrow() instanceof TabSheet tabSheet)) {
            return;
        }
        new TabManager(tabSheet).addOrSelect(title, tab ->
                new StoreForm(this.restClientOrganizationService, tab, action, storeDto, objectParams));
    }

    private HorizontalLayout getContent() {
        return gridSlot(storeDtoGrid, gridSkeleton);
    }

    /**
     * Builds the toolbar with a lazy name filter and a "New Store" button.
     *
     * @return the toolbar layout to place above the grid
     */
    public HorizontalLayout getToolbarComponent() {
        configureSearchFilter();
        filterText.addValueChangeListener(changeEvent -> {
            if (!changeEvent.getOldValue().equals(changeEvent.getValue())) {
                currentPage = 1;
                fetchStores();
            }
        });

        Button addChainButton = UiUtil.addButton("New Store", event -> showAddEditStore(null, "New Store", FormAction.CREATE));
        HorizontalLayout toolbar = new HorizontalLayout(filterText, addChainButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private HorizontalLayout getPaginationFooter() {
        return paginationFooter(() -> {
            if (currentPage > 1) {
                currentPage--;
                fetchStores();
            }
        }, () -> {
            if (currentPage < totalPages) {
                currentPage++;
                fetchStores();
            }
        });
    }

    private void fetchStores() {
        int pageSize = 10;
        gridSkeleton.show();
        asyncRestClientOrganizationService.getAllStoreAsync(result -> UiUtil.safeAccess(ui, () -> {
            gridSkeleton.hide();
            if (ObjectUtils.isNotEmpty(result.get("data"))
                    && result.get("data") instanceof List<?> dataList && !dataList.isEmpty()) {
                totalPages = Integer.parseInt(result.get("page") == null ? "0" :result.get("page").toString());

                List<StoreDto> storeDtos = new ArrayList<>();
                dataList.forEach(object -> {
                    StoreDto storeDto = ObjectUtil.convertValueToObject(object, StoreDto.class);
                    storeDtos.add(storeDto);
                });

                storeDtoGrid.setItems(storeDtos);
                updatePagination();
            } else {
                storeDtoGrid.setItems(new ArrayList<>());
                totalPages = 0;
                updatePagination();
            }
        }), error -> UiUtil.safeAccess(ui, () -> {
            gridSkeleton.hide();
            UiUtil.errorWithRetry("Couldn't load stores", this::fetchStores);
        }), accessService.getUserDetail().getStoreDto().getChainDto().getId(), currentPage, pageSize, filterText.getValue());
    }

    private void fetchChains() {
        asyncRestClientOrganizationService.getAllChainByBrandIdAsync(result -> objectParams.put(LIST_CHAIN, result),
                accessService.getUserDetail().getStoreDto().getChainDto().getBrandId());
    }

    private void fetchTierPrices() {
        asyncRestClientOrganizationService.getAllTierByBrandAsync(result -> objectParams.put(LIST_TIER_PRICE, result),
                accessService.getUserDetail().getStoreDto().getChainDto().getBrandId(), TierTypeDto.PRICE);
    }

    private void fetchTierMenus() {
        asyncRestClientOrganizationService.getAllTierByBrandAsync(result -> objectParams.put(LIST_TIER_MENU, result),
                accessService.getUserDetail().getStoreDto().getChainDto().getBrandId(), TierTypeDto.MENU);
    }

    private void fetchTierServices() {
        asyncRestClientOrganizationService.getAllTierByBrandAsync(result -> objectParams.put(LIST_TIER_SERVICE, result),
                accessService.getUserDetail().getStoreDto().getChainDto().getBrandId(), TierTypeDto.SERVICE);
    }
}
