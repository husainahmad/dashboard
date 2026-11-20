package com.harmoni.menu.dashboard.layout.setting.device;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.dto.DeviceDto;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.layout.component.StoreCascade;
import com.harmoni.menu.dashboard.layout.util.ActionPermissions;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.AuthorizationService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientSettingService;
import com.harmoni.menu.dashboard.service.data.rest.DeviceRestClientService;
import com.harmoni.menu.dashboard.util.Messages;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import lombok.RequiredArgsConstructor;
import com.vaadin.flow.shared.Registration;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Device registry for the selected store: a grid of the store's devices with
 * their hardware type, operating mode and mode-specific configuration.
 *
 * <p>The store is picked through the shared brand → chain → store cascade, so
 * the options are filtered and locked by the signed-in user's scope.</p>
 */
@RequiredArgsConstructor
@Slf4j
public class DeviceView extends VerticalLayout implements BroadcastMessageService {

    private static final Set<String> DEVICE_EVENTS = Set.of(
            BroadcastMessage.DEVICE_INSERT_SUCCESS,
            BroadcastMessage.DEVICE_UPDATED_SUCCESS,
            BroadcastMessage.DEVICE_DELETED_SUCCESS);

    private final DeviceRestClientService deviceRestClientService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final AsyncRestClientSettingService asyncRestClientSettingService;
    private final AccessService accessService;
    private final AuthorizationService authorizationService;

    private final ComboBox<BrandDto> brandBox = new ComboBox<>();
    private final ComboBox<ChainDto> chainBox = new ComboBox<>();
    private final ComboBox<StoreDto> storeBox = new ComboBox<>();
    private final Grid<DeviceDto> grid = new Grid<>(DeviceDto.class);

    private final List<DeviceDto> devices = new ArrayList<>();
    private final Button refreshButton = new Button(VaadinIcon.REFRESH.create());
    private final Button addButton = new Button(Messages.get("action.newDevice"), event -> openDialog(null));

    private UI ui;
    private StoreDto selectedStore;
    private boolean built;
    private Registration broadcasterRegistration;
    private StoreCascade cascade;
    private boolean canEdit;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        ui = attachEvent.getUI();
        if (built) {
            return;
        }
        built = true;
        canEdit = ActionPermissions.canEditTerminal(authorizationService);
        setSizeFull();
        setPadding(false);
        setSpacing(true);

        broadcasterRegistration = Broadcaster.register(message -> {
            try {
                BroadcastMessage broadcast =
                        (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
                if (broadcast != null && DEVICE_EVENTS.contains(broadcast.getType())) {
                    UiUtil.safeAccess(ui, this::fetchDevices);
                }
            } catch (JsonProcessingException e) {
                log.error("Device broadcast handler error", e);
            }
        });

        configureGrid();
        add(buildToolbar(), grid);
        setFlexGrow(1, grid);

        cascade = new StoreCascade(asyncRestClientOrganizationService, () -> ui,
                brandBox, chainBox, storeBox,
                new StoreCascade.Defaults(accessService.getBrandId(),
                        accessService.getChainId(), accessService.getStoreId()),
                true);
        storeBox.addValueChangeListener(event -> onStoreChanged(event.getValue()));
        cascade.load();
    }

