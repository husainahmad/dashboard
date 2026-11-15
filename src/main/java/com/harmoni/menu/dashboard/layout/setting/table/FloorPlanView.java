package com.harmoni.menu.dashboard.layout.setting.table;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.FloorDto;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.dto.TableDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientSettingService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientSettingService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import elemental.json.JsonArray;
import elemental.json.JsonObject;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Visual floor-plan editor for the "Meja &amp; Denah" page.
 *
 * <p>Floors are shown as tabs; the selected floor is drawn as a dotted canvas
 * with one draggable node per table. Selecting a node fills the property panel
 * on the right. Dragging moves a table and the bottom-right handle resizes it;
 * positions and sizes are stored as percentages of the canvas so the plan
 * scales with the screen. "Save plan" persists the whole floor in one batch.</p>
 */
@Slf4j
public class FloorPlanView extends VerticalLayout implements BroadcastMessageService {

    private final transient AsyncRestClientSettingService asyncRestClientSettingService;
    private final transient RestClientSettingService restClientSettingService;
    private final transient AccessService accessService;
    private final transient AsyncRestClientOrganizationService asyncRestClientOrganizationService;

    private final HorizontalLayout floorTabs = new HorizontalLayout();
    private final Div canvas = new Div();
    private final VerticalLayout properties = new VerticalLayout();
    private final VerticalLayout propertiesBody = new VerticalLayout();
    private final VerticalLayout propertiesEmpty = new VerticalLayout();
    private final Span footerNote = new Span();
    private final Span floorStats = new Span();
    private final Button floorEditButton = new Button(new Icon(VaadinIcon.PENCIL));
    private final Button floorDeleteButton = new Button(new Icon(VaadinIcon.TRASH));
    private final Map<Integer, Div> tableNodes = new HashMap<>();

    private final TextField nameField = new TextField(Messages.get("label.tableNumber"));
    private final IntegerField capacityField = new IntegerField(Messages.get("label.capacity"));
    private final Span floorValue = new Span();
    private final Checkbox activeField = new Checkbox(Messages.get("label.activeAtCashier"));
    private final List<Button> shapeButtons = new ArrayList<>();

    private final List<FloorDto> floors = new ArrayList<>();
    private final List<TableDto> tables = new ArrayList<>();

    private FloorDto selectedFloor;
    private TableDto selectedTable;
    private Div selectedNode;
    private String selectedShape = "SQUARE";
    private boolean populatingProperties;
    private boolean rendered;

    public FloorPlanView(AsyncRestClientSettingService asyncRestClientSettingService,
                         RestClientSettingService restClientSettingService,
                         AccessService accessService,
                         AsyncRestClientOrganizationService asyncRestClientOrganizationService) {
        this.asyncRestClientSettingService = asyncRestClientSettingService;
        this.restClientSettingService = restClientSettingService;
        this.accessService = accessService;
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (rendered) {
            return;
        }
        rendered = true;
        buildLayout();
        loadFloors();
    }

    private void buildLayout() {
        setSizeFull();
        setPadding(false);
        setSpacing(true);

        add(buildTopBar());

        properties.addClassName("floor-plan-properties");
        properties.setPadding(true);
        properties.setSpacing(true);
        buildPropertiesPanel();

        Component canvasColumn = canvasWrap();
        HorizontalLayout body = new HorizontalLayout(canvasColumn, properties);
        body.setWidthFull();
        body.setSpacing(true);
        body.setPadding(false);
        body.setAlignItems(FlexComponent.Alignment.STRETCH);
        body.setFlexGrow(1, canvasColumn);
        add(body);
        setFlexGrow(1, body);

        footerNote.addClassName("floor-plan-footer");
        add(footerNote);
    }

    private HorizontalLayout buildTopBar() {
        floorTabs.setAlignItems(FlexComponent.Alignment.CENTER);
        floorTabs.setSpacing(true);
        floorTabs.addClassName("floor-plan-tabs");

        floorStats.addClassName("floor-plan-stats");

        HorizontalLayout floorsBlock = new HorizontalLayout(floorTabs, floorStats);
        floorsBlock.setAlignItems(FlexComponent.Alignment.CENTER);
        floorsBlock.setSpacing(true);
        floorsBlock.setFlexGrow(1, floorTabs);

        floorEditButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON);
        floorEditButton.setTooltipText(Messages.get("action.editFloorInPlan"));
        floorEditButton.setEnabled(false);
        floorEditButton.addClickListener(event -> editSelectedFloor());

        floorDeleteButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ICON,
                ButtonVariant.LUMO_ERROR);
        floorDeleteButton.setTooltipText(Messages.get("action.deleteFloorInPlan"));
        floorDeleteButton.setEnabled(false);
        floorDeleteButton.addClickListener(event -> deleteSelectedFloor());

        Button preview = new Button(Messages.get("action.previewCashier"));
        preview.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        preview.setEnabled(false);
        preview.setTooltipText(Messages.get("tooltip.comingSoon"));

        Button save = new Button(Messages.get("action.saveLayout"), event -> saveLayout());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.setPrefixComponent(VaadinIcon.CHECK.create());

        Button addTable = new Button(Messages.get("action.addTableInPlan"), event -> addTable());
        addTable.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addTable.setPrefixComponent(VaadinIcon.PLUS.create());

        HorizontalLayout floorTools = new HorizontalLayout(floorEditButton, floorDeleteButton);
        floorTools.addClassName("floor-plan-floor-tools");
        floorTools.setAlignItems(FlexComponent.Alignment.CENTER);
        floorTools.setSpacing(false);

        HorizontalLayout actions = new HorizontalLayout(floorTools, preview, save, addTable);
        actions.setAlignItems(FlexComponent.Alignment.CENTER);
        actions.setSpacing(true);

        HorizontalLayout bar = new HorizontalLayout(floorsBlock, actions);
        bar.setWidthFull();
        bar.setAlignItems(FlexComponent.Alignment.CENTER);
        bar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        bar.addClassName("floor-plan-topbar");
        return bar;
    }

    private Component canvasWrap() {
        canvas.addClassName("floor-plan-canvas");
        canvas.setWidthFull();
        Div wrap = new Div(canvas);
        wrap.addClassName("floor-plan-canvas-wrap");
        wrap.setWidthFull();
        wrap.setHeightFull();
        return wrap;
    }

    private void buildPropertiesPanel() {
        Span title = new Span(Messages.get("label.tableProperties"));
        title.addClassName("floor-plan-properties-title");

        Span subtitle = new Span(Messages.get("label.tablePropertiesHint"));
        subtitle.addClassName("floor-plan-properties-subtitle");

        Icon emptyIcon = VaadinIcon.CURSOR.create();
        emptyIcon.addClassName("floor-plan-empty-icon");
        Span emptyTitle = new Span(Messages.get("table.properties.emptyTitle"));
        emptyTitle.addClassName("floor-plan-empty-title");
        Span emptyHint = new Span(Messages.get("table.properties.emptyHint"));
        emptyHint.addClassName("floor-plan-empty-hint");

        propertiesEmpty.add(emptyIcon, emptyTitle, emptyHint);
        propertiesEmpty.addClassName("floor-plan-properties-empty");
        propertiesEmpty.setPadding(false);
        propertiesEmpty.setSpacing(false);

        nameField.setWidthFull();
        nameField.addValueChangeListener(event -> {
            if (populatingProperties || selectedTable == null) {
                return;
            }
            selectedTable.setName(event.getValue());
            updateNodeLabel();
        });

        capacityField.setMin(1);
        capacityField.setMax(999);
        capacityField.setStepButtonsVisible(true);
        capacityField.setWidthFull();
        capacityField.addValueChangeListener(event -> {
            if (populatingProperties || selectedTable == null || event.getValue() == null) {
                return;
            }
            selectedTable.setCapacity(event.getValue());
            updateNodeLabel();
        });

        Span shapeLabel = new Span(Messages.get("label.shape"));
        shapeLabel.addClassName("floor-plan-field-label");
        HorizontalLayout shapeRow = new HorizontalLayout();
        shapeRow.setSpacing(true);
        addShapeButton(shapeRow, "SQUARE", Messages.get("shape.square"));
        addShapeButton(shapeRow, "ROUND", Messages.get("shape.round"));
        addShapeButton(shapeRow, "RECTANGLE", Messages.get("shape.rectangle"));

        Span floorLabel = new Span(Messages.get("label.floor"));
        floorLabel.addClassName("floor-plan-field-label");
        floorValue.addClassName("floor-plan-value");

        activeField.addValueChangeListener(event -> {
            if (populatingProperties || selectedTable == null) {
                return;
            }
            selectedTable.setActive(event.getValue());
            updateNodeState();
        });

        Button duplicate = new Button(Messages.get("action.duplicate"), event -> duplicateTable());
        Button delete = new Button(Messages.get("action.deleteTable"), event -> deleteTable());
        delete.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY);
        HorizontalLayout footer = new HorizontalLayout(duplicate, delete);
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        propertiesBody.add(nameField, capacityField, shapeLabel, shapeRow,
                floorLabel, floorValue, activeField, footer);
        propertiesBody.addClassName("floor-plan-properties-body");
        propertiesBody.setPadding(false);
        propertiesBody.setSpacing(true);
        propertiesBody.setWidthFull();

        properties.add(title, subtitle, propertiesEmpty, propertiesBody);
        setPropertiesEnabled(false);
    }

    private void addShapeButton(HorizontalLayout row, String shape, String label) {
        Button button = new Button(label, event -> selectShape(shape));
        button.addClassName("shape-button");
        shapeButtons.add(button);
        row.add(button);
    }

    private void selectShape(String shape) {
        selectedShape = shape;
        updateShapeButtons();
        if (selectedTable == null) {
            return;
        }
        selectedTable.setShape(shape);
        updateNodeState();
    }

    private void updateShapeButtons() {
        for (Button button : shapeButtons) {
            button.removeThemeVariants(ButtonVariant.LUMO_PRIMARY);
            button.removeClassName("selected");
        }
        int index = shapeIndex(selectedShape);
        if (index >= 0 && index < shapeButtons.size()) {
            shapeButtons.get(index).addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            shapeButtons.get(index).addClassName("selected");
        }
    }

    private int shapeIndex(String shape) {
        return switch (normalizeShape(shape)) {
            case "ROUND" -> 1;
            case "RECTANGLE" -> 2;
            default -> 0;
        };
    }

    private void loadFloors() {
        Integer storeId = sessionStoreId();
        if (storeId == null) {
            return;
        }
        asyncRestClientSettingService.getAllFloorsByStore(
                result -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    floors.clear();
                    if (result != null) {
                        floors.addAll(result);
                    }
                    FloorDto keep = selectedFloor;
                    selectedFloor = floors.stream()
                            .filter(floor -> keep != null && Objects.equals(floor.getId(), keep.getId()))
                            .findFirst()
                            .orElse(floors.isEmpty() ? null : floors.getFirst());
                    renderFloorTabs();
                    loadTables();
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null), () ->
                        UiUtil.errorWithRetry(Messages.get("notification.floor.loadFailed"), this::loadFloors)),
                storeId);
    }

    private void renderFloorTabs() {
        floorTabs.removeAll();
        for (FloorDto floor : floors) {
            Button tab = new Button(floor.getName(), event -> selectFloor(floor));
            tab.addClassName("floor-tab");
            if (selectedFloor != null && Objects.equals(selectedFloor.getId(), floor.getId())) {
                tab.addClassName("selected");
            }
            floorTabs.add(tab);
        }
        Button addFloor = new Button("+ " + Messages.get("label.floor"), event -> addFloor());
        addFloor.addClassName("floor-tab");
        floorTabs.add(addFloor);

        boolean hasFloor = selectedFloor != null || !floors.isEmpty();
        floorEditButton.setEnabled(hasFloor);
        floorDeleteButton.setEnabled(hasFloor);
        floorStats.setText(Messages.get("table.plan.floorCount", floors.size()));
    }

    private void selectFloor(FloorDto floor) {
        if (floor == null || Objects.equals(selectedFloor, floor)) {
            return;
        }
        selectedFloor = floor;
        clearSelection();
        renderFloorTabs();
        loadTables();
    }

    private void loadTables() {
        if (selectedFloor == null || selectedFloor.getId() == null) {
            tables.clear();
            renderCanvas();
            updateFooter();
            return;
        }
        asyncRestClientSettingService.getAllTablesByFloor(
                result -> UiUtil.safeAccess(getUI().orElse(null), () -> {
                    tables.clear();
                    if (result != null) {
                        tables.addAll(result);
                    }
                    clearSelection();
                    renderCanvas();
                    updateFooter();
                }),
                error -> UiUtil.safeAccess(getUI().orElse(null), () ->
                        UiUtil.errorWithRetry(Messages.get(Messages.Keys.NOTIFICATION_TABLE_LOAD_FAILED), this::loadTables)),
                selectedFloor.getId());
    }

    private void renderCanvas() {
        canvas.removeAll();
        tableNodes.clear();
        if (selectedFloor == null) {
            canvas.add(canvasEmptyState("table.plan.noFloors", "table.plan.noFloorsHint"));
            attachPointerHandlers();
            return;
        }
        for (TableDto table : tables) {
            Div node = buildNode(table);
            canvas.add(node);
            tableNodes.put(table.getId(), node);
        }
        if (tables.isEmpty()) {
            canvas.add(canvasEmptyState("table.plan.emptyFloor", "table.plan.emptyFloorHint"));
        }
        addDecorations();
        attachPointerHandlers();
    }

    /**
     * Builds the centred hint drawn over an empty plan.
     *
     * @param titleKey message key for the heading
     * @param hintKey  message key for the supporting line
     * @return the empty-state block
     */
    private Div canvasEmptyState(String titleKey, String hintKey) {
        Icon icon = VaadinIcon.GRID_BIG.create();
        icon.addClassName("floor-plan-empty-icon");
        Span title = new Span(Messages.get(titleKey));
        title.addClassName("floor-plan-empty-title");
        Span hint = new Span(Messages.get(hintKey));
        hint.addClassName("floor-plan-empty-hint");

        Div empty = new Div(icon, title, hint);
        empty.addClassName("floor-plan-canvas-empty");
        return empty;
    }

    private void addDecorations() {
        Div kitchen = new Div(new Span(Messages.get("label.kitchen")));
        kitchen.addClassName("floor-plan-kitchen");
        canvas.add(kitchen);

        Div entrance = new Div(new Span("\u25BC " + Messages.get("label.entrance")));
        entrance.addClassName("floor-plan-entrance");
        canvas.add(entrance);
    }

    private Div buildNode(TableDto table) {
        Div node = new Div();
        node.addClassName("floor-plan-table");
        node.addClassName("shape-" + normalizeShape(table.getShape()));
        if (Boolean.FALSE.equals(table.getActive())) {
            node.addClassName("inactive");
        }
        node.getElement().setAttribute("data-table-id", String.valueOf(table.getId()));
        applyGeometry(node, table);

        Span name = new Span(table.getName() == null ? "" : table.getName());
        name.addClassName("fp-name");
        Span capacity = new Span(seats(table));
        capacity.addClassName("fp-cap");
        Div label = new Div(name, capacity);
        label.addClassName("fp-label");

        Div handle = new Div();
        handle.addClassName("resize-handle");

        node.add(label, handle);
        node.addClickListener(event -> selectTable(table, node));
        return node;
    }

    private void applyGeometry(Div node, TableDto table) {
        node.getElement().getStyle()
                .set("left", percent(table.getPosX(), 10))
                .set("top", percent(table.getPosY(), 10))
                .set("width", percent(table.getWidth(), 12))
                .set("height", percent(table.getHeight(), 12));
    }

    private static String percent(Integer value, int fallback) {
        return (value == null ? fallback : value) + "%";
    }

    private void selectTable(TableDto table, Div node) {
        if (selectedNode != null) {
            selectedNode.removeClassName("selected");
        }
        selectedTable = table;
        selectedNode = node;
        node.addClassName("selected");
        populateProperties(table);
        setPropertiesEnabled(true);
    }

    private void clearSelection() {
        selectedTable = null;
        if (selectedNode != null) {
            selectedNode.removeClassName("selected");
            selectedNode = null;
        }
        setPropertiesEnabled(false);
    }

    private void populateProperties(TableDto table) {
        populatingProperties = true;
        try {
            nameField.setValue(table.getName() == null ? "" : table.getName());
            capacityField.setValue(table.getCapacity());
            activeField.setValue(!Boolean.FALSE.equals(table.getActive()));
            selectedShape = normalizeShape(table.getShape());
            floorValue.setText(selectedFloor == null ? "" : selectedFloor.getName());
            updateShapeButtons();
        } finally {
            populatingProperties = false;
        }
    }

    private void setPropertiesEnabled(boolean enabled) {
        propertiesEmpty.setVisible(!enabled);
        propertiesBody.setVisible(enabled);
        nameField.setEnabled(enabled);
        capacityField.setEnabled(enabled);
        activeField.setEnabled(enabled);
        shapeButtons.forEach(button -> button.setEnabled(enabled));
    }

    private void updateNodeLabel() {
        if (selectedNode == null || selectedTable == null) {
            return;
        }
        selectedNode.getElement().executeJs(
                "const n=this.querySelector('.fp-name'); if(n) n.textContent=$0;", selectedTable.getName());
        selectedNode.getElement().executeJs(
                "const c=this.querySelector('.fp-cap'); if(c) c.textContent=$0;", seats(selectedTable));
    }

    private void updateNodeState() {
        if (selectedNode == null || selectedTable == null) {
            return;
        }
        String shape = normalizeShape(selectedTable.getShape());
        selectedNode.getClassNames().stream()
                .filter(name -> name.startsWith("shape-"))
                .toList()
                .forEach(selectedNode::removeClassName);
        selectedNode.addClassName("shape-" + shape);
        if (Boolean.FALSE.equals(selectedTable.getActive())) {
            selectedNode.addClassName("inactive");
        } else {
            selectedNode.removeClassName("inactive");
        }
    }

    private void addTable() {
        if (selectedFloor == null) {
            UiUtil.error(Messages.get("validation.floor.required"));
            return;
        }
        TableDto table = new TableDto();
        table.setStoreId(sessionStoreId());
        table.setFloorId(selectedFloor.getId());
        table.setName(nextTableName());
        table.setCapacity(2);
        table.setShape("SQUARE");
        table.setPosX(12 + (tables.size() % 4) * 6);
        table.setPosY(12 + (tables.size() / 4) * 14);
        table.setWidth(12);
        table.setHeight(12);
        table.setActive(true);
        restClientSettingService.createTable(table).subscribe(response -> {
            broadcastMessage(BroadcastMessage.TABLE_INSERT_SUCCESS, response);
            UiUtil.safeAccess(getUI().orElse(null), this::loadTables);
        }, error -> UiUtil.safeAccess(getUI().orElse(null), () ->
                UiUtil.error(Messages.get("notification.table.saveFailed"))));
    }

    private String nextTableName() {
        int index = tables.size() + 1;
        while (tableNameExists("T" + index)) {
            index++;
        }
        return "T" + index;
    }

    private boolean tableNameExists(String name) {
        return tables.stream().anyMatch(table -> name.equalsIgnoreCase(table.getName()));
    }

    private void duplicateTable() {
        if (selectedTable == null) {
            return;
        }
        TableDto copy = new TableDto();
        copy.setStoreId(sessionStoreId());
        copy.setFloorId(selectedFloor == null ? selectedTable.getFloorId() : selectedFloor.getId());
        copy.setName(nextCopyName(selectedTable.getName()));
        copy.setCapacity(selectedTable.getCapacity());
        copy.setShape(selectedTable.getShape());
        copy.setPosX(Math.min(88, nz(selectedTable.getPosX(), 10) + 3));
        copy.setPosY(Math.min(88, nz(selectedTable.getPosY(), 10) + 3));
        copy.setWidth(selectedTable.getWidth());
        copy.setHeight(selectedTable.getHeight());
        copy.setActive(selectedTable.getActive());
        restClientSettingService.createTable(copy).subscribe(response -> {
            broadcastMessage(BroadcastMessage.TABLE_INSERT_SUCCESS, response);
            UiUtil.safeAccess(getUI().orElse(null), this::loadTables);
        }, error -> UiUtil.safeAccess(getUI().orElse(null), () ->
                UiUtil.error(Messages.get("notification.table.saveFailed"))));
    }

    private String nextCopyName(String base) {
        String root = (base == null || base.isBlank() ? "T" : base) + " copy";
        if (!tableNameExists(root)) {
            return root;
        }
        int index = 2;
        while (tableNameExists(root + " " + index)) {
            index++;
        }
        return root + " " + index;
    }

    private void deleteTable() {
        if (selectedTable == null) {
            return;
        }
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(Messages.get(Messages.Keys.DIALOG_CONFIRM_TITLE));
        dialog.setText(Messages.get("dialog.confirmDeleteTable", selectedTable.getName()));
        dialog.setCancelable(true);
        dialog.addConfirmListener(event -> restClientSettingService.deleteTable(selectedTable)
                .subscribe(response -> {
                    broadcastMessage(BroadcastMessage.TABLE_DELETED_SUCCESS, response);
                    UiUtil.safeAccess(getUI().orElse(null), () -> {
                        UiUtil.success(Messages.get("notification.table.deleted"));
                        loadTables();
                    });
                }, error -> UiUtil.safeAccess(getUI().orElse(null), () ->
                        UiUtil.error(Messages.get("notification.table.deleteFailed")))));
        dialog.open();
    }

    private void addFloor() {
        StoreDto store = sessionStore();
        if (store == null || store.getId() == null) {
            UiUtil.error(Messages.get("validation.floor.storeRequired"));
            return;
        }
        new FloorFormDialog(new FloorDto(), store, restClientSettingService,
                asyncRestClientOrganizationService, accessService, this::loadFloors).open();
    }

    /**
     * Opens the floor dialog for the selected floor. The store is fixed by the
     * current session, matching the list tab.
     */
    private void editSelectedFloor() {
        FloorDto target = currentFloor();
        if (target == null) {
            UiUtil.error(Messages.get("validation.floor.required"));
            return;
        }
        try {
            StoreDto store = sessionStore();
            new FloorFormDialog(target, store, restClientSettingService,
                    asyncRestClientOrganizationService, accessService, this::loadFloors).open();
        } catch (RuntimeException e) {
            log.error("Could not open the floor editor for floor {}", target.getId(), e);
            UiUtil.error(Messages.get("notification.floor.saveFailed"));
        }
    }

    /**
     * The floor the plan-level actions operate on. Normally the selected tab,
     * falling back to the first loaded floor so the edit control keeps working
     * even if the selection was never restored.
     *
     * @return the floor to edit/delete, or {@code null} when none is loaded
     */
    private FloorDto currentFloor() {
        if (selectedFloor != null) {
            return selectedFloor;
        }
        return floors.isEmpty() ? null : floors.getFirst();
    }

    /**
     * Confirms and deletes the selected floor. The server refuses while tables
     * still stand on it; that refusal is shown as a clear message.
     */
    private void deleteSelectedFloor() {
        FloorDto target = currentFloor();
        if (target == null) {
            UiUtil.error(Messages.get("validation.floor.required"));
            return;
        }
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(Messages.get(Messages.Keys.DIALOG_CONFIRM_TITLE));
        dialog.setText(Messages.get("dialog.confirmDeleteFloor", target.getName()));
        dialog.setCancelable(true);
        dialog.addConfirmListener(event -> restClientSettingService.deleteFloor(target)
                .subscribe(response -> {
                    broadcastMessage(BroadcastMessage.FLOOR_DELETED_SUCCESS, response);
                    UiUtil.safeAccess(getUI().orElse(null), () -> {
                        UiUtil.success(Messages.get("notification.floor.deleted"));
                        selectedFloor = null;
                        loadFloors();
                    });
                }, error -> UiUtil.safeAccess(getUI().orElse(null), () ->
                        UiUtil.error(Messages.get("notification.floor.deleteHasTables")))));
        dialog.open();
    }

    private void saveLayout() {
        canvas.getElement().executeJs("""
                return Array.from(this.querySelectorAll('.floor-plan-table')).map(el => ({
                    id: parseInt(el.dataset.tableId),
                    x: Math.round(parseFloat(el.style.left) || 0),
                    y: Math.round(parseFloat(el.style.top) || 0),
                    w: Math.round(parseFloat(el.style.width) || 0),
                    h: Math.round(parseFloat(el.style.height) || 0)
                }));
                """).then(JsonArray.class, this::persistLayout);
    }

    private void persistLayout(JsonArray layout) {
        Map<Integer, TableDto> byId = tables.stream()
                .filter(table -> table.getId() != null)
                .collect(Collectors.toMap(TableDto::getId, Function.identity(), (a, b) -> a));
        for (int i = 0; i < layout.length(); i++) {
            JsonObject row = layout.getObject(i);
            TableDto table = byId.get((int) row.getNumber("id"));
            if (table == null) {
                continue;
            }
            table.setPosX((int) Math.round(row.getNumber("x")));
            table.setPosY((int) Math.round(row.getNumber("y")));
            table.setWidth((int) Math.round(row.getNumber("w")));
            table.setHeight((int) Math.round(row.getNumber("h")));
        }
        if (tables.isEmpty()) {
            return;
        }
        restClientSettingService.saveTableLayout(new ArrayList<>(tables)).subscribe(response -> {
            broadcastMessage(BroadcastMessage.TABLE_UPDATED_SUCCESS, response);
            UiUtil.safeAccess(getUI().orElse(null), () ->
                    UiUtil.success(Messages.get("notification.layout.saved")));
        }, error -> UiUtil.safeAccess(getUI().orElse(null), () ->
                UiUtil.error(Messages.get("notification.layout.saveFailed"))));
    }

    private void attachPointerHandlers() {
        canvas.getElement().executeJs("""
                const canvas = this;
                canvas.querySelectorAll('.floor-plan-table').forEach(el => {
                    if (el.__fpWired) return;
                    el.__fpWired = true;
                    const handle = el.querySelector('.resize-handle');
                    let mode = null, sx = 0, sy = 0, sl = 0, st = 0, sw = 0, sh = 0, rect = null;

                    el.addEventListener('pointerdown', e => {
                        if (e.target.classList.contains('resize-handle')) return;
                        mode = 'move';
                        rect = canvas.getBoundingClientRect();
                        sx = e.clientX; sy = e.clientY;
                        sl = parseFloat(el.style.left) || 0;
                        st = parseFloat(el.style.top) || 0;
                        el.classList.add('dragging');
                        el.setPointerCapture?.(e.pointerId);
                    });
                    el.addEventListener('pointermove', e => {
                        if (mode !== 'move' || !rect) return;
                        const w = parseFloat(el.style.width) || 12;
                        const h = parseFloat(el.style.height) || 12;
                        const dx = (e.clientX - sx) / rect.width * 100;
                        const dy = (e.clientY - sy) / rect.height * 100;
                        el.style.left = Math.max(0, Math.min(100 - w, sl + dx)) + '%';
                        el.style.top = Math.max(0, Math.min(100 - h, st + dy)) + '%';
                    });
                    el.addEventListener('pointerup', e => {
                        mode = null; rect = null;
                        el.classList.remove('dragging');
                        el.releasePointerCapture?.(e.pointerId);
                    });

                    if (handle) {
                        handle.addEventListener('pointerdown', e => {
                            e.preventDefault();
                            e.stopPropagation();
                            mode = 'resize';
                            rect = canvas.getBoundingClientRect();
                            sx = e.clientX; sy = e.clientY;
                            sw = parseFloat(el.style.width) || 12;
                            sh = parseFloat(el.style.height) || 12;
                            handle.setPointerCapture?.(e.pointerId);
                        });
                        handle.addEventListener('pointermove', e => {
                            if (mode !== 'resize' || !rect) return;
                            const dw = (e.clientX - sx) / rect.width * 100;
                            const dh = (e.clientY - sy) / rect.height * 100;
                            el.style.width = Math.max(6, Math.min(100, sw + dw)) + '%';
                            el.style.height = Math.max(5, Math.min(100, sh + dh)) + '%';
                        });
                        handle.addEventListener('pointerup', e => {
                            mode = null; rect = null;
                            handle.releasePointerCapture?.(e.pointerId);
                        });
                    }
                });
                """);
    }

    private void updateFooter() {
        if (selectedFloor == null) {
            footerNote.setText("");
            return;
        }
        int seats = tables.stream().mapToInt(table -> nz(table.getCapacity(), 0)).sum();
        footerNote.setText(Messages.get("label.floorPlanSummary", tables.size(), seats, selectedFloor.getName()));
    }

    private static int nz(Integer value, int fallback) {
        return value == null ? fallback : value;
    }

    private static String seats(TableDto table) {
        return Messages.get("label.seats", nz(table.getCapacity(), 0));
    }

    private static String normalizeShape(String shape) {
        if (shape == null) {
            return "SQUARE";
        }
        return switch (shape.toUpperCase()) {
            case "ROUND" -> "ROUND";
            case "RECTANGLE" -> "RECTANGLE";
            default -> "SQUARE";
        };
    }

    private StoreDto sessionStore() {
        return accessService.getUserDetail() == null
                ? null : accessService.getUserDetail().getStoreDto();
    }

    private Integer sessionStoreId() {
        StoreDto store = sessionStore();
        return store == null ? null : store.getId();
    }
}
