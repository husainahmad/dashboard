package com.harmoni.menu.dashboard.layout.organization.store.kitchen;

import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.dto.KitchenCategoryRoutingDto;
import com.harmoni.menu.dashboard.dto.KitchenPrinterDto;
import com.harmoni.menu.dashboard.dto.KitchenProductRoutingDto;
import com.harmoni.menu.dashboard.dto.KitchenRoutingDto;
import com.harmoni.menu.dashboard.dto.KitchenStationDto;
import com.harmoni.menu.dashboard.dto.ProductDto;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.dto.UserDto;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.KitchenRestClientService;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.util.Messages;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Compact, table-first kitchen administration screen. Stations and their printers
 * live in the first tab; category and product routing in the next two.
 */
@RequiredArgsConstructor
@Slf4j
public class KitchenView extends VerticalLayout {

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_INACTIVE = "INACTIVE";

    private final KitchenRestClientService kitchenRestClientService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final AccessService accessService;

    private UI ui;
    private Integer storeId;
    private Integer brandId;

    private final ComboBox<StoreDto> storeBox = new ComboBox<>(Messages.get(Messages.Keys.LABEL_STORE));
    private final Grid<KitchenStationDto> stationGrid = new Grid<>(KitchenStationDto.class);
    private final Grid<KitchenCategoryRoutingDto> categoryRoutingGrid = new Grid<>(KitchenCategoryRoutingDto.class);
    private final Grid<KitchenProductRoutingDto> productRoutingGrid = new Grid<>(KitchenProductRoutingDto.class);

