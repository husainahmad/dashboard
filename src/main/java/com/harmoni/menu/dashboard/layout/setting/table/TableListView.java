package com.harmoni.menu.dashboard.layout.setting.table;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ChainDto;
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
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientSettingService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientSettingService;
import com.harmoni.menu.dashboard.util.Messages;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
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
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import com.harmoni.menu.dashboard.layout.util.Css;

/**
 * List of tables shown inside the table tab sheet: a {@link Grid} with name,
 * capacity, store, floor, status and action columns. The two-row toolbar
 * narrows the list by brand/chain/store/floor, keeps the floor management
 * actions together and ends with the "New Table" button; a summary line reports
 * the visible tables, seats and floors. Refreshes on {@link Broadcaster}
 * messages; the fetch is scoped to the store selected in the toolbar, falling
 * back to all tables when none is chosen.
 */
@RequiredArgsConstructor
@Slf4j
public class TableListView extends AbstractListView implements BroadcastMessageService {

    private static final int STORE_PAGE_SIZE = 1000;

    /** Rows shown per page; the table API returns the full list, so paging is client-side. */
    private static final int TABLE_PAGE_SIZE = 25;

    private final Grid<TableDto> tableDtoGrid = new Grid<>(TableDto.class);

    /** Every table returned by the current filter, before client-side paging. */
    private final List<TableDto> allTables = new ArrayList<>();

    /** The tables matching the search box, used for the summary and page count. */
    private List<TableDto> filteredTables = new ArrayList<>();

    private final AsyncRestClientSettingService asyncRestClientSettingService;
    private final RestClientSettingService restClientSettingService;
    private final AccessService accessService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final GridSkeleton gridSkeleton = new GridSkeleton(8);

    private final ComboBox<ChainDto> chainFilter = new ComboBox<>();
    private final ComboBox<StoreDto> storeFilter = new ComboBox<>();
    private final ComboBox<FloorDto> floorFilter = new ComboBox<>();
    private final Span summaryText = new Span();
    private final HorizontalLayout statsRow = new HorizontalLayout();
    private final Span tablesStatValue = new Span("0");
    private final Span seatsStatValue = new Span("0");
    private final Span floorsStatValue = new Span("0");
    private final Span activeStatValue = new Span("0");
    private boolean layoutBuilt;

