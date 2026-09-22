package com.harmoni.menu.dashboard.layout.setting.table;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.dto.TableDto;
import com.harmoni.menu.dashboard.event.table.TableDeleteEventListener;
import com.harmoni.menu.dashboard.layout.AbstractListView;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.GridSkeleton;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientSettingService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientSettingService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.data.provider.ListDataProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.Set;

/**
 * List of tables shown inside the table tab sheet: a {@link Grid} with name,
 * capacity, store and action columns, a filterable toolbar and a "New Table"
 * button. Refreshes on {@link Broadcaster} messages and scopes the fetch to
 * the current store when one is available.
 */
@RequiredArgsConstructor
@Slf4j
public class TableListView extends AbstractListView {

    private final Grid<TableDto> tableDtoGrid = new Grid<>(TableDto.class);

    private final AsyncRestClientSettingService asyncRestClientSettingService;
    private final RestClientSettingService restClientSettingService;
    private final AccessService accessService;
    private final GridSkeleton gridSkeleton = new GridSkeleton(8);

    private void renderLayout() {
        setSizeFull();
        setPadding(false);
        configureGrid();

        add(getContent());
        fetchTables();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        refreshOnBroadcast(Set.of(BroadcastMessage.TABLE_INSERT_SUCCESS,
                BroadcastMessage.TABLE_UPDATED_SUCCESS,
                BroadcastMessage.TABLE_DELETED_SUCCESS), this::fetchTables);

        renderLayout();
    }

    private void configureGrid() {
        tableDtoGrid.setSizeFull();
        tableDtoGrid.setEmptyStateText("No tables yet \u2014 click \u201CNew Table\u201D to add one.");
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
        return gridSlot(tableDtoGrid, gridSkeleton);
    }

    /**
     * Toolbar with a name filter and a "New Table" button that opens the
     * create tab.
     *
     * @return the toolbar layout
     */
    public HorizontalLayout getToolbarComponent() {
        configureSearchFilter();
        filterText.addValueChangeListener(event -> filterTables(event.getValue()));

        Button addTableButton = UiUtil.addButton("New Table", event -> addTable());
        HorizontalLayout toolbar = new HorizontalLayout(filterText, addTableButton);
        registerNewShortcut(this::addTable);
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
        gridSkeleton.show();
        StoreDto storeDto = accessService.getUserDetail().getStoreDto();
        if (ObjectUtils.isNotEmpty(storeDto) && ObjectUtils.isNotEmpty(storeDto.getId())) {
            asyncRestClientSettingService.getAllTablesByStore(result ->
                    UiUtil.safeAccess(ui, () -> {
                        gridSkeleton.hide();
                        tableDtoGrid.setItems(result);
                        filterTables(filterText.getValue());
                    }), error -> UiUtil.safeAccess(ui, () -> {
                        gridSkeleton.hide();
                        UiUtil.errorWithRetry("Couldn't load tables", this::fetchTables);
                    }), storeDto.getId());
        } else {
            asyncRestClientSettingService.getAllTables(result ->
                    UiUtil.safeAccess(ui, () -> {
                        gridSkeleton.hide();
                        tableDtoGrid.setItems(result);
                        filterTables(filterText.getValue());
                    }), error -> UiUtil.safeAccess(ui, () -> {
                        gridSkeleton.hide();
                        UiUtil.errorWithRetry("Couldn't load tables", this::fetchTables);
                    }));
        }
    }

    /**
     * Opens a {@link TableForm} in a new tab for creating or editing the given
     * table, seeding the store from the current user for new rows.
     *
     * @param tableDto   the table to edit, or an empty one to create
     * @param formAction whether the form creates or edits
     */
    public void editTable(TableDto tableDto, FormAction formAction) {
        if (!(this.getParent().orElseThrow() instanceof TabSheet tabSheet)) {
            return;
        }
        TabManager tabManager = new TabManager(tabSheet);
        String tabLabel = formAction == FormAction.EDIT && ObjectUtils.isNotEmpty(tableDto.getName())
                ? "Edit ".concat(tableDto.getName()) : "New Table";
        tabManager.addOrSelect(tabLabel, tab ->
                new TableForm(tabManager, tab, formAction, prepareDto(tableDto), restClientSettingService));
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
