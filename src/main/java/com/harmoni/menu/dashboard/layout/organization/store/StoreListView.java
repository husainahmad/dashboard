package com.harmoni.menu.dashboard.layout.organization.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.dto.TierTypeDto;
import com.harmoni.menu.dashboard.event.store.StoreDeleteEventListener;
import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Route(value = "store-list", layout = MainLayout.class)
@PageTitle("Store | POSHarmoni")
@Slf4j
public class StoreListView extends VerticalLayout {

    static final String LIST_CHAIN = "LIST_CHAIN";
    static final String LIST_TIER_PRICE = "LIST_TIER_PRICE";
    static final String LIST_TIER_MENU = "LIST_TIER_MENU";
    static final String LIST_TIER_SERVICE = "LIST_TIER_SERVICE";

    Registration broadcasterRegistration;
    private final Grid<StoreDto> storeDtoGrid = new Grid<>(StoreDto.class);
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;

    TextField filterText = new TextField();
    Text pageInfoText;

    UI ui;
    int totalPages;
    int currentPage = 1;
    final transient Map<String, Object> objectParams = new HashMap<>();
    static final int TEMP_BRAND_ID = 1;

    private void renderLayout() {
        addClassName("list-view");
        setSizeFull();
        configureGrid();
        add(getToolbar(), getContent(), getPaginationFooter());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(message -> {
            try {
                BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
                if (ObjectUtils.isNotEmpty(broadcastMessage) && ObjectUtils.isNotEmpty(broadcastMessage.getType())
                        && (broadcastMessage.getType().equals(BroadcastMessage.STORE_INSERT_SUCCESS) ||
                    broadcastMessage.getType().equals(BroadcastMessage.STORE_UPDATED_SUCCESS))) {
                        fetchStores();
                    }

            } catch (JsonProcessingException e) {
                log.error("Broadcast Handler Error", e);
            }
        });
        renderLayout();
        fetchStores();
        fetchChains();
        fetchTierPrices();
        fetchTierMenus();
        fetchTierServices();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    private void configureGrid() {
        storeDtoGrid.setSizeFull();
        storeDtoGrid.removeAllColumns();
        storeDtoGrid.addColumn(StoreDto::getName).setHeader("Name");
        storeDtoGrid.addColumn(StoreDto::getAddress).setHeader("Address");
        storeDtoGrid.addColumn("chainDto.name").setHeader("Chain");
        storeDtoGrid.addComponentColumn(this::applyGroupButton).setHeader("Action");
        storeDtoGrid.getColumns().forEach(storeDtoColumn -> storeDtoColumn.setAutoWidth(true));
    }

    private Component applyGroupButton(StoreDto storeDto) {
        HorizontalLayout horizontalLayout = new HorizontalLayout();

        Button editButton = new Button("Edit");
        editButton.addClickListener(_ -> showAddEditStore(storeDto, "Edit Store", FormAction.EDIT));

        horizontalLayout.add(editButton);

        Button deleteButton = new Button("Delete");
        deleteButton.addClickListener(new StoreDeleteEventListener(storeDto, this.restClientOrganizationService));
        horizontalLayout.add(deleteButton);

        return horizontalLayout;
    }

    private void showAddEditStore(StoreDto storeDto, String title, FormAction action) {
        if (!(this.getParent().orElseThrow() instanceof TabSheet tabSheet)) {
            return;
        }
        Tab tabNewStore = new Tab();
        tabNewStore.setLabel(title);
        tabSheet.add(tabNewStore, new StoreForm(this.asyncRestClientOrganizationService,
                this.restClientOrganizationService, tabNewStore, action, storeDto, objectParams));
        tabSheet.setSizeFull();
        tabSheet.setSelectedTab(tabNewStore);
    }

    private HorizontalLayout getContent() {
        HorizontalLayout content = new HorizontalLayout(storeDtoGrid);
        content.setFlexGrow(1, storeDtoGrid);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
    }

    private HorizontalLayout getToolbar() {
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(changeEvent -> {
            if (!changeEvent.getOldValue().equals(changeEvent.getValue())) {
                currentPage = 1;
                fetchStores();
            }
        });

        Button addChainButton = new Button("Add Store");
        addChainButton.addClickListener(_ -> showAddEditStore(null, "New Store", FormAction.CREATE));
        HorizontalLayout toolbar = new HorizontalLayout(filterText, addChainButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private HorizontalLayout getPaginationFooter() {
        HorizontalLayout paginationFooter = new HorizontalLayout();
        Button previousButton = new Button("Previous", _ -> {
            if (currentPage > 1) {
                currentPage--;
                fetchStores();
            }
        });
        Button nextButton = new Button("Next", _ -> {
            if (currentPage < totalPages) {
                currentPage++;
                fetchStores();
            }
        });
        pageInfoText = new Text(getPaginationInfo());
        paginationFooter.add(previousButton, pageInfoText, nextButton);
        paginationFooter.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        paginationFooter.setWidthFull();
        paginationFooter.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        return paginationFooter;
    }

    private String getPaginationInfo() {
        return "Page "
                .concat(String.valueOf(currentPage))
                .concat(" of ")
                .concat(String.valueOf(totalPages));
    }

    private void fetchStores() {
        int pageSize = 10;
        asyncRestClientOrganizationService.getAllStoreAsync(result -> {
            if (ObjectUtils.isNotEmpty(result.get("data"))
                    && result.get("data") instanceof List<?> dataList && !dataList.isEmpty()) {
                totalPages = Integer.parseInt(result.get("page") == null ? "0" :result.get("page").toString());

                List<StoreDto> storeDtos = new ArrayList<>();
                dataList.forEach(object -> {
                    StoreDto storeDto = ObjectUtil.convertValueToObject(object, StoreDto.class);
                    storeDtos.add(storeDto);
                });

                ui.access(()-> {
                    storeDtoGrid.setItems(storeDtos);
                    pageInfoText.setText(getPaginationInfo());
                });
            }
        }, 24L, currentPage, pageSize, filterText.getValue());
    }

    private void fetchChains() {
        asyncRestClientOrganizationService.getAllChainByBrandIdAsync(result -> objectParams.put(LIST_CHAIN, result), TEMP_BRAND_ID);
    }

    private void fetchTierPrices() {
        asyncRestClientOrganizationService.getAllTierByBrandAsync(result -> objectParams.put(LIST_TIER_PRICE, result), TEMP_BRAND_ID, TierTypeDto.PRICE);
    }

    private void fetchTierMenus() {
        asyncRestClientOrganizationService.getAllTierByBrandAsync(result -> objectParams.put(LIST_TIER_MENU, result), TEMP_BRAND_ID, TierTypeDto.MENU);
    }

    private void fetchTierServices() {
        asyncRestClientOrganizationService.getAllTierByBrandAsync(result -> objectParams.put(LIST_TIER_SERVICE, result), TEMP_BRAND_ID, TierTypeDto.SERVICE);
    }
}
