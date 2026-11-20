package com.harmoni.menu.dashboard.layout.setting.table;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.FloorDto;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.dto.TableDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.event.table.TableDeleteEventListener;
import com.harmoni.menu.dashboard.layout.AbstractListView;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.GridSkeleton;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.ScopeService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientSettingService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientSettingService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import com.harmoni.menu.dashboard.layout.util.Css;

/**
 * List of tables shown inside the table tab sheet: a {@link Grid} with name,
 * capacity, store, floor, status and action columns. The brand → chain → store
 * → floor filter lives in the page toolbar above the tab sheet and is shared
 * with the plan tab; this view narrows its grid to that selection. Refreshes on
 * {@link Broadcaster} messages.
 */
@RequiredArgsConstructor
@Slf4j
public class TableListView extends AbstractListView implements BroadcastMessageService {

    /** Rows shown per page; the table API returns the full list, so paging is client-side. */
    private static final int TABLE_PAGE_SIZE = 25;

    private final Grid<TableDto> tableDtoGrid = new Grid<>(TableDto.class);

    /** Every table returned by the current filter, before client-side paging. */
    private final List<TableDto> allTables = new ArrayList<>();

    /** The tables of the current selection, used for the summary and page count. */
    private List<TableDto> filteredTables = new ArrayList<>();

    private final AsyncRestClientSettingService asyncRestClientSettingService;
    private final RestClientSettingService restClientSettingService;
    private final AccessService accessService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final GridSkeleton gridSkeleton = new GridSkeleton(8);
    private final TableFilterBar filterBar;

    private final Span summaryText = new Span();
    private boolean layoutBuilt;
    private boolean filterWired;

    private void renderLayout() {
        setSizeFull();
        setPadding(false);
        configureGrid();

        add(getContent(), getPaginationFooter());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        refreshOnBroadcast(Set.of(BroadcastMessage.TABLE_INSERT_SUCCESS,
                BroadcastMessage.TABLE_UPDATED_SUCCESS,
                BroadcastMessage.TABLE_DELETED_SUCCESS,
                BroadcastMessage.FLOOR_INSERT_SUCCESS,
                BroadcastMessage.FLOOR_UPDATED_SUCCESS,
                BroadcastMessage.FLOOR_DELETED_SUCCESS), this::onFloorOrTableChanged);

        if (!layoutBuilt) {
            layoutBuilt = true;
            renderLayout();
        }
        if (!filterWired) {
            filterWired = true;
            filterBar.addSelectionListener(this::fetchTables);
        }
        if (filterBar.getSelectedStore() != null) {
            fetchTables();
        }
    }

    /**
     * Configures the {@link Grid}: the name with an initial avatar, a capacity
     * pill, the store and floor as plain text, an active/inactive status chip
     * and the edit/delete action column. Rows whose table is switched off are
     * dimmed through a part name rather than by overriding the theme.
     */
    private void configureGrid() {
        tableDtoGrid.setSizeFull();
        tableDtoGrid.setSelectionMode(Grid.SelectionMode.NONE);
        tableDtoGrid.setEmptyStateText(Messages.get("grid.empty.tables"));
        tableDtoGrid.removeAllColumns();

        tableDtoGrid.addComponentColumn(this::nameCell)
                .setHeader(Messages.get(Messages.Keys.LABEL_FIELD_TABLE_NAME))
                .setKey("name")
                .setWidth("280px")
                .setFlexGrow(1);
        tableDtoGrid.addComponentColumn(this::capacityCell)
                .setHeader(Messages.get(Messages.Keys.LABEL_CAPACITY))
                .setKey("capacity")
                .setWidth("130px")
                .setFlexGrow(0);
        tableDtoGrid.addComponentColumn(this::storeCell)
                .setHeader(Messages.get("grid.header.store"))
                .setKey("store")
                .setFlexGrow(1);
        tableDtoGrid.addComponentColumn(this::floorCell)
                .setHeader(Messages.get("grid.header.floor"))
                .setKey("floor")
                .setFlexGrow(1);
        tableDtoGrid.addComponentColumn(this::statusCell)
                .setHeader(Messages.get("grid.header.status"))
                .setKey("status")
                .setWidth("130px")
                .setFlexGrow(0);
        tableDtoGrid.addComponentColumn(this::applyButton)
                .setHeader(Messages.get(Messages.Keys.GRID_HEADER_ACTION))
                .setKey("actions")
                .setWidth("120px")
                .setFlexGrow(0);

        tableDtoGrid.addItemDoubleClickListener(event -> editTable(event.getItem(), FormAction.EDIT));

        tableDtoGrid.setPartNameGenerator(tableDto ->
                Boolean.FALSE.equals(tableDto.getActive()) ? "table-row-inactive" : null);
    }

