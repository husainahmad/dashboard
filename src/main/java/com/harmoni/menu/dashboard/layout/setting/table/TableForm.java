package com.harmoni.menu.dashboard.layout.setting.table;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.dto.FloorDto;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.dto.TableDto;
import com.harmoni.menu.dashboard.event.table.TableSaveEventListener;
import com.harmoni.menu.dashboard.event.table.TableUpdateEventListener;
import com.harmoni.menu.dashboard.layout.component.StoreCascade;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientSettingService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientSettingService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.HasValidation;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import com.vaadin.flow.shared.Registration;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Tab form for creating or editing a table, binding the name and capacity
 * fields through a {@link BeanValidationBinder}. The footer buttons are wired
 * to {@link TableSaveEventListener} / {@link TableUpdateEventListener}, which
 * persist through the injected {@link RestClientSettingService}; success is
 * awaited from {@link Broadcaster} receipts before the tab closes.
 */
@RequiredArgsConstructor
@Slf4j
public class TableForm extends FormLayout {

    Registration broadcasterRegistration;

    /** Binder that drives field validation and reads/writes the {@link TableDto}. */
    @Getter
    BeanValidationBinder<TableDto> binder = new BeanValidationBinder<>(TableDto.class);

    /** Only true while saving, so the binder only shows errors after Save/Update. */
    private transient boolean saveValidationInProgress;

    /** True while the edit bean is pushed into the fields, so option reloads do not clear them. */
    private transient boolean populatingBean;

    /** Guards the one-time layout build; re-attaching only re-registers the broadcaster. */
    private boolean rendered;

    /** Text field holding the table name. */
    @Getter
    TextField nameField = new TextField(Messages.get(Messages.Keys.LABEL_FIELD_TABLE_NAME));

    /** Integer field holding the seating capacity (1-999). */
    @Getter
    IntegerField capacityField = new IntegerField(Messages.get(Messages.Keys.LABEL_CAPACITY));

    /** Brand whose chains are listed in {@link #chainBox}; drives the chain options. */
    @Getter
    ComboBox<BrandDto> brandBox = new ComboBox<>(Messages.get(Messages.Keys.LABEL_BRAND));

    /** Chain whose stores are listed in {@link #storeBox}; driven by the selected brand. */
    @Getter
    ComboBox<ChainDto> chainBox = new ComboBox<>(Messages.get(Messages.Keys.LABEL_CHAIN));

    /** Store the table belongs to; its id is sent in the save/update payload. */
    @Getter
    ComboBox<StoreDto> storeBox = new ComboBox<>(Messages.get(Messages.Keys.LABEL_STORE));

    /** Floor the table stands on; its id is sent in the save/update payload. */
    @Getter
    ComboBox<FloorDto> floorBox = new ComboBox<>(Messages.get("label.floor"));

    Button saveButton = new Button(Messages.get(Messages.Keys.ACTION_SAVE));
    Button closeButton = new Button(Messages.get(Messages.Keys.ACTION_CANCEL));
    Button updateButton = UiUtil.updateButton();

    /** Live preview parts, updated as the operator edits the form. */
    private final Div previewCard = new Div();
    private final Span previewName = new Span();
    private final Span previewCapacity = new Span();
    private final Span previewFloor = new Span();

    private final TabManager tabManager;
    private final Tab currentTab;
    private final FormAction formAction;

    /** UI this form was attached to, used to marshal callbacks onto the UI thread. */
    @Getter
    UI ui;

    /** The table being edited, empty when creating a new one. */
    @Getter
    private final transient TableDto tableDto;

    private final transient RestClientSettingService restClientSettingService;

    /** Organization client used to load the brand and store options. */
    private final transient AsyncRestClientOrganizationService asyncRestClientOrganizationService;

    /** Settings client used to load the floor options. */
    private final transient AsyncRestClientSettingService asyncRestClientSettingService;

    /** Access service resolving the session store/brand used as default selection. */
    private final transient AccessService accessService;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(this::receiptBroadcast);
        if (rendered) {
            return;
        }
        rendered = true;
        binder.setFieldsValidationStatusChangeListenerEnabled(false);
        binder.setValidationStatusHandler(this::handleValidationStatus);
        addValidation();

        capacityField.setMin(1);
        capacityField.setMax(999);
        capacityField.setStepButtonsVisible(true);

        configureBrandAndStore();

        addClassName("table-form");
        setResponsiveSteps(new ResponsiveStep("0", 1), new ResponsiveStep("720px", 2));

