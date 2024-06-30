package com.harmoni.menu.dashboard.layout.organization.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.component.DialogClosing;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
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
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "store", layout = MainLayout.class)
@PageTitle("Store | POSHarmoni")
@Slf4j
public class StoreListView extends VerticalLayout {

    Registration broadcasterRegistration;
    private final Grid<StoreDto> storeDtoGrid = new Grid<>(StoreDto.class);
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final TextField filterText = new TextField();
    private UI ui;
    private final RestClientOrganizationService restClientOrganizationService;
    private StoreForm storeForm;
    public StoreListView(@Autowired AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                         @Autowired RestClientOrganizationService restClientOrganizationService) {
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
        this.restClientOrganizationService = restClientOrganizationService;
        addClassName("list-view");
        setSizeFull();
        configureGrid();
        configureForm();

        add(getToolbar(), getContent());
        closeEditor();

        fetchStores();
    }

    private void closeEditor() {
        storeForm.setVisible(false);
        removeClassName("editing");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(message -> {

            try {
                BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
                if (ObjectUtils.isNotEmpty(broadcastMessage) && ObjectUtils.isNotEmpty(broadcastMessage.getType())) {
                    if (broadcastMessage.getType().equals(BroadcastMessage.STORE_INSERT_SUCCESS)) {
                        fetchStores();
                        ui.access(()->{
                            storeForm.setVisible(false);
                            removeClassName("editing");
                        });
                    } else {
                        UiUtil.showErrorDialog(ui, this, message);
                    }
                }
            } catch (JsonProcessingException e) {
                log.error("Broadcast Handler Error", e);
            }
        });
    }

    private void showErrorDialog(String message) {
        DialogClosing dialog = new DialogClosing(message);
        ui.access(()-> {
            add(dialog);
            dialog.open();
        });
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
        storeDtoGrid.addColumn("brandDto.name").setHeader("Brand");
        storeDtoGrid.addColumn("tierDto.name").setHeader("Tier");

        storeDtoGrid.getColumns().forEach(storeDtoColumn -> storeDtoColumn.setAutoWidth(true));
        storeDtoGrid.asSingleSelect().addValueChangeListener(valueChangeEvent ->
                editStore(valueChangeEvent.getValue(), FormAction.EDIT));
    }

    private void editStore(StoreDto storeDto, FormAction formAction) {
        if (storeDto == null) {
            closeEditor();
        } else {
            storeForm.setStoreDto(storeDto);
            storeForm.restructureButton(formAction);
            storeForm.setVisible(true);
            addClassName("editing");
        }
    }

    private void configureForm() {
        storeForm = new StoreForm(this.asyncRestClientOrganizationService,
                                this.restClientOrganizationService);
        storeForm.setWidth("25em");
    }

    private HorizontalLayout getContent() {
        HorizontalLayout content = new HorizontalLayout(storeDtoGrid, storeForm);
        content.setFlexGrow(2, storeDtoGrid);
        content.setFlexGrow(1, storeForm);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
    }

    private HorizontalLayout getToolbar() {
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);

        Button addChainButton = new Button("Add Store");
        addChainButton.addClickListener(buttonClickEvent -> {
            addStore();
        });
        HorizontalLayout toolbar = new HorizontalLayout(filterText, addChainButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private void addStore() {
        storeDtoGrid.asSingleSelect().clear();
        editStore(new StoreDto(), FormAction.CREATE);
    }

    private void fetchStores() {
        asyncRestClientOrganizationService.getAllStoreAsync(result -> {
            ui.access(()-> {
                storeDtoGrid.setItems(result);
            });
        });
    }
}
