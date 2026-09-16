package com.harmoni.menu.dashboard.layout.organization.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.dto.TierTypeDto;
import com.harmoni.menu.dashboard.event.store.StoreDeleteEventListener;
import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.LoadingBar;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.icon.VaadinIcon;
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
    private final AccessService accessService;

    TextField filterText = new TextField();
    Text pageInfoText;

    UI ui;
    int totalPages;
    int currentPage = 1;
    final transient Map<String, Object> objectParams = new HashMap<>();
    private final LoadingBar loadingBar = new LoadingBar();

    private void renderLayout() {
        setSizeFull();
        setPadding(false);
        configureGrid();
        add(loadingBar, getContent(), getPaginationFooter());
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
        storeDtoGrid.setEmptyStateText(UiUtil.NO_RECORDS);
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
        HorizontalLayout content = new HorizontalLayout(storeDtoGrid);
        content.setFlexGrow(1, storeDtoGrid);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
    }

    public HorizontalLayout getToolbarComponent() {
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setPrefixComponent(VaadinIcon.SEARCH.create());
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
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
        HorizontalLayout paginationFooter = new HorizontalLayout();
        paginationFooter.addClassName("pagination");
        Button previousButton = new Button("Previous", event -> {
            if (currentPage > 1) {
                currentPage--;
                fetchStores();
            }
        });
        Button nextButton = new Button("Next", event -> {
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
        loadingBar.start();
        asyncRestClientOrganizationService.getAllStoreAsync(result -> ui.access(() -> {
            loadingBar.stop();
            if (ObjectUtils.isNotEmpty(result.get("data"))
                    && result.get("data") instanceof List<?> dataList && !dataList.isEmpty()) {
                totalPages = Integer.parseInt(result.get("page") == null ? "0" :result.get("page").toString());

                List<StoreDto> storeDtos = new ArrayList<>();
                dataList.forEach(object -> {
                    StoreDto storeDto = ObjectUtil.convertValueToObject(object, StoreDto.class);
                    storeDtos.add(storeDto);
                });

                storeDtoGrid.setItems(storeDtos);
                pageInfoText.setText(getPaginationInfo());
            } else {
                storeDtoGrid.setItems(new ArrayList<>());
                totalPages = 0;
                pageInfoText.setText(getPaginationInfo());
            }
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
