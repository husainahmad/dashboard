package com.harmoni.menu.dashboard.layout.setting.table;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.dto.TableDto;
import com.harmoni.menu.dashboard.event.table.TableDeleteEventListener;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.LoadingBar;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientSettingService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientSettingService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.Registration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * List of tables shown inside the table tab sheet: a {@link Grid} with name,
 * capacity, store and action columns, a filterable toolbar and a "New Table"
 * button. Refreshes on {@link Broadcaster} messages and scopes the fetch to
 * the current store when one is available.
 */
@RequiredArgsConstructor
@Slf4j
public class TableListView extends VerticalLayout {

    Registration broadcasterRegistration;

    private final Grid<TableDto> tableDtoGrid = new Grid<>(TableDto.class);

    private UI ui;

    private final TextField filterText = new TextField();
    private final AsyncRestClientSettingService asyncRestClientSettingService;
    private final RestClientSettingService restClientSettingService;
    private final AccessService accessService;
    private final LoadingBar loadingBar = new LoadingBar();

    private void renderLayout() {
        setSizeFull();
        setPadding(false);
        configureGrid();

        add(loadingBar, getContent());
        fetchTables();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(message -> {
            try {
                BroadcastMessage broadcastMessage =
                        (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
                if (ObjectUtils.isNotEmpty(broadcastMessage) && ObjectUtils.isNotEmpty(broadcastMessage.getType())
                        && (broadcastMessage.getType().equals(BroadcastMessage.TABLE_INSERT_SUCCESS) ||
                            broadcastMessage.getType().equals(BroadcastMessage.TABLE_UPDATED_SUCCESS) ||
                            broadcastMessage.getType().equals(BroadcastMessage.TABLE_DELETED_SUCCESS))) {
                        fetchTables();
                    }

            } catch (JsonProcessingException e) {
                log.error("Broadcast Handler Error", e);
            }
        });

        renderLayout();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    private void configureGrid() {
        tableDtoGrid.setSizeFull();
        tableDtoGrid.setEmptyStateText(UiUtil.NO_RECORDS);
        tableDtoGrid.setColumns("name", "capacity");
        tableDtoGrid.getColumnByKey("name").setHeader("Table Name");
        tableDtoGrid.getColumnByKey("capacity").setHeader("Capacity");
        tableDtoGrid.addColumn(this::getStoreName).setHeader("Store").setKey("store");
        tableDtoGrid.getColumns().forEach(tableDtoColumn -> tableDtoColumn.setAutoWidth(true));
        tableDtoGrid.addComponentColumn(this::applyButton).setHeader("Action");
    }

    private String getStoreName(TableDto tableDto) {
        return ObjectUtils.isNotEmpty(tableDto.getStoreDto())
                ? tableDto.getStoreDto().getName() : StringUtils.EMPTY;
    }

    private Component applyButton(TableDto tableDto) {        HorizontalLayout layout = new HorizontalLayout();
        layout.add(applyButtonEdit(tableDto));
        layout.add(applyButtonDelete(tableDto));
        return layout;
    }

    private Button applyButtonEdit(TableDto tableDto) {
        return UiUtil.editButton(event -> editTable(tableDto, FormAction.EDIT));
    }

    private Button applyButtonDelete(TableDto tableDto) {
        return UiUtil.deleteButton(
                new TableDeleteEventListener(tableDto, restClientSettingService, ui));
    }

    private HorizontalLayout getContent() {
        HorizontalLayout content = new HorizontalLayout(tableDtoGrid);
        content.setFlexGrow(1, tableDtoGrid);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
    }

    /**
     * Toolbar with a name filter and a "New Table" button that opens the
     * create dialog.
     *
     * @return the toolbar layout
     */
    public HorizontalLayout getToolbarComponent() {
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setPrefixComponent(VaadinIcon.SEARCH.create());
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
        filterText.addValueChangeListener(event -> filterTables(event.getValue()));

        Button addTableButton = UiUtil.addButton("New Table", event -> addTable());
        HorizontalLayout toolbar = new HorizontalLayout(filterText, addTableButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    @SuppressWarnings("unchecked")
    private void filterTables(String filter) {
        if (tableDtoGrid.getDataProvider() instanceof ListDataProvider) {
            ListDataProvider<TableDto> dataProvider = (ListDataProvider<TableDto>) tableDtoGrid.getDataProvider();
            dataProvider.setFilter(tableDto ->
                    StringUtils.containsIgnoreCase(tableDto.getName(), StringUtils.trimToEmpty(filter)));
        }
    }

    private void fetchTables() {
        loadingBar.start();
        StoreDto storeDto = accessService.getUserDetail().getStoreDto();
        if (ObjectUtils.isNotEmpty(storeDto) && ObjectUtils.isNotEmpty(storeDto.getId())) {
            asyncRestClientSettingService.getAllTablesByStore(result ->
                    ui.access(() -> {
                        loadingBar.stop();
                        tableDtoGrid.setItems(result);
                        filterTables(filterText.getValue());
                    }), storeDto.getId());
        } else {
            asyncRestClientSettingService.getAllTables(result ->
                    ui.access(() -> {
                        loadingBar.stop();
                        tableDtoGrid.setItems(result);
                        filterTables(filterText.getValue());
                    }));
        }
    }

    /**
     * Opens a dialog with a {@link TableForm} for creating or editing the
     * given table, seeding the store from the current user for new rows.
     *
     * @param tableDto   the table to edit, or an empty one to create
     * @param formAction whether the form creates or edits
     */
    public void editTable(TableDto tableDto, FormAction formAction) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(formAction == FormAction.EDIT ? "Edit Table" : "Add Table");
        dialog.setWidth("400px");
        dialog.add(new TableForm(dialog, formAction, prepareDto(tableDto), restClientSettingService));
        dialog.open();
    }

    private TableDto prepareDto(TableDto tableDto) {
        if (formActionIsCreate(tableDto)) {
            StoreDto storeDto = accessService.getUserDetail().getStoreDto();
            if (ObjectUtils.isNotEmpty(storeDto)) {
                tableDto.setStoreId(storeDto.getId());
            }
        }
        return tableDto;
    }

    private boolean formActionIsCreate(TableDto tableDto) {
        return ObjectUtils.isEmpty(tableDto.getId());
    }

    private void addTable() {
        tableDtoGrid.asSingleSelect().clear();
        editTable(new TableDto(), FormAction.CREATE);
    }
}