    private Component buildToolbar() {
        brandBox.setLabel(Messages.get(Messages.Keys.LABEL_BRAND));
        chainBox.setLabel(Messages.get(Messages.Keys.LABEL_CHAIN));
        storeBox.setLabel(Messages.get(Messages.Keys.LABEL_STORE));

        refreshButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        refreshButton.setTooltipText(Messages.get("action.refresh"));
        refreshButton.addClickListener(event -> fetchDevices());

        addButton.setIcon(VaadinIcon.PLUS.create());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.setEnabled(canEdit);
        addButton.setVisible(addButton.isEnabled());

        HorizontalLayout toolbar = new HorizontalLayout(brandBox, chainBox, storeBox, refreshButton, addButton);
        toolbar.addClassName(Css.TOOLBAR);
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.BASELINE);
        toolbar.setFlexGrow(1, brandBox);
        toolbar.setFlexGrow(1, chainBox);
        toolbar.setFlexGrow(1, storeBox);
        return toolbar;
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        if (broadcasterRegistration != null) {
            broadcasterRegistration.remove();
            broadcasterRegistration = null;
        }
    }

    private void configureGrid() {
        grid.removeAllColumns();
        grid.setSizeFull();
        grid.setSelectionMode(Grid.SelectionMode.NONE);
        grid.setEmptyStateText(Messages.get("device.empty"));

        grid.addColumn(DeviceDto::getName)
                .setHeader(Messages.get("grid.header.device"))
                .setKey("device")
                .setFlexGrow(1);
        grid.addColumn(DeviceDto::getAddress)
                .setHeader(Messages.get("grid.header.deviceAddress"))
                .setKey("address")
                .setWidth("160px")
                .setFlexGrow(0);
        grid.addColumn(DeviceDto::typeLabel)
                .setHeader(Messages.get("grid.header.type"))
                .setKey("type")
                .setWidth("140px")
                .setFlexGrow(0);
        grid.addColumn(DeviceDto::modeLabel)
                .setHeader(Messages.get("grid.header.mode"))
                .setKey("mode")
                .setWidth("180px")
                .setFlexGrow(0);
        grid.addColumn(DeviceDto::configurationLabel)
                .setHeader(Messages.get("grid.header.configuration"))
                .setKey("configuration")
                .setFlexGrow(1);
        grid.addComponentColumn(this::actions)
                .setHeader(Messages.get(Messages.Keys.GRID_HEADER_ACTION))
                .setKey("actions")
                .setWidth("120px")
                .setFlexGrow(0);
    }

    private Component actions(DeviceDto device) {
        if (!canEdit) {
            return new Span();
        }
        HorizontalLayout actions = new HorizontalLayout(
                UiUtil.editButton(event -> openDialog(device)),
                UiUtil.deleteButton(event -> confirmDelete(device)));
        actions.setSpacing(false);
        return actions;
    }

    private void onStoreChanged(StoreDto store) {
        selectedStore = store;
        devices.clear();
        grid.setItems(devices);
        if (store != null && store.getId() != null) {
            fetchDevices();
        }
    }

    private void fetchDevices() {
        StoreDto store = storeBox.getValue();
        if (store == null || store.getId() == null) {
            devices.clear();
            grid.setItems(devices);
            return;
        }
        deviceRestClientService.listDevices(store.getId()).subscribe(
                response -> UiUtil.safeAccess(ui, () -> {
                    devices.clear();
                    if (response != null && response.getData() != null) {
                        devices.addAll(ObjectUtil.convertObjectToObject(response.getData(),
                                new com.fasterxml.jackson.core.type.TypeReference<List<DeviceDto>>() { }));
                    }
                    grid.setItems(devices);
                }),
                error -> UiUtil.safeAccess(ui, () ->
                        UiUtil.errorWithRetry(Messages.get("notification.device.loadFailed"), this::fetchDevices)));
    }

    private void openDialog(DeviceDto device) {
        StoreDto store = storeBox.getValue();
        if (store == null || store.getId() == null) {
            UiUtil.error(Messages.get("validation.device.storeRequired"));
            return;
        }
        new DeviceDialog(deviceRestClientService, asyncRestClientSettingService,
                store, device, this::onSaved).open();
    }

    private void onSaved() {
        fetchDevices();
    }

    private void confirmDelete(DeviceDto device) {
        if (device == null || device.getId() == null) {
            return;
        }
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(Messages.get(Messages.Keys.DIALOG_CONFIRM_TITLE));
        dialog.setText(Messages.get("dialog.confirmDeleteDevice", device.getName()));
        dialog.setCancelable(true);
        dialog.setConfirmText(Messages.get(Messages.Keys.ACTION_DELETE));
        dialog.setConfirmButtonTheme("error primary");
        dialog.addConfirmListener(event -> deviceRestClientService.deleteDevice(device.getId()).subscribe(
                response -> UiUtil.safeAccess(ui, () -> {
                    UiUtil.success(Messages.get("notification.device.deleted"));
                    fetchDevices();
                }),
                error -> UiUtil.safeAccess(ui, () ->
                        UiUtil.error(Messages.get("notification.device.deleteFailed")))));
        dialog.open();
    }
}