        VerticalLayout locationSection = section(null, null,
                brandBox, chainBox, storeBox, floorBox);
        VerticalLayout detailsSection = section(Messages.get("table.section.details"),
                null, nameField, capacityField);

        VerticalLayout previewBlock = previewSection();

        add(locationSection, detailsSection, previewBlock);
        setColspan(locationSection, 2);
        setColspan(detailsSection, 2);
        setColspan(previewBlock, 2);

        binder.bindInstanceFields(this);
        restructureButton(formAction);

        if (formAction == FormAction.EDIT && ObjectUtils.isNotEmpty(tableDto)) {
            // A ComboBox rejects a non-null value while it has no items, so seed the
            // current store/floor first; the async option load replaces them later.
            seedSelectionItems();
            populatingBean = true;
            try {
                binder.readBean(tableDto);
            } finally {
                populatingBean = false;
            }
            capacityField.setReadOnly(false);
            capacityField.setEnabled(true);
            log.info("TableForm [{}] capacity enabled={} readOnly={}", formAction,
                    capacityField.isEnabled(), capacityField.isReadOnly());
        }

        nameField.addValueChangeListener(event -> updatePreview());
        capacityField.addValueChangeListener(event -> updatePreview());
        floorBox.addValueChangeListener(event -> updatePreview());
        addFooterButtons();
        updatePreview();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    /**
     * Puts the edit target's store and floor into their combo boxes before the
     * bean is read. A {@code ComboBox} refuses a non-null value while it has no
     * items, and the async option load has not run yet at attach time, so
     * without this the read aborts and the form stays empty.
     */
    private void seedSelectionItems() {
        if (ObjectUtils.isNotEmpty(tableDto.getStoreDto())) {
            storeBox.setItems(tableDto.getStoreDto());
        }
        if (ObjectUtils.isNotEmpty(tableDto.getFloorDto())) {
            floorBox.setItems(tableDto.getFloorDto());
        }
    }

    /**
     * Builds one labelled group of fields for the form. The heading and caption
     * are optional, so a section can also be used for fields that need no
     * introduction.
     *
     * @param title   the section heading, or {@code null} to omit it
     * @param caption the explanatory line under the heading, or {@code null}
     * @param fields  the fields belonging to the section
     * @return the section layout
     */
    private static VerticalLayout section(String title, String caption, Component... fields) {
        FormLayout fieldLayout = new FormLayout(fields);
        fieldLayout.addClassName("form-section-fields");
        fieldLayout.setResponsiveSteps(new ResponsiveStep("0", 1),
                new ResponsiveStep("420px", 2));
        fieldLayout.setWidthFull();

        VerticalLayout section = new VerticalLayout();
        if (title != null) {
            H3 heading = new H3(title);
            heading.addClassName("form-section-title");
            section.add(heading);
        }
        if (caption != null) {
            Span note = new Span(caption);
            note.addClassName("form-section-caption");
            section.add(note);
        }
        section.add(fieldLayout);
        section.addClassName("form-section-card");
        section.setPadding(false);
        section.setSpacing(false);
        section.setWidthFull();
        return section;
    }

    /**
     * Builds the live preview block: a miniature table with the name and the
     * two values that identify it at the cashier. It sits under the fields so
     * the operator can confirm the result before saving without leaving the
     * form.
     *
     * @return the preview section
     */
    private VerticalLayout previewSection() {
        Icon shapeIcon = VaadinIcon.TABLE.create();
        shapeIcon.addClassName("table-preview-shape-icon");
        Div shape = new Div(shapeIcon);
        shape.addClassName("table-preview-shape");
        String shapeName = tableDto == null || tableDto.getShape() == null
                ? "SQUARE" : tableDto.getShape().toUpperCase();
        shape.addClassName("shape-" + shapeName);

        previewName.addClassName("table-preview-name");
        previewCapacity.addClassName("table-preview-chip");
        previewFloor.addClassName("table-preview-chip");

        HorizontalLayout meta = new HorizontalLayout(previewCapacity, previewFloor);
        meta.addClassName("table-preview-meta");
        meta.setPadding(false);
        meta.setSpacing(true);
        meta.setAlignItems(FlexComponent.Alignment.CENTER);

        previewCard.add(shape, previewName, meta);
        previewCard.addClassName("table-preview-card");

        H3 heading = new H3(Messages.get("table.form.previewTitle"));
        heading.addClassName("form-section-title");
        Span note = new Span(Messages.get("table.form.previewCaption"));
        note.addClassName("form-section-caption");

        VerticalLayout section = new VerticalLayout(heading, note, previewCard);
        section.addClassName("form-section-card");
        section.addClassName("table-preview-section");
        section.setPadding(false);
        section.setSpacing(false);
        section.setWidthFull();
        return section;
    }