    /**
     * Renders the table name with its initial as a quiet avatar. The floor lives
     * in its own column now, so the cell stays a single, scannable line.
     *
     * @param tableDto the table to render
     * @return the name cell
     */
    private Component nameCell(TableDto tableDto) {
        String nameText = StringUtils.defaultIfBlank(tableDto.getName(), "-");

        Div avatar = new Div();
        avatar.addClassName("table-avatar");
        avatar.setText(initialOf(nameText));
        if (Boolean.FALSE.equals(tableDto.getActive())) {
            avatar.addClassName("table-avatar--inactive");
        }

        Span name = new Span(nameText);
        name.addClassName("table-name");

        HorizontalLayout cell = new HorizontalLayout(avatar, name);
        cell.addClassName("table-name-cell");
        cell.setPadding(false);
        cell.setSpacing(true);
        cell.setAlignItems(FlexComponent.Alignment.CENTER);
        return cell;
    }

    private static String initialOf(String name) {
        String trimmed = StringUtils.trimToEmpty(name);
        return trimmed.isEmpty() ? "?" : trimmed.substring(0, 1).toUpperCase();
    }

    /**
     * Renders the seating capacity as a compact pill, without an icon so the
     * column stays a plain numeric value.
     *
     * @param tableDto the table to render
     * @return the capacity cell
     */
    private Component capacityCell(TableDto tableDto) {
        Integer capacity = tableDto.getCapacity();
        Span value = new Span(capacity == null ? "-" : Messages.get("label.seats", capacity));
        HorizontalLayout pill = new HorizontalLayout(value);
        pill.addClassName("seat-pill");
        pill.setPadding(false);
        pill.setSpacing(false);
        pill.setAlignItems(FlexComponent.Alignment.CENTER);
        return pill;
    }

    /**
     * Renders the store as a quiet name, without an icon so the column reads as
     * plain metadata.
     *
     * @param tableDto the table to render
     * @return the store cell
     */
    private Component storeCell(TableDto tableDto) {
        String storeName = getStoreName(tableDto);
        Span label = new Span(storeName.isBlank() ? "-" : storeName);
        label.addClassName("table-store-name");
        return label;
    }

    /**
     * Renders the floor the table stands on, or a muted "no floor" hint when it
     * is not assigned yet.
     *
     * @param tableDto the table to render
     * @return the floor cell
     */
    private Component floorCell(TableDto tableDto) {
        String floorName = getFloorName(tableDto);
        Span label = new Span(floorName.isBlank()
                ? Messages.get("table.unassigned") : floorName);
        label.addClassName("table-floor-name");
        return label;
    }

    /**
     * Renders the active state as a status chip. A missing flag is treated as
     * active, matching the floor plan.
     *
     * @param tableDto the table to render
     * @return the status cell
     */
    private Component statusCell(TableDto tableDto) {
        boolean active = !Boolean.FALSE.equals(tableDto.getActive());
        Span status = new Span(Messages.get(active ? "table.status.active" : "table.status.inactive"));
        status.addClassName("table-status");
        status.addClassName(active ? "table-status--active" : "table-status--inactive");
        return status;
    }

    /**
     * Returns the store name for the given table, or an empty string if the
     * store is not set.
     *
     * @param tableDto the table to get the store name for
     * @return the store name or an empty string
     */
    private String getStoreName(TableDto tableDto) {
        return ObjectUtils.isNotEmpty(tableDto.getStoreDto())
                ? StringUtils.defaultString(tableDto.getStoreDto().getName()) : StringUtils.EMPTY;
    }

    /**
     * Returns the floor name for the given table, or an empty string when the
     * table is not attached to a floor yet.
     *
     * @param tableDto the table to get the floor name for
     * @return the floor name or an empty string
     */
    private String getFloorName(TableDto tableDto) {
        return ObjectUtils.isNotEmpty(tableDto.getFloorDto())
                ? StringUtils.defaultString(tableDto.getFloorDto().getName()) : StringUtils.EMPTY;
    }

