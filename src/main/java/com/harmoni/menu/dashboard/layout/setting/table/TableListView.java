package com.harmoni.menu.dashboard.layout.setting.table;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.dto.TableDto;
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
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.data.provider.ListDataProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.harmoni.menu.dashboard.layout.util.Css;

/**
 * List of tables shown inside the table tab sheet: a {@link Grid} with name,
 * capacity, store and action columns, a filterable toolbar with cascading
 * brand/chain/store selectors and a "New Table" button. Refreshes on
 * {@link Broadcaster} messages; the fetch is scoped to the store selected in
 * the toolbar, falling back to all tables when none is chosen.
 */
@RequiredArgsConstructor
@Slf4j
public class TableListView extends AbstractListView {

    private static final int PAGE_SIZE = 1000;

    private final Grid<TableDto> tableDtoGrid = new Grid<>(TableDto.class);

    private final AsyncRestClientSettingService asyncRestClientSettingService;
    private final RestClientSettingService restClientSettingService;
    private final AccessService accessService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final GridSkeleton gridSkeleton = new GridSkeleton(8);

    private final ComboBox<ChainDto> chainFilter = new ComboBox<>();
    private final ComboBox<StoreDto> storeFilter = new ComboBox<>();

    private void renderLayout() {
        setSizeFull();
        setPadding(false);
        configureGrid();

        add(getContent());
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
        tableDtoGrid.setEmptyStateText(Messages.get("grid.empty.tables"));
        tableDtoGrid.setColumns("name", "capacity");
        tableDtoGrid.getColumnByKey("name").setHeader(Messages.get(Messages.Keys.LABEL_FIELD_TABLE_NAME));
        tableDtoGrid.getColumnByKey("capacity").setHeader(Messages.get(Messages.Keys.LABEL_CAPACITY));
        tableDtoGrid.addColumn(this::getStoreName).setHeader(Messages.get("grid.header.store")).setKey("store");
        tableDtoGrid.getColumns().forEach(tableDtoColumn -> tableDtoColumn.setAutoWidth(true));
        tableDtoGrid.addComponentColumn(this::applyButton).setHeader(Messages.get(Messages.Keys.GRID_HEADER_ACTION));
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
     * Toolbar with cascading brand/chain/store selectors, a lazy name filter
     * and a "New Table" button that opens the create tab.
     *
     * @return the toolbar layout
     */
    public HorizontalLayout getToolbarComponent() {
        configureSearchFilter();
        filterText.addValueChangeListener(event -> filterTables(event.getValue()));

        configureFilters();

        Button addTableButton = UiUtil.addButton(Messages.get(Messages.Keys.ACTION_NEW_TABLE), event -> addTable());
        HorizontalLayout toolbar = new HorizontalLayout(brandFilter, chainFilter, storeFilter, filterText, addTableButton);
        registerNewShortcut(this::addTable);
        toolbar.addClassName(Css.TOOLBAR);
        return toolbar;
    }

    private void configureFilters() {
        brandFilter.setLabel(Messages.get(Messages.Keys.LABEL_BRAND));
        brandFilter.setItemLabelGenerator(BrandDto::getName);

        chainFilter.setLabel(Messages.get("label.chain"));
        chainFilter.setItemLabelGenerator(ChainDto::getName);

        storeFilter.setLabel(Messages.get(Messages.Keys.LABEL_STORE));
        storeFilter.setItemLabelGenerator(StoreDto::getName);

        brandFilter.addValueChangeListener(this::onBrandFilterChange);
        chainFilter.addValueChangeListener(this::onChainFilterChange);
        storeFilter.addValueChangeListener(this::onStoreFilterChange);
        loadBrandsForFilter();
    }

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
            fetchTables();
            return;
        }
        if (change.getValue() != null) {
            loadChainsForFilter();
        }
    }

    private void onChainFilterChange(HasValue.ValueChangeEvent<ChainDto> change) {
        if (change.isFromClient() && change.getValue() == null && change.getOldValue() != null) {
            storeFilter.setItems(Collections.emptyList());
            storeFilter.clear();
            fetchTables();
            return;
        }
        if (change.getValue() != null) {
            loadStoresForFilter();
        }
    }

    private void onStoreFilterChange(HasValue.ValueChangeEvent<StoreDto> change) {
        fetchTables();
    }

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

    private void selectDefaultBrandFilter() {
        Integer brandId = sessionBrandId(accessService);
        brandFilter.getListDataView().getItems()
                .filter(brand -> Objects.equals(brand.getId(), brandId))
                .findFirst()
                .ifPresentOrElse(brandFilter::setValue,
                        () -> brandFilter.setValue(brandOptions.getFirst()));
    }

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
                        UiUtil.errorWithRetry(Messages.get("notification.chain.loadFailed"),
                                this::loadChainsForFilter)),
                brand.getId());
    }

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
                    List<StoreDto> stores = extractStores(result);
                    storeFilter.setItems(stores);
                    selectDefaultStoreFilter();
                }),
                error -> UiUtil.safeAccess(ui, () ->
                        UiUtil.errorWithRetry(Messages.get("notification.store.loadFailed"),
                                this::loadStoresForFilter)),
                chain.getId(), 1, PAGE_SIZE, "");
    }

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

    private Integer sessionChainId() {
        StoreDto sessionStore = accessService.getUserDetail().getStoreDto();
        if (ObjectUtils.isNotEmpty(sessionStore) && ObjectUtils.isNotEmpty(sessionStore.getChainDto())) {
            return sessionStore.getChainDto().getId();
        }
        return null;
    }

    private Integer sessionStoreId() {
        StoreDto sessionStore = accessService.getUserDetail().getStoreDto();
        if (ObjectUtils.isNotEmpty(sessionStore)) {
            return sessionStore.getId();
        }
        return null;
    }

    private List<StoreDto> extractStores(Map<String, Object> result) {
        List<StoreDto> stores = new ArrayList<>();
        if (result == null || !(result.get("data") instanceof List<?> dataList)) {
            return stores;
        }
        for (Object row : dataList) {
            StoreDto store = ObjectUtil.convertValueToObject(row, StoreDto.class);
            if (store != null) {
                stores.add(store);
            }
        }
        return stores;
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
        StoreDto store = storeFilter.getValue();
        if (ObjectUtils.isNotEmpty(store) && ObjectUtils.isNotEmpty(store.getId())) {
            asyncRestClientSettingService.getAllTablesByStore(result ->
                    UiUtil.safeAccess(ui, () -> {
                        gridSkeleton.hide();
                        tableDtoGrid.setItems(result);
                        filterTables(filterText.getValue());
                    }), error -> UiUtil.safeAccess(ui, () -> {
                        gridSkeleton.hide();
                        UiUtil.errorWithRetry(Messages.get(Messages.Keys.NOTIFICATION_TABLE_LOAD_FAILED), this::fetchTables);
                    }), store.getId());
        } else {
            asyncRestClientSettingService.getAllTables(result ->
                    UiUtil.safeAccess(ui, () -> {
                        gridSkeleton.hide();
                        tableDtoGrid.setItems(result);
                        filterTables(filterText.getValue());
                    }), error -> UiUtil.safeAccess(ui, () -> {
                        gridSkeleton.hide();
                        UiUtil.errorWithRetry(Messages.get(Messages.Keys.NOTIFICATION_TABLE_LOAD_FAILED), this::fetchTables);
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
                ? Messages.get(Messages.Keys.ACTION_EDIT_NAME, tableDto.getName()) : Messages.get(Messages.Keys.ACTION_NEW_TABLE);
        tabManager.addOrSelect(tabLabel, tab ->
                new TableForm(tabManager, tab, formAction, prepareDto(tableDto),
                        restClientSettingService, asyncRestClientOrganizationService, accessService));
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