    /**
     * Mirrors the current name, capacity and floor into the preview card.
     */
    private void updatePreview() {
        String name = nameField.getValue();
        previewName.setText(name == null || name.isBlank()
                ? Messages.get("table.form.previewUnnamed") : name);

        Integer capacity = capacityField.getValue();
        previewCapacity.setText(capacity == null
                ? Messages.get("table.form.previewNoCapacity") : Messages.get("label.seats", capacity));

        FloorDto floor = floorBox.getValue();
        previewFloor.setText(floor == null || floor.getName() == null || floor.getName().isBlank()
                ? Messages.get("table.form.previewNoFloor") : floor.getName());
    }

    /**
     * Closes the hosting tab on the UI thread.
     */
    public void close() {
        UiUtil.safeAccess(ui, () -> tabManager.closeAndSelectFirst(currentTab));
    }

    private void addValidation() {
        binder.forField(brandBox)
                .withValidator(value -> value != null, Messages.get(Messages.Keys.VALIDATION_BRAND_REQUIRED))
                .bind(tableDto -> null, (tableDto, value) -> { });

        binder.forField(chainBox)
                .withValidator(value -> value != null, Messages.get("validation.chain.required"))
                .bind(tableDto -> null, (tableDto, value) -> { });

        binder.forField(storeBox)
                .withValidator(value -> value != null, Messages.get("validation.store.required"))
                .bind(TableDto::getStoreDto, TableDto::setStoreDto);

        binder.forField(floorBox)
                .withValidator(value -> value != null, Messages.get("validation.floor.required"))
                .bind(TableDto::getFloorDto, TableDto::setFloorDto);

        binder.forField(nameField)
                .withValidator(value -> {
                    String trimmed = value.trim();
                    return trimmed.length() >= 2 && trimmed.length() <= 45;
                }, Messages.get("validation.table.name.length"))
                .bind(TableDto::getName, TableDto::setName);

        binder.forField(capacityField)
                .withValidator(Objects::nonNull, Messages.get("validation.capacity.required"))
                .withValidator(value -> value >= 1 && value <= 999,
                        Messages.get("validation.capacity.range"))
                .bind(TableDto::getCapacity, TableDto::setCapacity);
    }

    /**
     * Renders validation only when the Save/Update was clicked, otherwise just
     * clears previously shown errors on fields that the user has since fixed.
     */
    private void handleValidationStatus(BinderValidationStatus<TableDto> status) {
        if (saveValidationInProgress) {
            renderFieldErrors(status);
            return;
        }
        status.getFieldValidationStatuses().forEach(result -> {
            if (!result.isError() && result.getField() instanceof HasValidation field) {
                field.setInvalid(false);
            }
        });
    }

    private static void renderFieldErrors(BinderValidationStatus<TableDto> status) {
        status.getFieldValidationStatuses().forEach(result -> {
            if (!(result.getField() instanceof HasValidation field)) {
                return;
            }
            if (result.isError()) {
                field.setInvalid(true);
                result.getMessage().ifPresent(field::setErrorMessage);
            } else {
                field.setInvalid(false);
            }
        });
    }

    /**
     * Validates the form and shows the validation errors; used by the save and
     * update listeners so field errors appear only after the button is clicked.
     *
     * @return the validation outcome for the caller to gate the API request
     */
    public BinderValidationStatus<TableDto> validateOnSave() {
        saveValidationInProgress = true;
        try {
            return binder.validate();
        } finally {
            saveValidationInProgress = false;
        }
    }

    private void configureBrandAndStore() {
        floorBox.setItemLabelGenerator(FloorDto::getName);
        floorBox.setPlaceholder(Messages.get("label.floor"));
        floorBox.setAllowCustomValue(false);

        StoreCascade cascade = new StoreCascade(asyncRestClientOrganizationService, () -> ui,
                brandBox, chainBox, storeBox, cascadeDefaults(), false);
        storeBox.addValueChangeListener(event -> {
            if (!populatingBean) {
                loadFloors();
            }
        });
        cascade.load();
    }

