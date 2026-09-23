package com.harmoni.menu.dashboard.layout.setting.table;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.dto.TableDto;
import com.harmoni.menu.dashboard.event.table.TableSaveEventListener;
import com.harmoni.menu.dashboard.event.table.TableUpdateEventListener;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientSettingService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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

    private static final int PAGE_SIZE = 1000;

    Registration broadcasterRegistration;

    /** Binder that drives field validation and reads/writes the {@link TableDto}. */
    @Getter
    BeanValidationBinder<TableDto> binder = new BeanValidationBinder<>(TableDto.class);

    /** Only true while saving, so the binder only shows errors after Save/Update. */
    private transient boolean saveValidationInProgress;

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
    ComboBox<ChainDto> chainBox = new ComboBox<>(Messages.get("label.chain"));

    /** Store the table belongs to; its id is sent in the save/update payload. */
    @Getter
    ComboBox<StoreDto> storeBox = new ComboBox<>(Messages.get(Messages.Keys.LABEL_STORE));

    Button saveButton = new Button(Messages.get(Messages.Keys.ACTION_SAVE));
    Button closeButton = new Button(Messages.get(Messages.Keys.ACTION_CANCEL));
    Button updateButton = UiUtil.updateButton();

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

    /** Access service resolving the session store/brand used as default selection. */
    private final transient AccessService accessService;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(this::receiptBroadcast);
        binder.setFieldsValidationStatusChangeListenerEnabled(false);
        binder.setValidationStatusHandler(this::handleValidationStatus);
        addValidation();

        capacityField.setMin(1);
        capacityField.setMax(999);
        capacityField.setStepButtonsVisible(true);

        configureBrandAndStore();

        add(brandBox);
        add(chainBox);
        add(storeBox);
        add(nameField);
        add(capacityField);
        binder.bindInstanceFields(this);
        restructureButton(formAction);

        if (formAction == FormAction.EDIT && ObjectUtils.isNotEmpty(tableDto)) {
            binder.readBean(tableDto);
        }

        addFooterButtons();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
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
        brandBox.setItemLabelGenerator(BrandDto::getName);
        brandBox.setPlaceholder(Messages.get("placeholder.selectBrand"));
        brandBox.setAllowCustomValue(false);

        chainBox.setItemLabelGenerator(ChainDto::getName);
        chainBox.setPlaceholder(Messages.get("label.chain"));
        chainBox.setAllowCustomValue(false);

        storeBox.setItemLabelGenerator(StoreDto::getName);
        storeBox.setPlaceholder(Messages.get("placeholder.selectStore"));
        storeBox.setAllowCustomValue(false);

        brandBox.addValueChangeListener(event -> loadChains());
        chainBox.addValueChangeListener(event -> loadStores());
        loadBrands();
    }

    private void loadBrands() {
        asyncRestClientOrganizationService.getAllBrandAsync(
                result -> UiUtil.safeAccess(ui, () -> {
                    if (ObjectUtils.isEmpty(result)) {
                        return;
                    }
                    brandBox.setItems(result);
                    selectDefaultBrand();
                }),
                error -> UiUtil.safeAccess(ui, () ->
                        UiUtil.errorWithRetry(Messages.get(Messages.Keys.NOTIFICATION_BRAND_LOAD_FAILED), this::loadBrands)));
    }

    private void selectDefaultBrand() {
        Integer brandId = resolveDefaultBrandId();
        if (ObjectUtils.isEmpty(brandId)) {
            return;
        }
        brandBox.getListDataView().getItems()
                .filter(brand -> Objects.equals(brand.getId(), brandId))
                .findFirst()
                .ifPresent(brandBox::setValue);
    }

    private Integer resolveDefaultBrandId() {
        if (formAction == FormAction.EDIT && ObjectUtils.isNotEmpty(tableDto)
                && ObjectUtils.isNotEmpty(tableDto.getStoreDto())
                && ObjectUtils.isNotEmpty(tableDto.getStoreDto().getChainDto())) {
            return tableDto.getStoreDto().getChainDto().getBrandId();
        }
        StoreDto sessionStore = accessService.getUserDetail().getStoreDto();
        if (ObjectUtils.isNotEmpty(sessionStore) && ObjectUtils.isNotEmpty(sessionStore.getChainDto())) {
            return sessionStore.getChainDto().getBrandId();
        }
        return null;
    }

    private void loadChains() {
        BrandDto brand = brandBox.getValue();
        chainBox.setItems(Collections.emptyList());
        chainBox.clear();
        storeBox.setItems(Collections.emptyList());
        storeBox.clear();
        if (brand == null || brand.getId() == null) {
            return;
        }
        asyncRestClientOrganizationService.getAllChainByBrandIdAsync(
                result -> UiUtil.safeAccess(ui, () -> {
                    if (ObjectUtils.isEmpty(result)) {
                        return;
                    }
                    chainBox.setItems(result);
                    selectDefaultChain();
                }),
                error -> UiUtil.safeAccess(ui, () ->
                        UiUtil.errorWithRetry(Messages.get("notification.chain.loadFailed"), this::loadChains)),
                brand.getId());
    }

    private void selectDefaultChain() {
        Integer chainId = resolveDefaultChainId();
        if (ObjectUtils.isEmpty(chainId)) {
            return;
        }
        chainBox.getListDataView().getItems()
                .filter(chain -> Objects.equals(chain.getId(), chainId))
                .findFirst()
                .ifPresent(chainBox::setValue);
    }

    private Integer resolveDefaultChainId() {
        if (formAction == FormAction.EDIT && ObjectUtils.isNotEmpty(tableDto)
                && ObjectUtils.isNotEmpty(tableDto.getStoreDto())
                && ObjectUtils.isNotEmpty(tableDto.getStoreDto().getChainDto())) {
            return tableDto.getStoreDto().getChainDto().getId();
        }
        StoreDto sessionStore = accessService.getUserDetail().getStoreDto();
        if (ObjectUtils.isNotEmpty(sessionStore) && ObjectUtils.isNotEmpty(sessionStore.getChainDto())) {
            return sessionStore.getChainDto().getId();
        }
        return null;
    }

    private void loadStores() {
        ChainDto chain = chainBox.getValue();
        storeBox.setItems(Collections.emptyList());
        storeBox.clear();
        if (chain == null || chain.getId() == null) {
            return;
        }
        asyncRestClientOrganizationService.getAllStoreAsync(
                result -> UiUtil.safeAccess(ui, () -> {
                    storeBox.setItems(extractStores(result));
                    restoreStoreSelection();
                }),
                error -> UiUtil.safeAccess(ui, () ->
                        UiUtil.errorWithRetry(Messages.get("notification.store.loadFailed"), this::loadStores)),
                chain.getId(), 1, PAGE_SIZE, "");
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

    private void restoreStoreSelection() {
        Integer storeId = resolveDefaultStoreId();
        if (ObjectUtils.isEmpty(storeId)) {
            return;
        }
        storeBox.getListDataView().getItems()
                .filter(store -> Objects.equals(store.getId(), storeId))
                .findFirst()
                .ifPresent(storeBox::setValue);
    }

    private Integer resolveDefaultStoreId() {
        if (formAction == FormAction.EDIT && ObjectUtils.isNotEmpty(tableDto)) {
            if (ObjectUtils.isNotEmpty(tableDto.getStoreDto())
                    && ObjectUtils.isNotEmpty(tableDto.getStoreDto().getId())) {
                return tableDto.getStoreDto().getId();
            }
            if (ObjectUtils.isNotEmpty(tableDto.getStoreId())) {
                return tableDto.getStoreId();
            }
        }
        StoreDto sessionStore = accessService.getUserDetail().getStoreDto();
        if (ObjectUtils.isNotEmpty(sessionStore)) {
            return sessionStore.getId();
        }
        return null;
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

        HorizontalLayout footer = new HorizontalLayout(saveButton, updateButton, closeButton);
        footer.setWidthFull();
        add(footer);
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
