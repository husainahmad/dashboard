package com.harmoni.menu.dashboard.layout.menu.customization;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.CustomizationDto;
import com.harmoni.menu.dashboard.dto.CustomizationOptionDto;
import com.harmoni.menu.dashboard.event.customization.CustomizationSaveEventListener;
import com.harmoni.menu.dashboard.layout.enums.SelectionType;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.*;

@RequiredArgsConstructor
@Route("customization-form")
@Slf4j
public class CustomizationForm extends VerticalLayout {

    private Registration broadcasterRegistration;

    @Getter
    private Binder<CustomizationDto> customizationBinder = new Binder<>(CustomizationDto.class);

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

    private static final String SINGLE = "Single";
    private static final String MULTIPLE = "Multiple";
    private static final String ALL = "All";
    private static final String ACTIVE = "Active";
    private static final String INACTIVE = "Inactive";

    private final Grid<CustomizationOptionDto> optionGrid = new Grid<>(CustomizationOptionDto.class, false);
    private GridListDataView<CustomizationOptionDto> dataView;
    @Getter
    private final List<CustomizationOptionDto> optionList = new ArrayList<>();
    @Getter
    private final Map<CustomizationOptionDto, Binder<CustomizationOptionDto>> rowBinders = new HashMap<>();
    @Getter
    private UI ui;

    @Getter
    private transient CustomizationDto customizationDto;

    private final RestClientMenuService restClientMenuService;
    private final TabSheet tabSheet;
    private final Tab currentTab;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        registerBroadcaster();
        configureBinder();
        configureForm();
        configureOptionGrid();
        configureButtons();
        loadSampleData();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        if (broadcasterRegistration != null) {
            broadcasterRegistration.remove();
            broadcasterRegistration = null;
        }
    }

    private void registerBroadcaster() {
        broadcasterRegistration = Broadcaster.register(message -> {
            try {
                BroadcastMessage broadcastMessage =
                        (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
                if (Objects.nonNull(broadcastMessage) && Objects.nonNull(broadcastMessage.getType())) {
                    if (!broadcastMessage.getType().equals(BroadcastMessage.CUSTOMIZATION_INSERT_SUCCESS)
                            && !broadcastMessage.getType().equals(BroadcastMessage.CUSTOMIZATION_UPDATED_SUCCESS)) {
                        return;
                    }
                    ui.access(() -> {
                        showNotification("Customization saved..", NotificationVariant.LUMO_SUCCESS);

                        // Close this tab
                        if (tabSheet != null && currentTab != null) {
                            tabSheet.remove(currentTab);
                        }
                    });
                }
            } catch (JsonProcessingException e) {
                log.error("Broadcast Handler Error", e);
            }
        });
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
            rowBinders.clear();
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
            return binder.validate().isOk(); // uses "Name is required"
        }
        // fallback in case not rendered yet
        return last.getName() != null && !last.getName().trim().isEmpty();
    }

    private void configureOptionGrid() {
        optionGrid.setWidthFull();
        optionGrid.setHeight("300px");

        optionGrid.removeAllColumns();
        rowBinders.clear();

        optionGrid.addColumn(new ComponentRenderer<>(option -> {
            // reuse the same binder + field if already created
            Binder<CustomizationOptionDto> binder = rowBinders.computeIfAbsent(option, o -> new Binder<>(CustomizationOptionDto.class));

            TextField nameField = new TextField();
            nameField.setValue(option.getName() != null ? option.getName() : "");
            nameField.setPlaceholder("Option name");

            // clear previous bindings before binding new
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
            Button deleteButton = new Button("✕", e -> {
                optionList.remove(option);
                dataView.refreshAll();
            });
            deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            return deleteButton;
        })).setHeader("Actions").setAutoWidth(true);

        dataView = optionGrid.setItems(optionList);
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

        closeButton.addClickListener(event -> {
            if (tabSheet != null && currentTab != null) {
                tabSheet.remove(currentTab);
            }
        });
    }

    private HorizontalLayout createButtonsLayout() {
        HorizontalLayout horizontalLayout = new HorizontalLayout(saveButton, updateButton, closeButton);
        horizontalLayout.setPadding(true);
        return horizontalLayout;
    }

    private void loadSampleData() {
        optionGrid.setItems(optionList);
    }

    public void showNotification(String text, NotificationVariant variant) {
        ui.access(() -> {
            Notification notification = new Notification(text, 3000, Notification.Position.MIDDLE);
            notification.addThemeVariants(variant);
            notification.open();
        });
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