    /**
     * Resolves the brand, chain and store to preselect: the edit target's store
     * when editing, otherwise the session store.
     *
     * @return the ids to preselect in the cascade
     */
    private StoreCascade.Defaults cascadeDefaults() {
        boolean editing = formAction == FormAction.EDIT && ObjectUtils.isNotEmpty(tableDto);
        ChainDto chain = editing && tableDto.getStoreDto() != null
                ? tableDto.getStoreDto().getChainDto() : null;
        Integer brandId = chain != null ? chain.getBrandId() : accessService.getBrandId();
        Integer chainId = chain != null ? chain.getId() : accessService.getChainId();

        Integer storeId = null;
        if (editing) {
            StoreDto store = tableDto.getStoreDto();
            if (store != null && store.getId() != null) {
                storeId = store.getId();
            } else if (tableDto.getStoreId() != null) {
                storeId = tableDto.getStoreId();
            }
        }
        if (storeId == null) {
            storeId = accessService.getStoreId();
        }
        return new StoreCascade.Defaults(brandId, chainId, storeId);
    }

    /**
     * Loads the floors of the currently selected store. The edit target's floor
     * is restored when it is still present in the list.
     */
    private void loadFloors() {
        floorBox.setItems(Collections.emptyList());
        floorBox.clear();
        StoreDto store = storeBox.getValue();
        if (store == null || store.getId() == null) {
            return;
        }
        asyncRestClientSettingService.getAllFloorsByStore(
                result -> UiUtil.safeAccess(ui, () -> {
                    List<FloorDto> floors = result == null ? Collections.emptyList() : result;
                    floorBox.setItems(floors);
                    selectDefaultFloor(floors);
                }),
                error -> UiUtil.safeAccess(ui, () ->
                        UiUtil.errorWithRetry(Messages.get("notification.floor.loadFailed"), this::loadFloors)),
                store.getId());
    }

    private void selectDefaultFloor(List<FloorDto> floors) {
        Integer floorId = Objects.nonNull(tableDto) ? tableDto.getFloorId() : null;
        if (floorId == null && Objects.nonNull(tableDto) && tableDto.getFloorDto() != null) {
            floorId = tableDto.getFloorDto().getId();
        }
        if (floorId == null) {
            return;
        }
        Integer wanted = floorId;
        floors.stream()
                .filter(floor -> Objects.equals(floor.getId(), wanted))
                .findFirst()
                .ifPresent(floorBox::setValue);
    }

    private void addFooterButtons() {
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        saveButton.addClickShortcut(Key.ENTER);
        updateButton.addClickShortcut(Key.ENTER);

        closeButton.addClickShortcut(Key.ESCAPE);

        updateButton.addClickListener(new TableUpdateEventListener(this, restClientSettingService));
        saveButton.addClickListener(new TableSaveEventListener(this, restClientSettingService));
        closeButton.addClickListener(event -> close());

        HorizontalLayout footer = new HorizontalLayout(closeButton, saveButton, updateButton);
        footer.addClassName("table-form-actions");
        footer.setWidthFull();
        footer.setAlignItems(FlexComponent.Alignment.CENTER);
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        add(footer);
        setColspan(footer, 2);
    }

    /**
     * Toggles the footer button visibility for the given action: the save
     * button for CREATE, update/delete for EDIT.
     *
     * @param formAction whether the form creates or edits
     */
    public void restructureButton(FormAction formAction) {
        if (Objects.requireNonNull(formAction) == FormAction.CREATE) {
            saveButton.setVisible(true);
            updateButton.setVisible(false);
            closeButton.setVisible(true);
        } else if (formAction == FormAction.EDIT) {
            saveButton.setVisible(false);
            updateButton.setVisible(true);
            closeButton.setVisible(true);
        }
    }

    private void receiptBroadcast(String message) {
        try {
            BroadcastMessage broadcastMessage =
                    (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
            if (ObjectUtils.isNotEmpty(broadcastMessage) && ObjectUtils.isNotEmpty(broadcastMessage.getType())
                    && (broadcastMessage.getType().equals(BroadcastMessage.TABLE_INSERT_SUCCESS) ||
                        broadcastMessage.getType().equals(BroadcastMessage.TABLE_UPDATED_SUCCESS))) {

                boolean isInsert = broadcastMessage.getType().equals(BroadcastMessage.TABLE_INSERT_SUCCESS);
                UiUtil.safeAccess(ui, () -> {
                    UiUtil.success(isInsert ? Messages.get("notification.table.created") : Messages.get("notification.table.updated"));
                    tabManager.closeAndSelectFirst(currentTab);
                });
            }
        } catch (JsonProcessingException e) {
            log.error("Broadcast Handler Error", e);
        }
    }
}
