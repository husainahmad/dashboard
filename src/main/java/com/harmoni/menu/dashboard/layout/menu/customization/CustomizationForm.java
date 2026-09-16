package com.harmoni.menu.dashboard.layout.menu.customization;

import com.harmoni.menu.dashboard.dto.CustomizationDto;
import com.harmoni.menu.dashboard.dto.CustomizationOptionDto;
import com.harmoni.menu.dashboard.event.customization.CustomizationSaveEventListener;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.layout.enums.SelectionType;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class CustomizationForm extends VerticalLayout {

    private static final String ALL = "All";
    private static final String ACTIVE = "Active";
    private static final String INACTIVE = "Inactive";

    @Getter
    private final Binder<CustomizationDto> customizationBinder = new Binder<>(CustomizationDto.class);

    // Form fields
    @Getter private final TextField nameField = new TextField("Name");
    @Getter private final ComboBox<SelectionType> selectionType = new ComboBox<>("Selection Type");

    // Filters
    @Getter private final TextField searchField = new TextField("Search");
    @Getter private final ComboBox<String> statusFilter = new ComboBox<>("Status");

    // Buttons
    private final Button addOptionButton = new Button("+ Add Customization Option");
    private final Button saveButton = new Button("Save");
    private final Button updateButton = new Button("Update");
    private final Button closeButton = new Button("Cancel");

    private final Grid<CustomizationOptionDto> optionGrid = new Grid<>(CustomizationOptionDto.class, false);
    private GridListDataView<CustomizationOptionDto> dataView;
    @Getter
    private final List<CustomizationOptionDto> optionList = new ArrayList<>();
    @Getter
    private final Map<CustomizationOptionDto, Binder<CustomizationOptionDto>> rowBinders = new HashMap<>();
    @Getter
    private UI ui;

    private final RestClientMenuService restClientMenuService;
    private final TabManager tabManager;
    private final Tab currentTab;
    private boolean initialized;

    public CustomizationForm(RestClientMenuService restClientMenuService, TabManager tabManager, Tab currentTab) {
        this.restClientMenuService = restClientMenuService;
        this.tabManager = tabManager;
        this.currentTab = currentTab;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        if (initialized) {
            return;
        }
        initialized = true;
        configureBinder();
        configureForm();
        configureOptionGrid();
        configureOptionFilters();
        configureButtons();
    }

    private void configureBinder() {
        customizationBinder.forField(nameField)
                .asRequired("Customization name is required")
                .bind(CustomizationDto::getName, CustomizationDto::setName);

        customizationBinder.forField(selectionType)
                .asRequired("Selection type is required")
                .bind(CustomizationDto::getSelectionType, CustomizationDto::setSelectionType);
    }

    private void configureForm() {
        setSizeFull();

        selectionType.setItems(SelectionType.values());
        selectionType.setItemLabelGenerator(SelectionType::getLabel);

        statusFilter.setItems(ALL, ACTIVE, INACTIVE);
        statusFilter.setValue(ALL);

        FormLayout formLayout = new FormLayout(nameField, selectionType);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 2)
        );

        HorizontalLayout filterRow = new HorizontalLayout(searchField, statusFilter);
        filterRow.setWidthFull();
        filterRow.setFlexGrow(1, searchField, statusFilter);

        add(
                formLayout,
                addOptionButton,
                filterRow,
                optionGrid,
                createButtonsLayout()
        );

        addOptionButton.addClickListener(e -> {
            if (!canAddNewRow()) {
                showNotification("Please fill in the previous option before adding another.", NotificationVariant.LUMO_ERROR);
                return;
            }
            optionList.add(CustomizationOptionDto.builder().status(ACTIVE).build());
            dataView.refreshAll();
        });

        setFlexGrow(1, optionGrid);
    }

    private boolean canAddNewRow() {
        if (optionList.isEmpty()) return true;

        CustomizationOptionDto last = optionList.getLast();
        Binder<CustomizationOptionDto> binder = rowBinders.get(last);
        if (binder != null) {
            return binder.validate().isOk();
        }
        return last.getName() != null && !last.getName().trim().isEmpty();
    }

    private void configureOptionGrid() {
        optionGrid.setWidthFull();
        optionGrid.setHeight("300px");

        optionGrid.removeAllColumns();
        rowBinders.clear();

        optionGrid.addColumn(new ComponentRenderer<>(option -> {
            Binder<CustomizationOptionDto> binder = rowBinders.computeIfAbsent(option, o -> new Binder<>(CustomizationOptionDto.class));

            TextField nameField = new TextField();
            nameField.setValue(option.getName() != null ? option.getName() : "");
            nameField.setPlaceholder("Option name");

            binder.removeBinding(nameField);
            binder.forField(nameField)
                    .asRequired("Name is required")
                    .bind(CustomizationOptionDto::getName, CustomizationOptionDto::setName);

            binder.setBean(option);
            return nameField;
        }));

        optionGrid.addColumn(new ComponentRenderer<>(option -> {
            ComboBox<String> statusBox = new ComboBox<>();
            statusBox.setItems(ACTIVE, INACTIVE);
            statusBox.setValue(option.getStatus() != null ? option.getStatus() : ACTIVE);
            statusBox.addValueChangeListener(ev -> option.setStatus(ev.getValue()));
            return statusBox;
        })).setHeader("Status").setAutoWidth(true);

        optionGrid.addColumn(new ComponentRenderer<>(option -> {
            Button deleteButton = new Button(VaadinIcon.TRASH.create(), e -> {
                optionList.remove(option);
                rowBinders.remove(option);
                dataView.refreshAll();
            });
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
            deleteButton.setTooltipText("Remove option");
            return deleteButton;
        })).setHeader("Actions").setAutoWidth(true);

        dataView = optionGrid.setItems(optionList);
    }

    private void configureOptionFilters() {
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.setClearButtonVisible(true);
        searchField.addValueChangeListener(event -> applyOptionFilters());
        statusFilter.addValueChangeListener(event -> applyOptionFilters());
    }

    private void applyOptionFilters() {
        String search = searchField.getValue() == null ? "" : searchField.getValue().trim().toLowerCase();
        String status = statusFilter.getValue();
        dataView.setFilter(option -> {
            boolean matchesSearch = search.isEmpty()
                    || option.getName() != null && option.getName().toLowerCase().contains(search);
            boolean matchesStatus = status == null || ALL.equals(status) || Objects.equals(status, option.getStatus());
            return matchesSearch && matchesStatus;
        });
    }

    private void configureButtons() {
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        saveButton.addClickShortcut(Key.ENTER);
        updateButton.addClickShortcut(Key.ENTER);
        closeButton.addClickShortcut(Key.ESCAPE);
        saveButton.addClickListener(
                new CustomizationSaveEventListener(this, restClientMenuService)
        );
        closeButton.addClickListener(event -> closeTab());
    }

    private HorizontalLayout createButtonsLayout() {
        HorizontalLayout horizontalLayout = new HorizontalLayout(saveButton, updateButton, closeButton);
        horizontalLayout.setPadding(true);
        return horizontalLayout;
    }

    private void closeTab() {
        if (tabManager != null && currentTab != null) {
            tabManager.closeAndSelectFirst(currentTab);
        }
    }

    public void onSaveSuccess() {
        if (ui == null) {
            return;
        }
        ui.access(() -> {
            UiUtil.success("Customization saved successfully");
            closeTab();
        });
    }

    public void onSaveError(Throwable error) {
        if (ui == null) {
            return;
        }
        ui.access(() -> UiUtil.error("Unable to save customization"));
    }

    public void showNotification(String text, NotificationVariant variant) {
        if (ui == null) {
            return;
        }
        ui.access(() -> UiUtil.show(text, variant, 3000));
    }

    public boolean validate() {
        return customizationBinder.validate().isOk();
    }

    public CustomizationDto getValue() {
        CustomizationDto dto = CustomizationDto.builder().build();
        customizationBinder.writeBeanIfValid(dto);
        return dto;
    }

    public void restructureButton(FormAction formAction) {
        if (Objects.requireNonNull(formAction) == FormAction.CREATE) {
            saveButton.setVisible(true);
            updateButton.setVisible(false);
        } else if (formAction == FormAction.EDIT) {
            saveButton.setVisible(false);
            updateButton.setVisible(true);
        }
        closeButton.setVisible(true);
    }
}
