package com.harmoni.menu.dashboard.layout.setting.table;

import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.dto.FloorDto;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.layout.component.StoreCascade;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientSettingService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Single brand → chain → store → floor filter shared by the floor plan and the
 * table list, so switching between the two tabs keeps the same selection.
 *
 * <p>The cascade is scope-aware: the options are filtered and locked from the
 * signed-in user's RBAC assignments. Floors are loaded for the selected store,
 * and the first accessible floor is selected by default.</p>
 */
public class TableFilterBar extends HorizontalLayout {

    private final transient AsyncRestClientSettingService settingService;
    private final ComboBox<BrandDto> brandFilter = new ComboBox<>();
    private final ComboBox<ChainDto> chainFilter = new ComboBox<>();
    private final ComboBox<StoreDto> storeFilter = new ComboBox<>();
    private final ComboBox<FloorDto> floorFilter = new ComboBox<>();
    private final List<FloorDto> floors = new ArrayList<>();
    private final List<Runnable> selectionListeners = new ArrayList<>();
    private final transient StoreCascade storeCascade;

    private boolean suppressEvents;

    /**
     * @param organizationService client loading brands, chains and stores
     * @param settingService      client loading the floors of a store
     * @param accessService       resolves the session brand/chain/store defaults
     */
    public TableFilterBar(AsyncRestClientOrganizationService organizationService,
                          AsyncRestClientSettingService settingService,
                          AccessService accessService) {
        this.settingService = settingService;
        configure();
        this.storeCascade = new StoreCascade(
                organizationService,
                () -> getUI().orElse(null),
                brandFilter, chainFilter, storeFilter,
                new StoreCascade.Defaults(accessService.getBrandId(),
                        accessService.getChainId(), accessService.getStoreId()),
                true);
        storeFilter.addValueChangeListener(event -> {
            if (!suppressEvents) {
                reloadFloors();
            }
        });

        add(brandFilter, chainFilter, storeFilter, floorFilter);
        addClassName("table-filter-bar");
        setSpacing(true);
        setAlignItems(FlexComponent.Alignment.BASELINE);
        setFlexGrow(1, brandFilter);
        setFlexGrow(1, chainFilter);
        setFlexGrow(1, storeFilter);
        setFlexGrow(1, floorFilter);
    }

    private void configure() {
        brandFilter.setLabel(Messages.get(Messages.Keys.LABEL_BRAND));
        brandFilter.setItemLabelGenerator(BrandDto::getName);

        chainFilter.setLabel(Messages.get(Messages.Keys.LABEL_CHAIN));
        chainFilter.setItemLabelGenerator(ChainDto::getName);

        storeFilter.setLabel(Messages.get(Messages.Keys.LABEL_STORE));
        storeFilter.setItemLabelGenerator(StoreDto::getName);

        floorFilter.setLabel(Messages.get("label.floor"));
        floorFilter.setItemLabelGenerator(FloorDto::getName);
        floorFilter.setClearButtonVisible(false);
        floorFilter.setEnabled(false);
        floorFilter.addValueChangeListener(event -> {
            if (!suppressEvents) {
                fireSelectionChanged();
            }
        });
    }

    /**
     * Starts the cascade: brands, then chains, then stores, then floors.
     */
    public void load() {
        storeCascade.load();
    }

    /**
     * Reloads the floors of the currently selected store and notifies the
     * listeners once the new selection is settled. Called after a floor is
     * added, edited or deleted, and whenever the store changes.
     */
    public void reloadFloors() {
        StoreDto store = storeFilter.getValue();
        if (store == null || store.getId() == null) {
            floors.clear();
            suppressEvents = true;
            try {
                floorFilter.setItems(Collections.emptyList());
                floorFilter.clear();
            } finally {
                suppressEvents = false;
            }
            floorFilter.setEnabled(false);
            fireSelectionChanged();
            return;
        }
        settingService.getAllFloorsByStore(
                result -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    floors.clear();
                    if (result != null) {
                        floors.addAll(result);
                    }
                    suppressEvents = true;
                    try {
                        floorFilter.setItems(floors);
                    } finally {
                        suppressEvents = false;
                    }
                    floorFilter.setEnabled(floors.size() > 1);
                    FloorDto selected = defaultFloor();
                    suppressEvents = true;
                    try {
                        floorFilter.setValue(selected);
                    } finally {
                        suppressEvents = false;
                    }
                    fireSelectionChanged();
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null), () ->
                        UiUtil.errorWithRetry(Messages.get("notification.floor.loadFailed"),
                                this::reloadFloors)),
                store.getId());
    }

    private FloorDto defaultFloor() {
        FloorDto current = floorFilter.getValue();
        if (current != null && floors.stream()
                .anyMatch(floor -> Objects.equals(floor.getId(), current.getId()))) {
            return current;
        }
        return floors.isEmpty() ? null : floors.getFirst();
    }

    /**
     * Selects a floor from outside the filter bar (for example a floor tab) and
     * notifies the listeners.
     *
     * @param floor the floor to select
     */
    public void selectFloor(FloorDto floor) {
        if (floor == null) {
            return;
        }
        if (floorFilter.getValue() != null
                && Objects.equals(floorFilter.getValue().getId(), floor.getId())) {
            return;
        }
        floorFilter.setValue(floor);
    }

    public StoreDto getSelectedStore() {
        return storeFilter.getValue();
    }

    public FloorDto getSelectedFloor() {
        return floorFilter.getValue();
    }

    /**
     * @return the floors currently loaded for the selected store
     */
    public List<FloorDto> getFloors() {
        return Collections.unmodifiableList(floors);
    }

    /**
     * Registers a listener fired whenever the store or floor selection changes.
     *
     * @param listener the action to run
     */
    public void addSelectionListener(Runnable listener) {
        if (listener != null) {
            selectionListeners.add(listener);
        }
    }

    private void fireSelectionChanged() {
        selectionListeners.forEach(Runnable::run);
    }
}
