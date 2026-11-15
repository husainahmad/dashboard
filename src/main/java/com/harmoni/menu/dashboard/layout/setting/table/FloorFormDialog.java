package com.harmoni.menu.dashboard.layout.setting.table;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.dto.FloorDto;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.exception.BusinessBadRequestException;
import com.harmoni.menu.dashboard.layout.component.StoreCascade;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientSettingService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.BinderValidationStatus;
import org.apache.commons.lang3.ObjectUtils;

/**
 * Create / edit dialog for a floor of a store.
 *
 * <p>Creating a floor needs the store it belongs to, because the API requires
 * {@code storeId}. The create form therefore offers the brand → chain → store
 * cascade and defaults each level to the session store. Editing keeps the
 * store fixed, because moving an existing floor would move its tables too; only
 * the name and active flag can be changed. The order field is hidden on edit
 * and is managed by the floor reordering action instead.</p>
 */
public class FloorFormDialog extends Dialog implements BroadcastMessageService {

    private final transient RestClientSettingService restClientSettingService;
    private final transient AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final transient AccessService accessService;
    private final transient Runnable onSaved;
    private final boolean editing;
    private final StoreDto initialStore;

    private final BeanValidationBinder<FloorDto> binder = new BeanValidationBinder<>(FloorDto.class);
    private final TextField nameField = new TextField(Messages.get("label.floor.name"));
    private final Checkbox activeField = new Checkbox(Messages.get("label.floor.active"));
    private final ComboBox<BrandDto> brandBox = new ComboBox<>(Messages.get(Messages.Keys.LABEL_BRAND));
    private final ComboBox<ChainDto> chainBox = new ComboBox<>(Messages.get(Messages.Keys.LABEL_CHAIN));
    private final ComboBox<StoreDto> storeBox = new ComboBox<>(Messages.get(Messages.Keys.LABEL_STORE));

    private transient StoreCascade cascade;
    private boolean optionsLoaded;

    /**
     * @param floorDto                        the floor to edit, or empty to create one
     * @param storeDto                        the store the dialog opens from
     * @param restClientSettingService        client used to persist the floor
     * @param asyncRestClientOrganizationService client used to load brand/chain/store options
     * @param accessService                   resolves the session store used as default
     * @param onSaved                         callback run after a successful save
     */
    public FloorFormDialog(FloorDto floorDto, StoreDto storeDto,
                           RestClientSettingService restClientSettingService,
                           AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                           AccessService accessService,
                           Runnable onSaved) {
        this.restClientSettingService = restClientSettingService;
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
        this.accessService = accessService;
        this.onSaved = onSaved;
        this.editing = floorDto.getId() != null;
        this.initialStore = storeDto;

        setHeaderTitle(editing
                ? Messages.get(Messages.Keys.ACTION_EDIT_NAME, floorDto.getName())
                : Messages.get("action.newFloor"));
        setWidth("480px");

        configureFields(floorDto, storeDto);
        add(buildForm());

        Button cancelButton = new Button(Messages.get(Messages.Keys.ACTION_CANCEL), event -> close());
        Button saveButton = new Button(Messages.get(Messages.Keys.ACTION_SAVE), event -> save(floorDto));
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(cancelButton, saveButton);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (editing || optionsLoaded) {
            return;
        }
        optionsLoaded = true;
        cascade.load();
    }

    private void configureFields(FloorDto floorDto, StoreDto storeDto) {
        nameField.setWidthFull();
        nameField.setClearButtonVisible(true);

        activeField.setValue(floorDto.getActive() == null || floorDto.getActive());

        cascade = new StoreCascade(asyncRestClientOrganizationService,
                () -> getUI().orElse(null), brandBox, chainBox, storeBox,
                cascadeDefaults(), true);

        binder.forField(nameField)
                .asRequired(Messages.get("validation.floor.name.required"))
                .withValidator(value -> value.trim().length() >= 2 && value.trim().length() <= 60,
                        Messages.get("validation.floor.name.length"))
                .bind(FloorDto::getName, FloorDto::setName);
        binder.forField(storeBox)
                .withValidator(value -> value != null && value.getId() != null,
                        Messages.get("validation.store.required"))
                .bind(FloorDto::getStoreDto, FloorDto::setStoreDto);

        if (editing) {
            StoreDto fixedStore = resolveFixedStore(floorDto, storeDto);
            if (fixedStore != null) {
                storeBox.setItems(fixedStore);
            }
            storeBox.setReadOnly(true);
        }

        binder.readBean(floorDto);

        if (editing) {
            StoreDto fixedStore = resolveFixedStore(floorDto, storeDto);
            if (fixedStore != null) {
                storeBox.setValue(fixedStore);
            }
        }
    }

    private FormLayout buildForm() {
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("360px", 2));

