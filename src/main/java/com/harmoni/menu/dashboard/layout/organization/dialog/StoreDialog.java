package com.harmoni.menu.dashboard.layout.organization.dialog;

import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.dto.TierTypeDto;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Modal dialog for creating a store, matching the "Tambah Store" mockup.
 */
public class StoreDialog extends Dialog {

    private final RestClientOrganizationService restClientOrganizationService;
    private final Runnable onSaved;
    private final Runnable onCreateChain;
    private final StoreDto editingStore;
    private Button addChainButton;

    private final TextField nameField = new TextField(Messages.get("organization.store.name"));
    private final ComboBox<ChainDto> chainBox = new ComboBox<>(Messages.get("organization.store.chain"));
    private final TextField addressField = new TextField(Messages.get("organization.store.address"));
    private final CheckboxGroup<String> orderTypes = new CheckboxGroup<>();
    private final ComboBox<TierDto> tierPriceBox = new ComboBox<>(Messages.get("organization.store.tierPrice"));
    private final ComboBox<TierDto> tierMenuBox = new ComboBox<>(Messages.get("organization.store.tierMenu"));
    private final ComboBox<TierDto> tierServiceBox = new ComboBox<>(Messages.get("organization.store.tierService"));

    public StoreDialog(RestClientOrganizationService restClientOrganizationService,
                       List<ChainDto> chains, ChainDto defaultChain,
                       Runnable onCreateChain, Runnable onSaved) {
        this(restClientOrganizationService, chains, defaultChain, null, Map.of(), onCreateChain, onSaved);
    }

    public StoreDialog(RestClientOrganizationService restClientOrganizationService,
                       List<ChainDto> chains, ChainDto defaultChain, StoreDto editingStore,
                       Runnable onCreateChain, Runnable onSaved) {
        this(restClientOrganizationService, chains, defaultChain, editingStore, Map.of(),
                onCreateChain, onSaved);
    }

    public StoreDialog(RestClientOrganizationService restClientOrganizationService,
                       List<ChainDto> chains, ChainDto defaultChain, StoreDto editingStore,
                       Map<TierTypeDto, List<TierDto>> tiers,
                       Runnable onCreateChain, Runnable onSaved) {
        this.restClientOrganizationService = restClientOrganizationService;
        this.onCreateChain = onCreateChain;
        this.onSaved = onSaved;
        this.editingStore = editingStore;

        setHeaderTitle(Messages.get("organization.store.title"));
        setWidth("560px");

        nameField.setRequired(true);
        nameField.setWidthFull();

        chainBox.setItems(chains);
        chainBox.setItemLabelGenerator(ChainDto::getName);
        chainBox.setValue(defaultChain);
        chainBox.setWidthFull();

        addChainButton = new Button(Messages.get("organization.store.addChain"), event -> {
            close();
            if (onCreateChain != null) {
                onCreateChain.run();
            }
        });
        addChainButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        addChainButton.addClassName("org-inline-add");

        HorizontalLayout chainRow = new HorizontalLayout(chainBox, addChainButton);
        chainRow.setWidthFull();
        chainRow.setAlignItems(com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.END);
        chainRow.setFlexGrow(1, chainBox);

        addressField.setWidthFull();

        orderTypes.addClassName("org-choice-chips");
        orderTypes.setItems("DINE_IN", "TAKEAWAY", "DELIVERY");
        orderTypes.setValue(Set.of("DINE_IN"));
        orderTypes.setItemLabelGenerator(value -> switch (value) {
            case "TAKEAWAY" -> Messages.get("organization.store.takeaway");
            case "DELIVERY" -> Messages.get("organization.store.delivery");
            default -> Messages.get("organization.store.dineIn");
        });

        configureTierBox(tierPriceBox);
        configureTierBox(tierMenuBox);
        configureTierBox(tierServiceBox);

        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
        form.add(nameField);
        form.add(chainRow);
        form.add(addressField);
        form.add(orderTypes);
        if (editingStore != null) {
            form.add(tierPriceBox);
            form.add(tierMenuBox);
            form.add(tierServiceBox);
        }
        add(form);

        Button cancel = new Button(Messages.get("action.cancel"), event -> close());
        cancel.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        Button save = new Button(Messages.get(editingStore == null
                ? "organization.store.create" : "organization.store.update"), event -> save());
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().add(cancel, save);

        if (editingStore != null) {
            applyEdit(editingStore, chains, defaultChain, tiers);
        }
    }

    private void configureTierBox(ComboBox<TierDto> box) {
        box.setItemLabelGenerator(TierDto::getName);
        box.setWidthFull();
        box.setClearButtonVisible(true);
    }