    /**
     * Returns a horizontal layout with edit and delete buttons for the given
     * table. The edit button opens the {@link TableForm} in a new tab, while the
     * delete button triggers a {@link TableDeleteEventListener}.
     *
     * @param tableDto the table to apply the buttons to
     * @return a horizontal layout with edit and delete buttons
     */
    private Component applyButton(TableDto tableDto) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.add(applyButtonEdit(tableDto));
        layout.add(applyButtonDelete(tableDto));
        return layout;
    }

    /**
     * Returns an edit button that opens the {@link TableForm} in a new tab for
     * editing the given table.
     *
     * @param tableDto the table to edit
     * @return an edit button
     */
    private Button applyButtonEdit(TableDto tableDto) {
        return UiUtil.editButton(event -> editTable(tableDto, FormAction.EDIT));
    }

    /**
     * Returns a delete button that triggers a {@link TableDeleteEventListener}
     * for the given table.
     *
     * @param tableDto the table to delete
     * @return a delete button
     */
    private Button applyButtonDelete(TableDto tableDto) {
        return UiUtil.deleteButton(
                new TableDeleteEventListener(tableDto, restClientSettingService, ui));
    }

    /**
     * Returns a horizontal layout containing the table grid and a skeleton
     * loader. The skeleton is shown while the tables are being fetched from the
     * server.
     *
     * @return a horizontal layout with the table grid and skeleton loader
     */
    private HorizontalLayout getContent() {
        return gridSlot(tableDtoGrid, gridSkeleton);
    }

    /**
     * Builds the client-side paging footer. The table endpoints return the whole
     * list, so the buttons only move the window over {@link #filteredTables}.
     *
     * @return the pagination footer
     */
    private HorizontalLayout getPaginationFooter() {
        return paginationFooter(() -> {
            if (currentPage > 1) {
                currentPage--;
                renderPage();
            }
        }, () -> {
            if (currentPage < totalPages) {
                currentPage++;
                renderPage();
            }
        });
    }

    /**
     * Builds the single page toolbar, matching the other list pages: the shared
     * brand → chain → store → floor filter, the selection summary and the
     * primary "New Table" button in one row.
     *
     * @return the toolbar row
     */
    public HorizontalLayout getToolbarComponent() {
        Button addTableButton = UiUtil.addButton(Messages.get(Messages.Keys.ACTION_NEW_TABLE), event -> addTable());

        summaryText.addClassName("table-summary");
        summaryText.setVisible(false);

        HorizontalLayout toolbar = new HorizontalLayout(filterBar, summaryText, addTableButton);
        toolbar.addClassName(Css.TOOLBAR);
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.BASELINE);
        toolbar.setFlexGrow(1, filterBar);
        toolbar.setFlexGrow(2, summaryText);
        registerNewShortcut(this::addTable);
        return toolbar;
    }

    /**
     * Builds the floor management actions shown inside the "All Tables" tab, so
     * the add/edit/delete controls stay next to the data they affect.
     *
     * @return the floor action row
     */
    public HorizontalLayout getFloorActionsComponent() {
        Button addFloorButton = new Button(Messages.get("action.newFloor"), event -> openFloorDialog(null));
        addFloorButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        addFloorButton.setPrefixComponent(VaadinIcon.PLUS.create());
        Button editFloorButton = UiUtil.editButton(Messages.get("label.floor"),
                event -> openFloorDialog(filterBar.getSelectedFloor()));
        Button deleteFloorButton = UiUtil.deleteButton(event -> deleteFloor(filterBar.getSelectedFloor()));

        Runnable updateFloorActions = () -> {
            boolean selected = filterBar.getSelectedFloor() != null;
            editFloorButton.setEnabled(selected);
            deleteFloorButton.setEnabled(selected);
        };
        filterBar.addSelectionListener(updateFloorActions);
        updateFloorActions.run();

        HorizontalLayout floorActions = new HorizontalLayout(addFloorButton, editFloorButton, deleteFloorButton);
        floorActions.addClassName("table-floor-actions");
        floorActions.setPadding(false);
        floorActions.setSpacing(true);
        floorActions.setAlignItems(FlexComponent.Alignment.CENTER);
        floorActions.setWidthFull();
        floorActions.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        return floorActions;
    }

    /**
     * Recomputes the page count and shows the current page of
     * {@link #allTables}. Client-side paging keeps the summary and the footer
     * in sync with the whole selection, not just the rows on screen.
     */
    private void renderPage() {
        filteredTables = new ArrayList<>(allTables);

        totalPages = filteredTables.isEmpty()
                ? 0 : (int) Math.ceil((double) filteredTables.size() / TABLE_PAGE_SIZE);
        if (totalPages == 0) {
            currentPage = 1;
        } else if (currentPage > totalPages) {
            currentPage = totalPages;
        }

        int from = Math.min((currentPage - 1) * TABLE_PAGE_SIZE, filteredTables.size());
        int to = Math.min(from + TABLE_PAGE_SIZE, filteredTables.size());
        tableDtoGrid.setItems(filteredTables.subList(from, to));

        updatePagination();
        updateSummary();
    }

    /**
     * Refreshes the caption in the toolbar with the number of tables in the
     * filtered selection, so the operator can read its size without counting
     * rows.
     */
    private void updateSummary() {
        summaryText.setText(scopeSummary(filteredTables.size()));
        summaryText.setVisible(true);
    }

    /**
     * Builds the line shown in the toolbar. It names the scope the count
     * belongs to, so the grid is unambiguous when a floor or store filter is
     * active.
     *
     * @param visibleCount the number of tables currently listed
     * @return the scope caption
     */
    private String scopeSummary(int visibleCount) {
        FloorDto floor = filterBar.getSelectedFloor();
        if (floor != null && floor.getName() != null && !floor.getName().isBlank()) {
            return Messages.get("table.scope.floor", visibleCount, floor.getName());
        }
        StoreDto store = filterBar.getSelectedStore();
        if (store != null && store.getName() != null && !store.getName().isBlank()) {
            return Messages.get("table.scope.store", visibleCount, store.getName());
        }
        return Messages.get("table.scope.all", visibleCount);
    }

    /**
     * Opens the create/edit dialog for a floor of the current store.
     *
     * @param floor the floor to edit, or {@code null} to create one
     */
    private void openFloorDialog(FloorDto floor) {
        StoreDto store = filterBar.getSelectedStore();
        if (ObjectUtils.isEmpty(store) && accessService.getUserDetail() != null) {
            store = accessService.getUserDetail().getStoreDto();
        }
        if (store == null) {
            store = new StoreDto();
        }
        new FloorFormDialog(floor == null ? new FloorDto() : floor, store,
                restClientSettingService, asyncRestClientOrganizationService, accessService,
                filterBar::reloadFloors).open();
    }

    /**
     * Confirms and deletes a floor. The server refuses while the floor still has
     * tables; that refusal is shown as a clear message.
     *
     * @param floor the floor to delete
     */
    private void deleteFloor(FloorDto floor) {
        if (floor == null || floor.getId() == null) {
            return;
        }
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(Messages.get(Messages.Keys.DIALOG_CONFIRM_TITLE));
        dialog.setText(Messages.get("dialog.confirmDeleteFloor", floor.getName()));
        dialog.setCancelable(true);
        dialog.addConfirmListener(event -> restClientSettingService.deleteFloor(floor)
                .subscribe(response -> UiUtil.safeAccess(ui, () -> {
                    broadcastMessage(BroadcastMessage.FLOOR_DELETED_SUCCESS, response);
                    UiUtil.success(Messages.get("notification.floor.deleted"));
                    filterBar.reloadFloors();
                }), error -> UiUtil.safeAccess(ui, () ->
                        UiUtil.error(Messages.get("notification.floor.deleteHasTables")))));
        dialog.open();
    }

    /**
     * Fetches the tables from the server for the shared filter selection. A
     * selected floor wins; otherwise the selected store is used. A scoped user
     * with no store selected sees an empty grid instead of the unfiltered list.
     * The grid skeleton is shown while fetching, and errors offer a retry.
     */
    private void fetchTables() {
        gridSkeleton.show();
        FloorDto floor = filterBar.getSelectedFloor();
        if (floor != null && floor.getId() != null) {
            asyncRestClientSettingService.getAllTablesByFloor(
                    this::onTablesLoaded, this::onTablesLoadFailed, floor.getId());
            return;
        }

        StoreDto store = filterBar.getSelectedStore();
        if (ObjectUtils.isNotEmpty(store) && ObjectUtils.isNotEmpty(store.getId())) {
            asyncRestClientSettingService.getAllTablesByStore(
                    this::onTablesLoaded, this::onTablesLoadFailed, store.getId());
            return;
        }

        ScopeService service = ScopeService.current();
        ScopeService.ScopeSnapshot scope = service == null ? null : service.snapshot();
        if (scope != null && !scope.isGlobal()) {
            gridSkeleton.hide();
            applyTables(Collections.emptyList());
            return;
        }
        asyncRestClientSettingService.getAllTables(
                this::onTablesLoaded, this::onTablesLoadFailed);
    }

    private void onTablesLoaded(List<TableDto> tables) {
        UiUtil.safeAccess(ui, () -> {
            gridSkeleton.hide();
            applyTables(tables);
        });
    }

    private void onTablesLoadFailed(Throwable error) {
        UiUtil.safeAccess(ui, () -> {
            gridSkeleton.hide();
            UiUtil.errorWithRetry(Messages.get(Messages.Keys.NOTIFICATION_TABLE_LOAD_FAILED), this::fetchTables);
        });
    }

    private void applyTables(List<TableDto> tables) {
        allTables.clear();
        if (tables != null) {
            allTables.addAll(tables);
        }
        currentPage = 1;
        renderPage();
    }

    private void onFloorOrTableChanged() {
        // Reloading the floors also fires the shared-filter listener, which
        // refetches the tables for the new selection.
        filterBar.reloadFloors();
    }

    /**
     * Opens a {@link TableForm} in a new tab for creating or editing the given
     * table, seeding the store from the current user for new rows.
     *
     * @param tableDto   the table to edit, or an empty one to create
     * @param formAction whether the form creates or edits
     */
    public void editTable(TableDto tableDto, FormAction formAction) {
        TabSheet tabSheet = enclosingTabSheet();
        if (tabSheet == null) {
            log.warn("Cannot open the table form: no TabSheet ancestor found for TableListView");
            return;
        }
        TabManager tabManager = new TabManager(tabSheet);
        String tabLabel = formAction == FormAction.EDIT && ObjectUtils.isNotEmpty(tableDto.getName())
                ? Messages.get(Messages.Keys.ACTION_EDIT_NAME, tableDto.getName()) : Messages.get(Messages.Keys.ACTION_NEW_TABLE);
        String tabKey = tableDto.getId() == null ? "table:new" : "table:" + tableDto.getId();
        VaadinIcon tabIcon = tableDto.getId() == null ? VaadinIcon.PLUS_CIRCLE : VaadinIcon.TABLE;
        tabManager.addOrSelect(tabKey, tabLabel, tabIcon, tab ->
                new TableForm(tabManager, tab, formAction, prepareDto(tableDto),
                        restClientSettingService, asyncRestClientOrganizationService,
                        asyncRestClientSettingService, accessService));
    }

    /**
     * Finds the {@link TabSheet} that hosts this list.
     *
     * <p>The list is wrapped in a panel with the toolbar before it is added to
     * the tab sheet, so its direct parent is that panel, not the tab sheet.
     * Walking up the ancestor chain is what makes the "New Table" button open a
     * tab; checking only the direct parent silently did nothing.</p>
     *
     * @return the hosting tab sheet, or {@code null} when the list is detached
     */
    private TabSheet enclosingTabSheet() {
        return findTabSheetAncestor(this).orElse(null);
    }

    private static Optional<TabSheet> findTabSheetAncestor(Component component) {
        Optional<Component> parent = component.getParent();
        while (parent.isPresent()) {
            Component current = parent.get();
            if (current instanceof TabSheet tabSheet) {
                return Optional.of(tabSheet);
            }
            parent = current.getParent();
        }
        return Optional.empty();
    }

    /**
     * Prepares the given {@link TableDto} for editing or creating. If the form
     * action is create, it seeds the store from the current user's session.
     *
     * @param tableDto the table DTO to prepare
     * @return the prepared table DTO
     */
    private TableDto prepareDto(TableDto tableDto) {
        if (formActionIsCreate(tableDto)) {
            StoreDto storeDto = filterBar.getSelectedStore();
            if (storeDto == null && accessService.getUserDetail() != null) {
                storeDto = accessService.getUserDetail().getStoreDto();
            }
            if (ObjectUtils.isNotEmpty(storeDto)) {
                tableDto.setStoreId(storeDto.getId());
            }
        }
        return tableDto;
    }

    /**
     * Determines if the form action is for creating a new table. Returns true if
     * the table DTO's ID is empty, indicating a new table; otherwise, returns
     * false.
     *
     * @param tableDto the table DTO to check
     * @return true if creating a new table, false otherwise
     */
    private boolean formActionIsCreate(TableDto tableDto) {
        return ObjectUtils.isEmpty(tableDto.getId());
    }

    /**
     * Opens a {@link TableForm} in a new tab for creating a new table. Clears any
     * selection in the grid and uses the parent {@link TabSheet} to manage the
     * new tab.
     */
    private void addTable() {
        editTable(new TableDto(), FormAction.CREATE);
    }
}