        if (editing) {
            Span caption = new Span(Messages.get("floor.form.editCaption"));
            caption.addClassName("floor-form-caption");
            form.add(caption);
            form.setColspan(caption, 2);

            form.add(storeBox, nameField, activeField);
            form.setColspan(storeBox, 2);
            form.setColspan(activeField, 2);
        } else {
            Span caption = new Span(Messages.get("floor.form.createCaption"));
            caption.addClassName("floor-form-caption");
            form.add(caption);
            form.setColspan(caption, 2);

            form.add(brandBox, chainBox, storeBox, nameField, activeField);
            form.setColspan(storeBox, 2);
            form.setColspan(activeField, 2);
        }
        return form;
    }

    private StoreDto resolveFixedStore(FloorDto floorDto, StoreDto storeDto) {
        if (ObjectUtils.isNotEmpty(floorDto.getStoreDto())
                && ObjectUtils.isNotEmpty(floorDto.getStoreDto().getId())) {
            return floorDto.getStoreDto();
        }
        return ObjectUtils.isNotEmpty(storeDto) ? storeDto : null;
    }

    /**
     * Resolves the brand, chain and store to preselect: the store the dialog was
     * opened from, otherwise the session store.
     *
     * @return the ids to preselect in the cascade
     */
    private StoreCascade.Defaults cascadeDefaults() {
        ChainDto chain = initialStore == null ? null : initialStore.getChainDto();
        Integer brandId = chain != null && chain.getBrandId() != null
                ? chain.getBrandId() : accessService.getBrandId();
        Integer chainId = chain != null && chain.getId() != null
                ? chain.getId() : accessService.getChainId();
        Integer storeId = initialStore != null && initialStore.getId() != null
                ? initialStore.getId() : accessService.getStoreId();
        return new StoreCascade.Defaults(brandId, chainId, storeId);
    }

    private void save(FloorDto floorDto) {
        BinderValidationStatus<FloorDto> status = binder.validate();
        if (status.hasErrors()) {
            return;
        }

        StoreDto store = storeBox.getValue();
        Integer storeId = store != null ? store.getId() : null;
        if (storeId == null) {
            storeId = floorDto.getStoreId();
        }
        if (storeId == null && floorDto.getStoreDto() != null) {
            storeId = floorDto.getStoreDto().getId();
        }
        if (storeId == null) {
            storeBox.setInvalid(true);
            storeBox.setErrorMessage(Messages.get("validation.store.required"));
            return;
        }

        floorDto.setName(nameField.getValue() == null ? "" : nameField.getValue().trim());
        floorDto.setActive(activeField.getValue());
        floorDto.setStoreId(storeId);
        if (store != null) {
            floorDto.setStoreDto(store);
        }
        if (editing) {
            // The order field is managed by the reorder action, so keep whatever
            // the server already has instead of overwriting it on save.
            if (floorDto.getSortOrder() == null) {
                floorDto.setSortOrder(0);
            }
        } else {
            floorDto.setSortOrder(0);
        }
        if (editing) {
            restClientSettingService.updateFloor(floorDto).subscribe(this::accept, this::onError);
        } else {
            restClientSettingService.createFloor(floorDto).subscribe(this::accept, this::onError);
        }
    }

    private void onError(Throwable error) {
        String message = Messages.get("notification.floor.saveFailed");
        if (error instanceof BusinessBadRequestException badRequest) {
            RestAPIResponse response = badRequest.getRestAPIResponse();
            if (response != null && response.getError() != null) {
                message = String.valueOf(response.getError());
            }
        }
        String shown = message;
        getUI().ifPresent(ui -> UiUtil.safeAccess(ui, () -> UiUtil.error(shown)));
    }

    private void accept(RestAPIResponse response) {
        broadcastMessage(editing ? BroadcastMessage.FLOOR_UPDATED_SUCCESS
                : BroadcastMessage.FLOOR_INSERT_SUCCESS, response);
        getUI().ifPresent(ui -> UiUtil.safeAccess(ui, () -> {
            UiUtil.success(Messages.get(editing ? "notification.floor.updated" : "notification.floor.created"));
            close();
            if (onSaved != null) {
                onSaved.run();
            }
        }));
    }

    private Integer sessionBrandId() {
        StoreDto store = sessionStore();
        if (ObjectUtils.isNotEmpty(store) && ObjectUtils.isNotEmpty(store.getChainDto())) {
            return store.getChainDto().getBrandId();
        }
        return null;
    }

    private Integer sessionChainId() {
        StoreDto store = sessionStore();
        if (ObjectUtils.isNotEmpty(store) && ObjectUtils.isNotEmpty(store.getChainDto())) {
            return store.getChainDto().getId();
        }
        return null;
    }

    private Integer sessionStoreId() {
        StoreDto store = sessionStore();
        return ObjectUtils.isNotEmpty(store) ? store.getId() : null;
    }

    private StoreDto sessionStore() {
        return accessService.getUserDetail() == null
                ? null : accessService.getUserDetail().getStoreDto();
    }
}