    /**
     * Options for the brand filter, used to select a default when the user has one.
     * */
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
        // The list now lives on a non-default tab, so it attaches only when the
        // operator opens it. The toolbar is built before that, when the UI is
        // still unset, so its async brand load is dropped. Load the filter
        // options here, once the UI is available.
        if (brandOptions.isEmpty()) {
            loadBrandsForFilter();
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
     * Overview cards above a two-row toolbar: the upper row narrows the list by
     * scope (brand, chain, store, floor) with the search field, the lower row
     * holds the scope summary, floor management actions and the primary
     * "New Table" button.
     *
     * @return the stats + toolbar stack
     */
    public VerticalLayout getToolbarComponent() {
        configureSearchFilter();
        filterText.addValueChangeListener(event -> {
            currentPage = 1;
            renderPage();
        });

        configureFilters();

        Button addTableButton = UiUtil.addButton(Messages.get(Messages.Keys.ACTION_NEW_TABLE), event -> addTable());
        Button addFloorButton = new Button(Messages.get("action.newFloor"), event -> openFloorDialog(null));
        addFloorButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        addFloorButton.setPrefixComponent(VaadinIcon.PLUS.create());
        Button editFloorButton = UiUtil.editButton(Messages.get("label.floor"), event -> openFloorDialog(floorFilter.getValue()));
        Button deleteFloorButton = UiUtil.deleteButton(event -> deleteFloor(floorFilter.getValue()));
        editFloorButton.setEnabled(false);
        deleteFloorButton.setEnabled(false);
        floorFilter.addValueChangeListener(event -> {
            boolean selected = event.getValue() != null;
            editFloorButton.setEnabled(selected);
            deleteFloorButton.setEnabled(selected);
        });

        HorizontalLayout filters = new HorizontalLayout(brandFilter, chainFilter, storeFilter,
                floorFilter, filterText);
        filters.addClassName("table-toolbar-row");
        filters.addClassName("table-filter-bar");
        filters.setWidthFull();
        filters.setAlignItems(FlexComponent.Alignment.BASELINE);
        filters.setFlexGrow(1, filterText);

        summaryText.addClassName("table-summary");

        HorizontalLayout floorActions = new HorizontalLayout(addFloorButton, editFloorButton, deleteFloorButton);
        floorActions.addClassName("table-floor-actions");
        floorActions.setPadding(false);
        floorActions.setSpacing(true);
        floorActions.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout actions = new HorizontalLayout(summaryText, floorActions, addTableButton);
        actions.addClassName("table-toolbar-row");
        actions.addClassName("table-toolbar-actions");
        actions.setWidthFull();
        actions.setAlignItems(FlexComponent.Alignment.CENTER);
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        actions.setFlexGrow(1, summaryText);

        VerticalLayout toolbar = new VerticalLayout(filters, actions);
        toolbar.addClassName(Css.TOOLBAR);
        toolbar.addClassName("table-toolbar");
        toolbar.setWidthFull();
        toolbar.setPadding(false);
        toolbar.setSpacing(false);

        renderStats();

        VerticalLayout stack = new VerticalLayout(statsRow, toolbar);
        stack.addClassName("table-toolbar-stack");
        stack.setWidthFull();
        stack.setPadding(false);
        stack.setSpacing(false);
        registerNewShortcut(this::addTable);
        return stack;
    }

    /**
     * Builds the overview cards above the toolbar: total tables, seats, floors
     * and active tables for the current selection. Values are refreshed by
     * {@link #updateSummary()}.
     */
    private void renderStats() {
        statsRow.removeAll();
        statsRow.addClassName("table-stats");
        statsRow.setWidthFull();
        statsRow.setPadding(false);
        statsRow.setSpacing(false);
        statsRow.add(
                statCard(VaadinIcon.TABLE, tablesStatValue, Messages.get("table.stats.tables"), "tables"),
                statCard(VaadinIcon.USERS, seatsStatValue, Messages.get("table.stats.seats"), "seats"),
                statCard(VaadinIcon.BUILDING, floorsStatValue, Messages.get("table.stats.floors"), "floors"),
                statCard(VaadinIcon.CHECK_CIRCLE, activeStatValue, Messages.get("table.stats.active"), "active"));
    }

    /**
     * One overview card: an accent icon, the current value and its label.
     *
     * @param icon    the glyph shown in the tinted square
     * @param value   the value span, kept so summary refreshes can update it
     * @param label   the card caption
     * @param variant the accent variant class suffix
     * @return the card
     */
    private Component statCard(VaadinIcon icon, Span value, String label, String variant) {
        Icon glyph = icon.create();
        glyph.addClassName("stat-icon");

        value.addClassName("stat-value");
        Span caption = new Span(label);
        caption.addClassName("stat-label");

        VerticalLayout body = new VerticalLayout(value, caption);
        body.addClassName("stat-body");
        body.setPadding(false);
        body.setSpacing(false);

        HorizontalLayout card = new HorizontalLayout(glyph, body);
        card.addClassName("stat-card");
        card.addClassName("stat-card--" + variant);
        card.setPadding(false);
        card.setSpacing(false);
        card.setAlignItems(FlexComponent.Alignment.CENTER);
        return card;
    }

    /**
     * Configures the brand, chain and store filters with labels, item label
     * generators and value change listeners. Loads the brands for the filter on
     * initialization.
     */
    private void configureFilters() {
        brandFilter.setLabel(Messages.get(Messages.Keys.LABEL_BRAND));
        brandFilter.setItemLabelGenerator(BrandDto::getName);

        chainFilter.setLabel(Messages.get(Messages.Keys.LABEL_CHAIN));
        chainFilter.setItemLabelGenerator(ChainDto::getName);

        storeFilter.setLabel(Messages.get(Messages.Keys.LABEL_STORE));
        storeFilter.setItemLabelGenerator(StoreDto::getName);

        floorFilter.setLabel(Messages.get("label.floor"));
        floorFilter.setItemLabelGenerator(FloorDto::getName);
        floorFilter.setClearButtonVisible(true);
        floorFilter.addValueChangeListener(event -> {
            if (event.isFromClient()) {
                fetchTables();
            }
        });

        brandFilter.addValueChangeListener(this::onBrandFilterChange);
        chainFilter.addValueChangeListener(this::onChainFilterChange);
        storeFilter.addValueChangeListener(this::onStoreFilterChange);
    }

    /**
     * Handles changes to the brand filter. If the new value is null and the old
     * value is not null, it checks if the old value is still in the brand options.
     * If it is, it resets the filter to the old value. If not, it clears the chain
     * and store filters and fetches all tables. If the new value is not null, it
     * loads the chains for the selected brand.
     *
     * @param change the value change event for the brand filter
     */
    private void onBrandFilterChange(HasValue.ValueChangeEvent<BrandDto> change) {
        if (change.getValue() == null && change.getOldValue() != null && change.isFromClient()) {
            if (brandOptions.stream().anyMatch(brand -> Objects.equals(brand.getId(), change.getOldValue().getId()))) {
                brandFilter.setValue(change.getOldValue());
                return;
            }
            chainFilter.setItems(Collections.emptyList());
            chainFilter.clear();
            storeFilter.setItems(Collections.emptyList());
            storeFilter.clear();
            floorFilter.setItems(Collections.emptyList());
            floorFilter.clear();
            fetchFloors();
            fetchTables();
            return;
        }
        if (change.getValue() != null) {
            loadChainsForFilter();
        }
    }

    /**
     * Handles changes to the chain filter. If the new value is null and the old
     * value is not null, it clears the store filter and fetches all tables. If the
     * new value is not null, it loads the stores for the selected chain.
     *
     * @param change the value change event for the chain filter
     */
    private void onChainFilterChange(HasValue.ValueChangeEvent<ChainDto> change) {
        if (change.isFromClient() && change.getValue() == null && change.getOldValue() != null) {
            storeFilter.setItems(Collections.emptyList());
            storeFilter.clear();
            floorFilter.setItems(Collections.emptyList());
            floorFilter.clear();
            fetchFloors();
            fetchTables();
            return;
        }
        if (change.getValue() != null) {
            loadStoresForFilter();
        }
    }

    /**
     * Handles changes to the store filter. Whenever the store filter value
     * changes, it fetches the tables for the selected store.
     *
     * @param change the value change event for the store filter
     */
    private void onStoreFilterChange(HasValue.ValueChangeEvent<StoreDto> change) {
        floorFilter.setItems(Collections.emptyList());
        floorFilter.clear();
        fetchFloors();
        fetchTables();
    }

    /**
     * Loads the brands for the brand filter asynchronously. If the result is
     * empty, it fetches all tables. If there is an error, it shows an error
     * notification with a retry option.
     */
    private void loadBrandsForFilter() {
        asyncRestClientOrganizationService.getAllBrandAsync(
                result -> UiUtil.safeAccess(ui, () -> {
                    if (ObjectUtils.isEmpty(result)) {
                        fetchTables();
                        return;
                    }
                    brandOptions = result;
                    brandFilter.setItems(result);
                    selectDefaultBrandFilter();
                }),
                error -> UiUtil.safeAccess(ui, () ->
                        UiUtil.errorWithRetry(Messages.get(Messages.Keys.NOTIFICATION_BRAND_LOAD_FAILED),
                                this::loadBrandsForFilter)));
    }

    /**
     * Selects the default brand filter based on the user's session. If the
     * session brand ID is found in the brand options, it sets that as the value.
     * Otherwise, it selects the first brand option.
     */
    private void selectDefaultBrandFilter() {
        Integer brandId = sessionBrandId(accessService);
        brandFilter.getListDataView().getItems()
                .filter(brand -> Objects.equals(brand.getId(), brandId))
                .findFirst()
                .ifPresentOrElse(brandFilter::setValue,
                        () -> brandFilter.setValue(brandOptions.getFirst()));
    }

    /**
     * Loads the chains for the selected brand filter asynchronously. If the
     * brand is null or has no ID, it fetches all tables. If there is an error,
     * it shows an error notification with a retry option.
     */
    private void loadChainsForFilter() {
        BrandDto brand = brandFilter.getValue();
        chainFilter.setItems(Collections.emptyList());
        chainFilter.clear();
        storeFilter.setItems(Collections.emptyList());
        storeFilter.clear();
        if (brand == null || brand.getId() == null) {
            fetchTables();
            return;
        }
        asyncRestClientOrganizationService.getAllChainByBrandIdAsync(
                result -> UiUtil.safeAccess(ui, () -> {
                    if (ObjectUtils.isEmpty(result)) {
                        fetchTables();
                        return;
                    }
                    chainFilter.setItems(result);
                    selectDefaultChainFilter();
                }),
                error -> UiUtil.safeAccess(ui, () ->
                        UiUtil.errorWithRetry(Messages.get(Messages.Keys.NOTIFICATION_CHAIN_LOAD_FAILED),
                                this::loadChainsForFilter)),
                brand.getId());
    }

    /**
     * Selects the default chain filter based on the user's session. If the
     * session chain ID is found in the chain options, it sets that as the value.
     * Otherwise, it fetches all tables.
     */
    private void selectDefaultChainFilter() {
        Integer chainId = sessionChainId();
        boolean selected = chainFilter.getListDataView().getItems()
                .filter(chain -> Objects.equals(chain.getId(), chainId))
                .findFirst()
                .map(chain -> {
                    chainFilter.setValue(chain);
                    return true;
                })
                .orElse(false);
        if (!selected) {
            fetchTables();
        }
    }

    /**
     * Loads the stores for the selected chain filter asynchronously. If the
     * chain is null or has no ID, it fetches all tables. If there is an error,
     * it shows an error notification with a retry option.
     */
    private void loadStoresForFilter() {
        ChainDto chain = chainFilter.getValue();
        storeFilter.setItems(Collections.emptyList());
        storeFilter.clear();
        if (chain == null || chain.getId() == null) {
            fetchTables();
            return;
        }
        asyncRestClientOrganizationService.getAllStoreAsync(
                result -> UiUtil.safeAccess(ui, () -> {
                    List<StoreDto> stores = ObjectUtil.extractDataList(result, StoreDto.class);
                    storeFilter.setItems(stores);
                    selectDefaultStoreFilter();
                }),
                error -> UiUtil.safeAccess(ui, () ->
                        UiUtil.errorWithRetry(Messages.get(Messages.Keys.NOTIFICATION_STORE_LOAD_FAILED),
                                this::loadStoresForFilter)),
                chain.getId(), 1, STORE_PAGE_SIZE, "");
    }

    /**
     * Selects the default store filter based on the user's session. If the
     * session store ID is found in the store options, it sets that as the value.
     * Otherwise, it fetches all tables.
     */
    private void selectDefaultStoreFilter() {
        Integer storeId = sessionStoreId();
        boolean selected = storeFilter.getListDataView().getItems()
                .filter(store -> Objects.equals(store.getId(), storeId))
                .findFirst()
                .map(store -> {
                    storeFilter.setValue(store);
                    return true;
                })
                .orElse(false);
        if (!selected) {
            fetchTables();
        }
    }

    /**
     * Retrieves the brand ID from the user's session. If the session store and
     * chain are not empty, it returns the brand ID. Otherwise, it returns null.
     *
     * @param accessService the access service to get user details
     * @return the brand ID from the session or null
     */
    private Integer sessionChainId() {
        StoreDto sessionStore = accessService.getUserDetail().getStoreDto();
        if (ObjectUtils.isNotEmpty(sessionStore) && ObjectUtils.isNotEmpty(sessionStore.getChainDto())) {
            return sessionStore.getChainDto().getId();
        }
        return null;
    }

    /**
     * Retrieves the store ID from the user's session. If the session store is
     * not empty, it returns the store ID. Otherwise, it returns null.
     *
     * @return the store ID from the session or null
     */
    private Integer sessionStoreId() {
        StoreDto sessionStore = accessService.getUserDetail().getStoreDto();
        if (ObjectUtils.isNotEmpty(sessionStore)) {
            return sessionStore.getId();
        }
        return null;
    }

    /**
     * Applies the search box to {@link #allTables}, recomputes the page count and
     * shows the current page in the grid. Client-side paging keeps the summary
     * and the footer in sync with the whole filtered selection, not just the
     * rows on screen.
     */
    private void renderPage() {
        String filter = StringUtils.trimToEmpty(filterText.getValue());
        filteredTables = allTables.stream()
                .filter(table -> StringUtils.containsIgnoreCase(table.getName(), filter))
                .toList();

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
     * Refreshes the caption above the grid with the number of tables, seats and
     * floors in the filtered selection, so the operator can read its size
     * without counting rows.
     */
    private void updateSummary() {
        List<TableDto> visible = filteredTables;
        int seats = visible.stream()
                .mapToInt(table -> table.getCapacity() == null ? 0 : table.getCapacity())
                .sum();
        long floors = visible.stream()
                .map(this::getFloorName)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .count();
        long active = visible.stream()
                .filter(table -> !Boolean.FALSE.equals(table.getActive()))
                .count();

        tablesStatValue.setText(String.valueOf(visible.size()));
        seatsStatValue.setText(String.valueOf(seats));
        floorsStatValue.setText(String.valueOf(floors));
        activeStatValue.setText(String.valueOf(active));
        summaryText.setText(scopeSummary(visible.size()));
    }

    /**
     * Builds the line shown in the action row. It names the scope the counts
     * belong to, so the cards above and the grid below are unambiguous when a
     * floor or store filter is active.
     *
     * @param visibleCount the number of tables currently listed
     * @return the scope caption
     */
    private String scopeSummary(int visibleCount) {
        FloorDto floor = floorFilter.getValue();
        if (floor != null && floor.getName() != null && !floor.getName().isBlank()) {
            return Messages.get("table.scope.floor", visibleCount, floor.getName());
        }
        StoreDto store = storeFilter.getValue();
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
        StoreDto store = storeFilter.getValue();
        if (ObjectUtils.isEmpty(store) && accessService.getUserDetail() != null) {
            store = accessService.getUserDetail().getStoreDto();
        }
        if (store == null) {
            store = new StoreDto();
        }
        new FloorFormDialog(floor == null ? new FloorDto() : floor, store,
                restClientSettingService, asyncRestClientOrganizationService, accessService,
                this::fetchFloors).open();
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
                    fetchFloors();
                }), error -> UiUtil.safeAccess(ui, () ->
                        UiUtil.error(Messages.get("notification.floor.deleteHasTables")))));
        dialog.open();
    }

    /**
     * Fetches the tables from the server. A selected floor wins; otherwise the
     * store filter is used, falling back to all tables when neither is chosen.
     * The grid skeleton is shown while fetching, and errors offer a retry.
     */
    private void fetchTables() {
        gridSkeleton.show();
        FloorDto floor = floorFilter.getValue();
        if (floor != null && floor.getId() != null) {
            asyncRestClientSettingService.getAllTablesByFloor(
                    this::onTablesLoaded, this::onTablesLoadFailed, floor.getId());
            return;
        }

        StoreDto store = storeFilter.getValue();
        if (ObjectUtils.isNotEmpty(store) && ObjectUtils.isNotEmpty(store.getId())) {
            asyncRestClientSettingService.getAllTablesByStore(
                    this::onTablesLoaded, this::onTablesLoadFailed, store.getId());
        } else {
            asyncRestClientSettingService.getAllTables(
                    this::onTablesLoaded, this::onTablesLoadFailed);
        }
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

    /**
     * Loads the floors of the selected store into the floor filter.
     */
    private void fetchFloors() {
        Integer storeId = currentStoreId();
        if (storeId == null) {
            floorFilter.setItems(Collections.emptyList());
            floorFilter.clear();
            return;
        }
        asyncRestClientSettingService.getAllFloorsByStore(result ->
                UiUtil.safeAccess(ui, () -> floorFilter.setItems(
                        result == null ? Collections.emptyList() : result)),
                error -> UiUtil.safeAccess(ui, () ->
                        UiUtil.errorWithRetry(Messages.get("notification.floor.loadFailed"), this::fetchFloors)),
                storeId);
    }

    private Integer currentStoreId() {
        StoreDto store = storeFilter.getValue();
        if (store != null && store.getId() != null) {
            return store.getId();
        }
        StoreDto sessionStore = accessService.getUserDetail() == null
                ? null : accessService.getUserDetail().getStoreDto();
        return sessionStore == null ? null : sessionStore.getId();
    }

    private void onFloorOrTableChanged() {
        fetchFloors();
        fetchTables();
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
            StoreDto storeDto = accessService.getUserDetail().getStoreDto();
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