package com.harmoni.menu.dashboard.layout.organization.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
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

@RequiredArgsConstructor
@Route(value = "store-list", layout = MainLayout.class)
@PageTitle("Store | POSHarmoni")
@Slf4j
public class StoreListView extends VerticalLayout {

    private static final String EDITING = "editing";
    Registration broadcasterRegistration;
    private final Grid<StoreDto> storeDtoGrid = new Grid<>(StoreDto.class);
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;

    private final TextField filterText = new TextField();
    private UI ui;

    private static Button applyButtonEdit(StoreDto storeDto) {
        return new Button("Edit");
    }

    private void renderLayout() {
        addClassName("list-view");
        setSizeFull();
        configureGrid();
        add(getToolbar(), getContent());
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
        storeDtoGrid.addColumn("tierDto.name").setHeader("Tier");

        storeDtoGrid.getColumns().forEach(storeDtoColumn -> storeDtoColumn.setAutoWidth(true));
        storeDtoGrid.addComponentColumn(StoreListView::applyButtonEdit);
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

        Button addChainButton = new Button("Add Store");
        addChainButton.addClickListener(_ -> addStore());
        HorizontalLayout toolbar = new HorizontalLayout(filterText, addChainButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private void addStore() {
        storeDtoGrid.asSingleSelect().clear();

        if (!(this.getParent().orElseThrow() instanceof TabSheet tabSheet)) {
            return;
        }
        Tab tabNewStore = new Tab();
        tabNewStore.setLabel("New Store");
        tabSheet.add(tabNewStore, new StoreForm(this.asyncRestClientOrganizationService,
                this.restClientOrganizationService, tabNewStore));
        tabSheet.setSizeFull();
        tabSheet.setSelectedTab(tabNewStore);
    }

    private void fetchStores() {
        asyncRestClientOrganizationService.getAllStoreAsync(result -> ui.access(()->
                storeDtoGrid.setItems(result)));
    }
}
