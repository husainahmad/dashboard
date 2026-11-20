package com.harmoni.menu.dashboard.layout.setting.device;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.harmoni.menu.dashboard.dto.DeviceDto;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.dto.TableDto;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientSettingService;
import com.harmoni.menu.dashboard.service.data.rest.DeviceRestClientService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Create / edit dialog for a store device.
 *
 * <p>Name, address, hardware type and operating mode are always shown. The
 * configuration editor below them changes with the mode: payment methods for a
 * cashier, a multi-table selector for table order, order types for a kiosk and
 * a free-text label for quick order. The payload is sent as a JSON object,
 * matching the shape the menu service validates.</p>
 */
public class DeviceDialog extends Dialog {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private static final String CASHIER_POS = "CASHIER_POS";
    private static final String TABLE_ORDER = "TABLE_ORDER";
    private static final String SELF_ORDER_KIOSK = "SELF_ORDER_KIOSK";
    private static final String QUICK_ORDER = "QUICK_ORDER";

    private static final String CASH = "CASH";
    private static final String CARD = "CARD";
    private static final String QR = "QR";
    private static final String DINE_IN = "DINE_IN";
    private static final String TAKEAWAY = "TAKEAWAY";

    private final DeviceRestClientService deviceRestClientService;
    private final AsyncRestClientSettingService asyncRestClientSettingService;
    private final StoreDto store;
    private final DeviceDto editingDevice;
    private final Runnable onSaved;

    private final TextField nameField = new TextField(Messages.get("label.deviceName"));
    private final TextField addressField = new TextField(Messages.get("label.deviceAddress"));
    private final ComboBox<String> typeBox = new ComboBox<>(Messages.get("label.deviceType"));
    private final ComboBox<String> modeBox = new ComboBox<>(Messages.get("label.deviceMode"));
    private final Checkbox activeField = new Checkbox(Messages.get("label.deviceActive"));

    private final CheckboxGroup<String> payments = new CheckboxGroup<>();
    private final MultiSelectComboBox<TableDto> tableBox = new MultiSelectComboBox<>();
    private final CheckboxGroup<String> orderTypes = new CheckboxGroup<>();
    private final TextField labelField = new TextField(Messages.get("device.config.labelField"));

    private final VerticalLayout configPanel = new VerticalLayout();
    private final List<TableDto> tables = new ArrayList<>();

    public DeviceDialog(DeviceRestClientService deviceRestClientService,
                        AsyncRestClientSettingService asyncRestClientSettingService,
                        StoreDto store, DeviceDto editingDevice, Runnable onSaved) {
        this.deviceRestClientService = deviceRestClientService;
        this.asyncRestClientSettingService = asyncRestClientSettingService;
        this.store = store;
        this.editingDevice = editingDevice;
        this.onSaved = onSaved;

        setHeaderTitle(Messages.get(editingDevice == null
                ? "device.dialog.title.create" : "device.dialog.title.edit"));
        setWidth("560px");

        configureFields();
        add(form());

        Button cancel = new Button(Messages.get(Messages.Keys.ACTION_CANCEL), event -> close());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button save = new Button(Messages.get(editingDevice == null
                ? "device.dialog.create" : "device.dialog.update"), event -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(cancel, save);

        loadTables();
        if (editingDevice != null) {
            applyEdit(editingDevice);
        } else {
            renderConfiguration();
        }
    }

    private void configureFields() {
        nameField.setRequired(true);
        nameField.setWidthFull();
        nameField.getElement().setAttribute("autocomplete", "off");

        addressField.setRequired(true);
        addressField.setWidthFull();
        addressField.setMaxLength(125);
        addressField.getElement().setAttribute("autocomplete", "off");

        typeBox.setItems("TABLET", "ANDROID", "DESKTOP");
        typeBox.setItemLabelGenerator(this::typeLabel);
        typeBox.setValue("TABLET");
        typeBox.setWidthFull();

        modeBox.setItems(CASHIER_POS, TABLE_ORDER, SELF_ORDER_KIOSK, QUICK_ORDER);
        modeBox.setItemLabelGenerator(this::modeLabel);
        modeBox.setValue(CASHIER_POS);
        modeBox.setWidthFull();
        modeBox.addValueChangeListener(event -> renderConfiguration());

        activeField.setValue(true);

        payments.setLabel(Messages.get("device.config.payments"));
        payments.setItems(CASH, CARD, QR);
        payments.setItemLabelGenerator(this::paymentLabel);
        payments.setValue(Set.of(CASH, CARD, QR));

        tableBox.setLabel(Messages.get("device.config.tableField"));
        tableBox.setItemLabelGenerator(TableDto::getName);
        tableBox.setWidthFull();

        orderTypes.setLabel(Messages.get("device.config.orderTypes"));
        orderTypes.setItems(DINE_IN, TAKEAWAY);
        orderTypes.setItemLabelGenerator(this::orderTypeLabel);
        orderTypes.setValue(Set.of(DINE_IN));

        labelField.setLabel(Messages.get("device.config.labelField"));
        labelField.setValue("Counter");
        labelField.setWidthFull();
    }

    private FormLayout form() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("30em", 2));
        form.add(nameField, 2);
        form.add(addressField, 2);
        form.add(typeBox, modeBox);
        form.add(activeField, 2);

