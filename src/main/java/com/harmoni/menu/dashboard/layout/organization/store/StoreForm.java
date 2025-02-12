package com.harmoni.menu.dashboard.layout.organization.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.event.store.StoreSaveEventListener;
import com.harmoni.menu.dashboard.event.store.StoreUpdateEventListener;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.accordion.Accordion;
import com.vaadin.flow.component.accordion.AccordionPanel;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.*;


@RequiredArgsConstructor
@Route("store-form")
@Slf4j
public class StoreForm extends FormLayout  {
    Registration broadcasterRegistration;
    BeanValidationBinder<StoreDto> binder = new BeanValidationBinder<>(StoreDto.class);

    TextField storeNameField = new TextField("Store name");
    TextArea storeAddressArea = new TextArea("Address");
    ComboBox<ChainDto> chainDtoComboBox = new ComboBox<>("Chain");
    ComboBox<TierDto> tierPriceBox = new ComboBox<>("Price");
    ComboBox<TierDto> tierMenuBox = new ComboBox<>("Menu");
    ComboBox<TierDto> tierServiceBox = new ComboBox<>("Service");

    Button saveButton = new Button("Save");
    Button closeButton = new Button("Cancel");
    Button updateButton = new Button("Update");

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final Tab storeTab;
    private final FormAction formAction;
    private final transient StoreDto storeDto;
    private final transient Map<String, Object> objectParams;

    UI ui;
    static final int TEMP_BRAND_ID = 1;

    private void renderLayout() {
        setSizeFull();

        chainDtoComboBox.setItems(getChainDtoList(objectParams));
        chainDtoComboBox.setItemLabelGenerator(ChainDto::getName);

        tierPriceBox.setItems(getTierDtoList(objectParams, StoreListView.LIST_TIER_PRICE));
        tierPriceBox.setItemLabelGenerator(TierDto::getName);

        tierMenuBox.setItems(getTierDtoList(objectParams, StoreListView.LIST_TIER_MENU));
        tierMenuBox.setItemLabelGenerator(TierDto::getName);

        tierServiceBox.setItems(getTierDtoList(objectParams, StoreListView.LIST_TIER_SERVICE));
        tierServiceBox.setItemLabelGenerator(TierDto::getName);

        add(chainDtoComboBox);

        add(storeNameField);
        add(storeAddressArea);

        Accordion accordion = new Accordion();
        FormLayout tierFormLayout = new FormLayout();
        tierFormLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new ResponsiveStep("20cm", 2));
        tierFormLayout.add(tierPriceBox);
        tierFormLayout.add(tierMenuBox);
        tierFormLayout.add(tierServiceBox);

        AccordionPanel menuPanel = accordion.add("Tier", tierFormLayout);

        menuPanel.setOpened(true);

        add(accordion);

        add(createButtonsLayout());
        restructureAddOrEdit();