    private final List<StoreDto> stores = new ArrayList<>();
    private final List<KitchenStationDto> stations = new ArrayList<>();
    private final List<CategoryDto> categories = new ArrayList<>();

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.ui = attachEvent.getUI();
        setSizeFull();
        setPadding(false);
        build();
        loadStores();
    }

    private void build() {
        removeAll();

        storeBox.setItemLabelGenerator(StoreDto::getName);
        storeBox.setWidth("280px");
        storeBox.addValueChangeListener(event -> {
            if (event.getValue() != null) {
                storeId = event.getValue().getId();
                loadKitchen();
            }
        });

        Button refresh = new Button("Refresh", event -> loadKitchen());
        HorizontalLayout storeBar = new HorizontalLayout(storeBox, refresh);
        storeBar.setAlignItems(FlexComponent.Alignment.END);
        storeBar.addClassName(Css.TOOLBAR);

        TabSheet tabs = new TabSheet();
        tabs.add(new Tab(Messages.get("tab.kitchen.stations")), buildStationsTab());
        tabs.add(new Tab(Messages.get("tab.kitchen.categoryRouting")), buildCategoryRoutingTab());
        tabs.add(new Tab(Messages.get("tab.kitchen.productRouting")), buildProductRoutingTab());
        tabs.setSizeFull();

        add(storeBar, tabs);
        setFlexGrow(1, tabs);
    }

    private Component buildStationsTab() {
        configureStationGrid();
        Button add = new Button(Messages.get("action.newKitchenStation"),
                event -> openStationDialog(null));
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout toolbar = new HorizontalLayout(add);
        toolbar.addClassName(Css.TOOLBAR);

        VerticalLayout content = new VerticalLayout(toolbar, stationGrid);
        content.setPadding(false);
        content.setSizeFull();
        content.setFlexGrow(1, stationGrid);
        return content;
    }

    private Component buildCategoryRoutingTab() {
        configureCategoryRoutingGrid();
        Button add = new Button("Add Category Route", event -> openCategoryRoutingDialog());
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout toolbar = new HorizontalLayout(add);
        toolbar.addClassName(Css.TOOLBAR);

        VerticalLayout content = new VerticalLayout(toolbar, categoryRoutingGrid);
        content.setPadding(false);
        content.setSizeFull();
        content.setFlexGrow(1, categoryRoutingGrid);
        return content;
    }

    private Component buildProductRoutingTab() {
        configureProductRoutingGrid();
        Button add = new Button("Add Product Route", event -> openProductRoutingDialog());
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        HorizontalLayout toolbar = new HorizontalLayout(add);
        toolbar.addClassName(Css.TOOLBAR);

        VerticalLayout content = new VerticalLayout(toolbar, productRoutingGrid);
        content.setPadding(false);
        content.setSizeFull();
        content.setFlexGrow(1, productRoutingGrid);
        return content;
    }

    private void configureStationGrid() {
        stationGrid.removeAllColumns();
        stationGrid.setSizeFull();
        stationGrid.setEmptyStateText("No kitchen stations yet");
        stationGrid.addColumn(KitchenStationDto::getName).setHeader(Messages.get(Messages.Keys.GRID_HEADER_NAME));
        stationGrid.addColumn(KitchenStationDto::getCode).setHeader("Code");
        stationGrid.addColumn(KitchenStationDto::getStatus).setHeader("Status");
        stationGrid.addColumn(KitchenStationDto::printerCount).setHeader("Printers");
        stationGrid.addColumn(KitchenStationDto::primaryPrinterName).setHeader("Primary printer");
        stationGrid.addComponentColumn(this::stationActions)
                .setHeader(Messages.get(Messages.Keys.GRID_HEADER_ACTION));
        stationGrid.getColumns().forEach(column -> column.setAutoWidth(true));
    }

    private Component stationActions(KitchenStationDto station) {
        Button edit = UiUtil.editButton(event -> openStationDialog(station));
        Button printers = new Button("Printers", event -> openPrintersDialog(station));
        printers.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        Button delete = UiUtil.deleteButton(event -> confirmDelete(
                "Delete kitchen station " + station.getName() + "?",
                () -> kitchenRestClientService.deleteStation(station.getId())
                        .subscribe(response -> onSaved(), this::onError)));
        return new HorizontalLayout(edit, printers, delete);
    }

    private void configureCategoryRoutingGrid() {
        categoryRoutingGrid.removeAllColumns();
        categoryRoutingGrid.setSizeFull();
        categoryRoutingGrid.setEmptyStateText("No category routing yet");
        categoryRoutingGrid.addColumn(KitchenCategoryRoutingDto::getCategoryName).setHeader("Category");
        categoryRoutingGrid.addColumn(KitchenCategoryRoutingDto::getKitchenStationName).setHeader("Kitchen station");
        categoryRoutingGrid.addComponentColumn(routing -> UiUtil.deleteButton(event -> confirmDelete(
                        "Remove routing for " + routing.getCategoryName() + "?",
                        () -> kitchenRestClientService.deleteCategoryRouting(storeId, routing.getCategoryId())
                                .subscribe(response -> onSaved(), this::onError))))
                .setHeader(Messages.get(Messages.Keys.GRID_HEADER_ACTION));
        categoryRoutingGrid.getColumns().forEach(column -> column.setAutoWidth(true));
    }

    private void configureProductRoutingGrid() {
        productRoutingGrid.removeAllColumns();
        productRoutingGrid.setSizeFull();
        productRoutingGrid.setEmptyStateText("No product routing yet");
        productRoutingGrid.addColumn(KitchenProductRoutingDto::getProductName).setHeader("Product");
        productRoutingGrid.addColumn(KitchenProductRoutingDto::getKitchenStationName).setHeader("Kitchen station");
        productRoutingGrid.addComponentColumn(routing -> UiUtil.deleteButton(event -> confirmDelete(
                        "Remove routing for " + routing.getProductName() + "?",
                        () -> kitchenRestClientService.deleteProductRouting(storeId, routing.getProductId())
                                .subscribe(response -> onSaved(), this::onError))))
                .setHeader(Messages.get(Messages.Keys.GRID_HEADER_ACTION));
        productRoutingGrid.getColumns().forEach(column -> column.setAutoWidth(true));
    }

    private void loadStores() {
        UserDto user = accessService.getUserDetail();
        if (user == null || user.getStoreDto() == null || user.getStoreDto().getChainDto() == null) {
            UiUtil.error("No store context for the signed-in user");
            return;
        }
        brandId = user.getStoreDto().getChainDto().getBrandId();
        Integer chainId = user.getStoreDto().getChainDto().getId();

        asyncRestClientOrganizationService.getAllStoreAsync(result -> UiUtil.safeAccess(ui, () -> {
            stores.clear();
            stores.addAll(toList(result.get("data"), StoreDto.class));
            storeBox.setItems(stores);

            StoreDto selected = stores.stream()
                    .filter(store -> Objects.equals(store.getId(), user.getStoreId()))
                    .findFirst()
                    .orElse(stores.isEmpty() ? null : stores.get(0));
            storeBox.setValue(selected);
        }), this::onError, chainId, 1, 100, "");
    }

    private void loadKitchen() {
        if (storeId == null) {
            return;
        }
        loadStations();
        loadRoutings();
        loadCategories();
    }

    private void loadStations() {
        kitchenRestClientService.listStations(storeId).subscribe(response -> UiUtil.safeAccess(ui, () -> {
            stations.clear();
            stations.addAll(toList(response.getData(), KitchenStationDto.class));
            stationGrid.setItems(stations);
        }), this::onError);
    }

    private void loadRoutings() {
        kitchenRestClientService.getRoutings(storeId).subscribe(response -> UiUtil.safeAccess(ui, () -> {
            KitchenRoutingDto routing = ObjectUtil.convertValueToObject(response.getData(), KitchenRoutingDto.class);
            categoryRoutingGrid.setItems(routing == null || routing.getCategories() == null
                    ? List.of() : routing.getCategories());
            productRoutingGrid.setItems(routing == null || routing.getProducts() == null
                    ? List.of() : routing.getProducts());
        }), this::onError);
    }

    private void loadCategories() {
        if (brandId == null) {
            return;
        }
        asyncRestClientMenuService.getAllCategoryAsync(list -> UiUtil.safeAccess(ui, () -> {
            categories.clear();
            if (list != null) {
                categories.addAll(list);
            }
        }), this::onError, brandId);
    }

    private void openStationDialog(KitchenStationDto existing) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null
                ? Messages.get("action.newKitchenStation") : "Edit kitchen station");

        TextField name = new TextField(Messages.get(Messages.Keys.LABEL_NAME));
        name.setRequiredIndicatorVisible(true);
        TextField code = new TextField("Code");
        code.setRequiredIndicatorVisible(true);
        TextArea description = new TextArea(Messages.get(Messages.Keys.LABEL_DESCRIPTION));
        ComboBox<String> status = new ComboBox<>("Status");
        status.setItems(STATUS_ACTIVE, STATUS_INACTIVE);
        status.setValue(STATUS_ACTIVE);

        if (existing != null) {
            name.setValue(existing.getName());
            code.setValue(existing.getCode());
            description.setValue(existing.getDescription());
            status.setValue(existing.getStatus() == null ? STATUS_ACTIVE : existing.getStatus());
        }

        Button save = new Button(Messages.get(Messages.Keys.ACTION_SAVE), event -> {
            if (isBlank(name.getValue()) || isBlank(code.getValue())) {
                UiUtil.error("Name and code are required");
                return;
            }
            KitchenStationDto dto = new KitchenStationDto();
            dto.setName(name.getValue().trim());
            dto.setCode(code.getValue().trim());
            dto.setDescription(description.getValue());
            dto.setStatus(status.getValue());

            Mono<RestAPIResponse> call = existing == null
                    ? kitchenRestClientService.createStation(storeId, dto)
                    : kitchenRestClientService.updateStation(existing.getId(), dto);

            call.subscribe(response -> UiUtil.safeAccess(ui, () -> {
                dialog.close();
                onSaved();
            }), this::onError);
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancel = new Button(Messages.get(Messages.Keys.ACTION_CANCEL), event -> dialog.close());

        VerticalLayout form = new VerticalLayout(name, code, description, status);
        form.setPadding(false);
        dialog.add(form, new HorizontalLayout(save, cancel));
        dialog.setWidth("460px");
        dialog.open();
    }

    private void openPrintersDialog(KitchenStationDto station) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Printers - " + station.getName());
        dialog.setWidth("900px");

        Grid<KitchenPrinterDto> printerGrid = new Grid<>(KitchenPrinterDto.class);
        configurePrinterGrid(printerGrid, station, dialog);

        Button add = new Button(Messages.get("action.newKitchenPrinter"),
                event -> openPrinterForm(station, null, dialog, printerGrid));
        add.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button close = new Button(Messages.get(Messages.Keys.ACTION_CLOSE), event -> dialog.close());

        VerticalLayout content = new VerticalLayout(new HorizontalLayout(add), printerGrid);
        content.setPadding(false);
        content.setSizeFull();
        content.setFlexGrow(1, printerGrid);
        dialog.add(content, new HorizontalLayout(close));
        dialog.open();

        loadPrinters(station.getId(), printerGrid);
    }

    private void configurePrinterGrid(Grid<KitchenPrinterDto> grid, KitchenStationDto station,
                                      Dialog parent) {
        grid.removeAllColumns();
        grid.setSizeFull();
        grid.setEmptyStateText("No printers configured for this station");
        grid.addColumn(KitchenPrinterDto::getName).setHeader(Messages.get(Messages.Keys.LABEL_NAME));
        grid.addColumn(KitchenPrinterDto::getPrinterType).setHeader(Messages.get("label.printerType"));
        grid.addColumn(KitchenPrinterDto::getPaperWidth).setHeader(Messages.get("label.paperWidth"));
        grid.addColumn(printer -> Boolean.TRUE.equals(printer.getIsPrimary()) ? "Yes" : "No")
                .setHeader(Messages.get("label.primaryPrinter"));
        grid.addColumn(KitchenPrinterDto::getStatus).setHeader(Messages.get("label.status"));
        grid.addComponentColumn(printer -> {
            Button edit = UiUtil.editButton(event -> openPrinterForm(station, printer, parent, grid));
            Button delete = UiUtil.deleteButton(event -> confirmDelete(
                    "Delete printer " + printer.getName() + "?",
                    () -> kitchenRestClientService.deletePrinter(printer.getId())
                            .subscribe(response -> {
                                loadPrinters(station.getId(), grid);
                                loadStations();
                            }, this::onError)));
            return new HorizontalLayout(edit, delete);
        }).setHeader(Messages.get(Messages.Keys.GRID_HEADER_ACTION));
        grid.getColumns().forEach(column -> column.setAutoWidth(true));
    }

    private void openPrinterForm(KitchenStationDto station, KitchenPrinterDto existing,
                                 Dialog parent, Grid<KitchenPrinterDto> grid) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null ? "New printer" : "Edit printer");

        TextField name = new TextField(Messages.get(Messages.Keys.LABEL_NAME));
        name.setRequiredIndicatorVisible(true);
        ComboBox<String> type = new ComboBox<>(Messages.get("label.printerType"));
        type.setItems("NETWORK", "USB", "BLUETOOTH");
        type.setValue("NETWORK");
        ComboBox<String> paper = new ComboBox<>(Messages.get("label.paperWidth"));
        paper.setItems("58MM", "80MM");
        paper.setValue("80MM");
        Checkbox primary = new Checkbox(Messages.get("label.primaryPrinter"));
        ComboBox<String> status = new ComboBox<>(Messages.get("label.status"));
        status.setItems(STATUS_ACTIVE, STATUS_INACTIVE);
        status.setValue(STATUS_ACTIVE);
        TextArea connection = new TextArea(Messages.get("label.connectionConfig"));
        connection.setWidthFull();
        connection.setValue("{\"host\":\"192.168.1.50\",\"port\":9100}");
        connection.setHelperText("NETWORK: host/port. USB: queue or device. BLUETOOTH: device.");

        type.addValueChangeListener(event -> {
            if (isBlank(connection.getValue())) {
                connection.setValue(defaultConnectionConfig(event.getValue()));
            }
        });

        if (existing != null) {
            name.setValue(existing.getName());
            type.setValue(existing.getPrinterType());
            paper.setValue(existing.getPaperWidth());
            primary.setValue(Boolean.TRUE.equals(existing.getIsPrimary()));
            status.setValue(existing.getStatus());
            connection.setValue(existing.getConnectionConfig());
        }

        Button save = new Button(Messages.get(Messages.Keys.ACTION_SAVE), event -> {
            if (isBlank(name.getValue()) || isBlank(connection.getValue())) {
                UiUtil.error("Name and connection are required");
                return;
            }
            KitchenPrinterDto dto = new KitchenPrinterDto();
            dto.setName(name.getValue().trim());
            dto.setPrinterType(type.getValue());
            dto.setPaperWidth(paper.getValue());
            dto.setIsPrimary(primary.getValue());
            dto.setStatus(status.getValue());
            dto.setConnectionConfig(connection.getValue());

            Mono<RestAPIResponse> call = existing == null
                    ? kitchenRestClientService.createPrinter(station.getId(), dto)
                    : kitchenRestClientService.updatePrinter(existing.getId(), dto);

            call.subscribe(response -> UiUtil.safeAccess(ui, () -> {
                dialog.close();
                loadPrinters(station.getId(), grid);
                loadStations();
            }), this::onError);
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button(Messages.get(Messages.Keys.ACTION_CANCEL), event -> dialog.close());

        VerticalLayout form = new VerticalLayout(name, type, paper, primary, status, connection);
        form.setPadding(false);
        dialog.add(form, new HorizontalLayout(save, cancel));
        dialog.setWidth("500px");
        dialog.open();
    }

    private void openCategoryRoutingDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add category route");

        ComboBox<CategoryDto> category = new ComboBox<>("Category");
        category.setItems(categories);
        category.setItemLabelGenerator(CategoryDto::getName);
        category.setWidthFull();

        ComboBox<KitchenStationDto> station = new ComboBox<>(Messages.get("label.kitchenStation"));
        station.setItems(stations);
        station.setItemLabelGenerator(KitchenStationDto::getName);
        station.setWidthFull();

        Button save = new Button(Messages.get(Messages.Keys.ACTION_SAVE), event -> {
            if (category.getValue() == null || station.getValue() == null) {
                UiUtil.error("Select a category and a station");
                return;
            }
            KitchenCategoryRoutingDto dto = new KitchenCategoryRoutingDto();
            dto.setCategoryId(category.getValue().getId());
            dto.setKitchenStationId(station.getValue().getId());
            kitchenRestClientService.routeCategory(storeId, dto)
                    .subscribe(response -> UiUtil.safeAccess(ui, () -> {
                        dialog.close();
                        onSaved();
                    }), this::onError);
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button(Messages.get(Messages.Keys.ACTION_CANCEL), event -> dialog.close());

        dialog.add(new VerticalLayout(category, station), new HorizontalLayout(save, cancel));
        dialog.setWidth("460px");
        dialog.open();
    }

    private void openProductRoutingDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Add product route");

        ComboBox<CategoryDto> category = new ComboBox<>("Category");
        category.setItems(categories);
        category.setItemLabelGenerator(CategoryDto::getName);
        category.setWidthFull();

        ComboBox<ProductDto> product = new ComboBox<>("Product");
        product.setItemLabelGenerator(ProductDto::getName);
        product.setWidthFull();
        product.setEnabled(false);

        category.addValueChangeListener(event -> {
            product.clear();
            if (event.getValue() == null || brandId == null) {
                product.setEnabled(false);
                return;
            }
            asyncRestClientMenuService.getAllProductCategoryBrandAsync(result -> UiUtil.safeAccess(ui, () -> {
                product.setItems(toList(result.get("data"), ProductDto.class));
                product.setEnabled(true);
            }), this::onError, event.getValue().getId(), brandId, 1, 200, "");
        });

        ComboBox<KitchenStationDto> station = new ComboBox<>(Messages.get("label.kitchenStation"));
        station.setItems(stations);
        station.setItemLabelGenerator(KitchenStationDto::getName);
        station.setWidthFull();

        Button save = new Button(Messages.get(Messages.Keys.ACTION_SAVE), event -> {
            if (product.getValue() == null || station.getValue() == null) {
                UiUtil.error("Select a product and a station");
                return;
            }
            KitchenProductRoutingDto dto = new KitchenProductRoutingDto();
            dto.setProductId(product.getValue().getId());
            dto.setKitchenStationId(station.getValue().getId());
            kitchenRestClientService.routeProduct(storeId, dto)
                    .subscribe(response -> UiUtil.safeAccess(ui, () -> {
                        dialog.close();
                        onSaved();
                    }), this::onError);
        });
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        Button cancel = new Button(Messages.get(Messages.Keys.ACTION_CANCEL), event -> dialog.close());

        dialog.add(new VerticalLayout(category, product, station), new HorizontalLayout(save, cancel));
        dialog.setWidth("460px");
        dialog.open();
    }

    private void confirmDelete(String message, Runnable onConfirm) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(Messages.get(Messages.Keys.DIALOG_CONFIRM_TITLE));
        dialog.add(new Span(message));
        Button delete = new Button(Messages.get(Messages.Keys.ACTION_DELETE), event -> {
            dialog.close();
            onConfirm.run();
        });
        delete.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);
        Button cancel = new Button(Messages.get(Messages.Keys.ACTION_CANCEL), event -> dialog.close());
        dialog.add(new HorizontalLayout(delete, cancel));
        dialog.open();
    }

    private void loadPrinters(Integer stationId, Grid<KitchenPrinterDto> grid) {
        kitchenRestClientService.listPrinters(stationId).subscribe(response -> UiUtil.safeAccess(ui,
                () -> grid.setItems(toList(response.getData(), KitchenPrinterDto.class))), this::onError);
    }

    private void onSaved() {
        UiUtil.success(Messages.get("notification.kitchen.saved"));
        loadStations();
        loadRoutings();
    }

    private void onError(Throwable error) {
        log.error("Kitchen configuration request failed", error);
        UiUtil.safeAccess(ui, () -> UiUtil.error(Messages.get("notification.kitchen.loadFailed")));
    }

    private static String defaultConnectionConfig(String type) {
        if ("USB".equalsIgnoreCase(type)) {
            return "{\"queue\":\"Kitchen_USB\"}";
        }
        if ("BLUETOOTH".equalsIgnoreCase(type)) {
            return "{\"device\":\"/dev/rfcomm0\"}";
        }
        return "{\"host\":\"192.168.1.50\",\"port\":9100}";
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private static <T> List<T> toList(Object data, Class<T> type) {
        if (!(data instanceof List<?> list)) {
            return List.of();
        }
        List<T> result = new ArrayList<>(list.size());
        for (Object value : list) {
            result.add(ObjectUtil.convertValueToObject(value, type));
        }
        return result;
    }
}