        configPanel.setPadding(false);
        configPanel.setSpacing(true);
        configPanel.setWidthFull();
        form.add(configPanel, 2);
        return form;
    }

    /**
     * Swaps the configuration editor for the selected mode.
     */
    private void renderConfiguration() {
        configPanel.removeAll();
        String mode = modeBox.getValue();
        Span hint = new Span(Messages.get("device.config.hint"));
        hint.addClassName("device-config-hint");

        switch (mode == null ? CASHIER_POS : mode) {
            case TABLE_ORDER -> configPanel.add(hint, tableBox);
            case SELF_ORDER_KIOSK -> configPanel.add(hint, orderTypes);
            case QUICK_ORDER -> configPanel.add(hint, labelField);
            default -> configPanel.add(hint, payments);
        }
    }

    private void loadTables() {
        if (store == null || store.getId() == null) {
            return;
        }
        asyncRestClientSettingService.getAllTablesByStore(
                result -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    tables.clear();
                    if (result != null) {
                        tables.addAll(result);
                    }
                    tableBox.setItems(tables);
                    if (editingDevice != null) {
                        applyTableSelection(editingDevice);
                    }
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null), () ->
                        UiUtil.error(Messages.get("notification.table.loadFailed"))),
                store.getId());
    }

    private void applyEdit(DeviceDto device) {
        nameField.setValue(device.getName() == null ? "" : device.getName());
        addressField.setValue(device.getAddress() == null ? "" : device.getAddress());
        if (device.getDeviceType() != null) {
            typeBox.setValue(device.getDeviceType());
        }
        if (device.getDeviceMode() != null) {
            modeBox.setValue(device.getDeviceMode());
        }
        activeField.setValue(device.isActive());
        renderConfiguration();

        DeviceDto.Config config = device.parseConfiguration();
        if (config.getPayments() != null && !config.getPayments().isEmpty()) {
            payments.setValue(new LinkedHashSet<>(config.getPayments()));
        }
        if (config.getOrderTypes() != null && !config.getOrderTypes().isEmpty()) {
            orderTypes.setValue(new LinkedHashSet<>(config.getOrderTypes()));
        }
        if (config.getLabel() != null && !config.getLabel().isBlank()) {
            labelField.setValue(config.getLabel());
        }
        applyTableSelection(device);
    }

    /**
     * Selects the device's tables in the current list. Also called after the
     * asynchronous table load so an edit form opened early still restores its
     * selection.
     *
     * @param device the device being edited, or {@code null} for a new device
     */
    private void applyTableSelection(DeviceDto device) {
        if (device == null) {
            tableBox.setValue(Set.of());
            return;
        }
        Set<Integer> selectedIds = new LinkedHashSet<>(
                device.parseConfiguration().tableIds());
        tableBox.setValue(tables.stream()
                .filter(table -> table.getId() != null && selectedIds.contains(table.getId()))
                .collect(Collectors.toSet()));
    }

    private void save() {
        if (!valid()) {
            return;
        }
        DeviceDto dto = editingDevice == null ? new DeviceDto() : editingDevice;
        dto.setName(nameField.getValue().trim());
        dto.setAddress(addressField.getValue().trim());
        dto.setDeviceType(typeBox.getValue());
        dto.setDeviceMode(modeBox.getValue());
        dto.setStatus(activeField.getValue() ? "ACTIVE" : "INACTIVE");
        dto.setConfiguration(configurationJson());

        if (editingDevice == null) {
            deviceRestClientService.createDevice(store.getId(), dto).subscribe(
                    response -> success("notification.device.created"),
                    error -> failure("notification.device.saveFailed"));
            return;
        }
        deviceRestClientService.updateDevice(dto.getId(), dto).subscribe(
                response -> success("notification.device.updated"),
                error -> failure("notification.device.saveFailed"));
    }

    private boolean valid() {
        boolean valid = true;
        if (nameField.isEmpty() || nameField.getValue().trim().length() < 3) {
            nameField.setInvalid(true);
            nameField.setErrorMessage(Messages.get("validation.device.nameRequired"));
            valid = false;
        }
        if (addressField.isEmpty() || addressField.getValue().trim().isEmpty()) {
            addressField.setInvalid(true);
            addressField.setErrorMessage(Messages.get("validation.device.addressRequired"));
            valid = false;
        }
        String mode = modeBox.getValue();
        if (TABLE_ORDER.equals(mode) && (tableBox.getValue() == null || tableBox.getValue().isEmpty())) {
            tableBox.setInvalid(true);
            tableBox.setErrorMessage(Messages.get("validation.device.tableRequired"));
            valid = false;
        }
        if (CASHIER_POS.equals(mode) && (payments.getValue() == null || payments.getValue().isEmpty())) {
            payments.setInvalid(true);
            payments.setErrorMessage(Messages.get("validation.device.paymentsRequired"));
            valid = false;
        }
        if (SELF_ORDER_KIOSK.equals(mode) && (orderTypes.getValue() == null || orderTypes.getValue().isEmpty())) {
            orderTypes.setInvalid(true);
            orderTypes.setErrorMessage(Messages.get("validation.device.orderTypesRequired"));
            valid = false;
        }
        if (QUICK_ORDER.equals(mode) && (labelField.isEmpty() || labelField.getValue().trim().isEmpty())) {
            labelField.setInvalid(true);
            labelField.setErrorMessage(Messages.get("validation.device.labelRequired"));
            valid = false;
        }
        return valid;
    }

    /**
     * Builds the mode-specific JSON configuration.
     *
     * @return the JSON payload sent to the menu service
     */
    private String configurationJson() {
        ObjectNode node = MAPPER.createObjectNode();
        String mode = modeBox.getValue();
        switch (mode == null ? CASHIER_POS : mode) {
            case TABLE_ORDER -> node.set("tableIds", tableIds(tableBox.getValue()));
            case SELF_ORDER_KIOSK -> node.set("orderTypes", codes(orderTypes.getValue()));
            case QUICK_ORDER -> node.put("label", labelField.getValue().trim());
            default -> node.set("payments", codes(payments.getValue()));
        }
        return node.toString();
    }

    private static ArrayNode codes(Set<String> values) {
        ArrayNode array = MAPPER.createArrayNode();
        if (values != null) {
            values.forEach(array::add);
        }
        return array;
    }

    private static ArrayNode tableIds(Set<TableDto> tables) {
        ArrayNode array = MAPPER.createArrayNode();
        if (tables != null) {
            tables.stream()
                    .map(TableDto::getId)
                    .filter(Objects::nonNull)
                    .sorted()
                    .forEach(array::add);
        }
        return array;
    }

    private String typeLabel(String type) {
        return Messages.get("device.type." + (type == null ? "" : type.toLowerCase()));
    }

    private String modeLabel(String mode) {
        return Messages.get("device.mode." + (mode == null ? "" : mode.toLowerCase()));
    }

    private String paymentLabel(String code) {
        return switch (code) {
            case CARD -> Messages.get("device.payment.card");
            case QR -> Messages.get("device.payment.qris");
            default -> Messages.get("device.payment.cash");
        };
    }

    private String orderTypeLabel(String code) {
        return switch (code) {
            case TAKEAWAY -> Messages.get("device.orderType.takeaway");
            default -> Messages.get("device.orderType.dineIn");
        };
    }

    private void success(String messageKey) {
        UiUtil.safeAccess(getUI().orElse(null), () -> {
            UiUtil.success(Messages.get(messageKey));
            close();
            if (onSaved != null) {
                onSaved.run();
            }
        });
    }

    private void failure(String messageKey) {
        UiUtil.safeAccess(getUI().orElse(null),
                () -> UiUtil.error(Messages.get(messageKey)));
    }
}