        addValidation();
        binder.bindInstanceFields(this);
        setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1, ResponsiveStep.LabelsPosition.ASIDE));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        this.ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(message -> {
            try {
                BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
                if (ObjectUtils.isNotEmpty(broadcastMessage) && ObjectUtils.isNotEmpty(broadcastMessage.getType())
                        && (broadcastMessage.getType().equals(BroadcastMessage.STORE_INSERT_SUCCESS) ||
                        broadcastMessage.getType().equals(BroadcastMessage.STORE_UPDATED_SUCCESS))) {
                        removeFromSheet();
                    }
            } catch (JsonProcessingException e) {
                log.error("Broadcast Handler Error", e);
            }
        });
        renderLayout();
    }

    public void removeFromSheet() {
        this.ui.access(() -> {
            if (!(this.getParent().orElseThrow() instanceof TabSheet tabSheet)) {
                return;
            }
            tabSheet.remove(storeTab);
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    private void addValidation() {
        chainDtoComboBox.addValueChangeListener(_ -> binder.validate());
        binder.forField(chainDtoComboBox)
                .withValidator(value -> value.getId() > 0, "Chain not allow to be empty"
                ).bind(StoreDto::getChainDto, StoreDto::setChainDto);

        tierPriceBox.addValueChangeListener(_ -> binder.validate());

        binder.forField(storeAddressArea)
                .withValidator(value -> !value.isEmpty(), "Address not allow to be empty"
                ).bind(StoreDto::getAddress, StoreDto::setAddress);

        tierPriceBox.addValueChangeListener(_ -> binder.validate());

        storeNameField.addValueChangeListener(
                (HasValue.ValueChangeListener<AbstractField.ComponentValueChangeEvent<TextField, String>>)
                        _ -> binder.validate());
        binder.forField(storeNameField)
                .withValidator(value -> value.length()>2,
                        "Name must contain at least three characters")
                .bind(StoreDto::getName, StoreDto::setName);
    }

    public StoreDto getStoreDto() {
        storeDto.setName(this.storeNameField.getValue());
        storeDto.setChainId(this.chainDtoComboBox.getValue().getId());
        storeDto.setTierPriceId(this.tierPriceBox.getValue().getId());
        storeDto.setTierMenuId(this.tierMenuBox.getValue().getId());
        storeDto.setTierServiceId(this.tierServiceBox.getValue().getId());
        storeDto.setAddress(this.storeAddressArea.getValue());
        return storeDto;
    }

    private List<ChainDto> getChainDtoList(Map<String, Object> objectParams) {
        Object chainsObject = objectParams.get(StoreListView.LIST_CHAIN);
        if (chainsObject instanceof List<?>) {
            List<ChainDto> list = new ArrayList<>();
            for (Object o : ((List<?>) chainsObject)) {
                if (o instanceof ChainDto chainDto) {
                    list.add(chainDto);
                }
            }
            return list;
        }
        return Collections.emptyList();
    }

    private List<TierDto> getTierDtoList(Map<String, Object> objectParams, String key) {
        Object tierObject = objectParams.get(key);
        if (tierObject instanceof List<?>) {
            List<TierDto> list = new ArrayList<>();
            for (Object o : ((List<?>) tierObject)) {
                if (o instanceof TierDto tierDto) {
                    list.add(tierDto);
                }
            }
            return list;
        }
        return Collections.emptyList();
    }

    private HorizontalLayout createButtonsLayout() {

        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        saveButton.addClickShortcut(Key.ENTER);
        updateButton.addClickShortcut(Key.ENTER);

        closeButton.addClickShortcut(Key.ESCAPE);

        updateButton.addClickListener(
                new StoreUpdateEventListener(this, restClientOrganizationService));
        saveButton.addClickListener(
                new StoreSaveEventListener(this, restClientOrganizationService));
        closeButton.addClickListener(_ -> removeFromSheet());

        HorizontalLayout horizontalLayout = new HorizontalLayout(saveButton, updateButton, updateButton, closeButton);
        horizontalLayout.setPadding(true);

        return horizontalLayout;
    }

    private void restructureAddOrEdit() {
        if (ObjectUtils.isNotEmpty(formAction) && formAction.equals(FormAction.EDIT) && ObjectUtils.isNotEmpty(storeDto)) {
            chainDtoComboBox.getListDataView().getItems()
                    .filter(chainDto -> Objects.equals(chainDto.getId(), storeDto.getChainId()))
                    .findFirst()
                    .ifPresent(chainDtoComboBox::setValue);

            tierPriceBox.getListDataView().getItems()
                    .filter(tierDto -> Objects.equals(tierDto.getId(), storeDto.getTierPriceId()))
                    .findFirst()
                    .ifPresent(tierPriceBox::setValue);

            tierMenuBox.getListDataView().getItems()
                    .filter(tierDto -> Objects.equals(tierDto.getId(), storeDto.getTierMenuId()))
                    .findFirst()
                    .ifPresent(tierMenuBox::setValue);

            tierServiceBox.getListDataView().getItems()
                    .filter(tierDto -> Objects.equals(tierDto.getId(), storeDto.getTierServiceId()))
                    .findFirst()
                    .ifPresent(tierServiceBox::setValue);
            storeNameField.setValue(storeDto.getName());
            storeAddressArea.setValue(storeDto.getAddress());
        }
        restructureButton();
    }

    public void restructureButton() {
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
}