    private void applyEdit(StoreDto store, List<ChainDto> chains, ChainDto defaultChain,
                           Map<TierTypeDto, List<TierDto>> tiers) {
        setHeaderTitle(Messages.get("organization.store.editTitle"));
        nameField.setValue(store.getName() == null ? "" : store.getName());
        addressField.setValue(store.getAddress() == null ? "" : store.getAddress());
        orderTypes.setValue(parseOrderTypes(store.getOrderTypes()));

        Integer chainId = store.getChainId();
        ChainDto chain = chainId == null ? null : chains.stream()
                .filter(item -> chainId.equals(item.getId()))
                .findFirst()
                .orElse(null);
        chainBox.setValue(chain != null ? chain : defaultChain);

        tierPriceBox.setItems(tiers.getOrDefault(TierTypeDto.PRICE, List.of()));
        tierMenuBox.setItems(tiers.getOrDefault(TierTypeDto.MENU, List.of()));
        tierServiceBox.setItems(tiers.getOrDefault(TierTypeDto.SERVICE, List.of()));
        selectTier(tierPriceBox, store.getTierPriceId());
        selectTier(tierMenuBox, store.getTierMenuId());
        selectTier(tierServiceBox, store.getTierServiceId());

        addChainButton.setVisible(false);
    }

    private void selectTier(ComboBox<TierDto> box, Integer tierId) {
        if (tierId == null) {
            return;
        }
        box.getListDataView().getItems()
                .filter(tier -> tierId.equals(tier.getId()))
                .findFirst()
                .ifPresent(box::setValue);
    }

    private Set<String> parseOrderTypes(String orderTypes) {
        if (orderTypes == null || orderTypes.isBlank()) {
            return Set.of("DINE_IN");
        }
        return Arrays.stream(orderTypes.split(","))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .collect(Collectors.toSet());
    }

    private void save() {
        if (nameField.isEmpty() || nameField.getValue().trim().length() < 2) {
            nameField.setInvalid(true);
            nameField.setErrorMessage(Messages.get("validation.name.minLength"));
            return;
        }
        if (chainBox.getValue() == null) {
            chainBox.setInvalid(true);
            chainBox.setErrorMessage(Messages.get("validation.chain.required"));
            return;
        }
        if (addressField.isEmpty()) {
            addressField.setInvalid(true);
            addressField.setErrorMessage(Messages.get("validation.address.required"));
            return;
        }

        StoreDto dto = editingStore == null ? new StoreDto() : copyOf(editingStore);
        dto.setName(nameField.getValue().trim());
        dto.setChainId(chainBox.getValue().getId());
        dto.setAddress(addressField.getValue().trim());
        dto.setOrderTypes(orderTypes.getValue().stream().sorted().collect(Collectors.joining(",")));
        if (editingStore != null) {
            dto.setTierPriceId(tierId(tierPriceBox));
            dto.setTierMenuId(tierId(tierMenuBox));
            dto.setTierServiceId(tierId(tierServiceBox));
        }
        if (editingStore == null) {
            dto.setTaxServiceMode("INHERIT");
            dto.setReceiptLogoMode("INHERIT");
            dto.setMenuPriceMode("INHERIT");
            dto.setOrderTypeMode("INHERIT");
            restClientOrganizationService.createStore(dto).subscribe(
                    response -> success("organization.store.created"),
                    error -> failure("organization.store.failed"));
            return;
        }

        restClientOrganizationService.updateStore(dto).subscribe(
                response -> success("organization.store.updated"),
                error -> failure("organization.store.updateFailed"));
    }

    private Integer tierId(ComboBox<TierDto> box) {
        TierDto tier = box.getValue();
        return tier == null ? null : tier.getId();
    }

    private StoreDto copyOf(StoreDto source) {
        StoreDto dto = new StoreDto();
        dto.setId(source.getId());
        dto.setName(source.getName());
        dto.setChainId(source.getChainId());
        dto.setTierMenuId(source.getTierMenuId());
        dto.setTierPriceId(source.getTierPriceId());
        dto.setTierServiceId(source.getTierServiceId());
        dto.setAddress(source.getAddress());
        dto.setOrderTypes(source.getOrderTypes());
        dto.setTaxServiceMode(source.getTaxServiceMode());
        dto.setTaxPercent(source.getTaxPercent());
        dto.setServiceChargePercent(source.getServiceChargePercent());
        dto.setReceiptLogoMode(source.getReceiptLogoMode());
        dto.setLogoUrl(source.getLogoUrl());
        dto.setMenuPriceMode(source.getMenuPriceMode());
        dto.setOrderTypeMode(source.getOrderTypeMode());
        return dto;
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
