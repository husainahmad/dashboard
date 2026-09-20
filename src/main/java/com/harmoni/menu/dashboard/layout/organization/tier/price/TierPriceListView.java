package com.harmoni.menu.dashboard.layout.organization.tier.price;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.dto.TierTypeDto;
import com.harmoni.menu.dashboard.event.tier.TierDeleteEventListener;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.LoadingBar;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.Registration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Vaadin grid view listing the price tiers (type {@code PRICE}) of the current
 * user's brand. Refreshes on TIER BROADCAST insert/update/delete and hosts an
 * inline {@link TierPriceForm} editor for add/edit.
 */
@RequiredArgsConstructor
@Slf4j
public class TierPriceListView extends VerticalLayout {

    Registration broadcasterRegistration;
    private final Grid<TierDto> tierDtoGrid = new Grid<>(TierDto.class);
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final AccessService accessService;

    private final TextField filterText = new TextField();
    private UI ui;
    private TierPriceForm tierForm;
    private final LoadingBar loadingBar = new LoadingBar();

    private void renderLayout() {
        setSizeFull();
        setPadding(false);
        configureGrid();
        configureForm();

        add(loadingBar, getContent());
        closeEditor();

        fetchTier();
    }

    private void closeEditor() {
        tierForm.setVisible(false);
        removeClassName("editing");
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(message -> {

            try {
                BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
                if (org.apache.commons.lang3.ObjectUtils.isNotEmpty(broadcastMessage)
                        && org.apache.commons.lang3.ObjectUtils.isNotEmpty(broadcastMessage.getType())
                        && (broadcastMessage.getType().equals(BroadcastMessage.TIER_INSERT_SUCCESS) ||
                            broadcastMessage.getType().equals(BroadcastMessage.TIER_UPDATED_SUCCESS) ||
                        broadcastMessage.getType().equals(BroadcastMessage.TIER_DELETED_SUCCESS))) {
                        fetchTier();
                    }

            } catch (JsonProcessingException e) {
                log.error("Broadcast Handler Error", e);
            }
        });

        renderLayout();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    private void configureGrid() {
        tierDtoGrid.setSizeFull();
        tierDtoGrid.removeAllColumns();
        tierDtoGrid.setEmptyStateText(UiUtil.NO_RECORDS);
        tierDtoGrid.addClassName("tier-grid");
        tierDtoGrid.addColumn(TierDto::getName).setHeader("Name");
        tierDtoGrid.addColumn("brandDto.name").setHeader("Brand Name");
        tierDtoGrid.addComponentColumn(this::applyButton).setHeader("Action");
        tierDtoGrid.getColumns().forEach(tierDtoColumn -> tierDtoColumn.setAutoWidth(true));
    }

    private Button applyEditButton(TierDto tierDto) {
        return UiUtil.editButton("Edit Name", event -> editTier(tierDto, FormAction.EDIT));
    }

    private Button applyDeleteButton(TierDto tierDto) {
        return UiUtil.deleteButton(new TierDeleteEventListener(this.ui, tierDto.getId(),
                this.restClientOrganizationService));
    }

    private Component applyButton(TierDto tierDto) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.add(applyEditButton(tierDto));
        layout.add(applyDeleteButton(tierDto));
        return layout;
    }

    private void editTier(TierDto tierDto, FormAction formAction) {
        if (tierDto == null) {
            closeEditor();
        } else {

            tierDto.setBrandId(accessService.getUserDetail().getStoreDto().getChainDto().getBrandId());
            tierForm.changeTierDto(tierDto);
            tierForm.restructureButton(formAction);
            tierForm.setVisible(true);
            addClassName("editing");
        }
    }

    private void configureForm() {
        tierForm = new TierPriceForm(this.restClientOrganizationService, this.asyncRestClientOrganizationService);
        tierForm.setWidth("25em");
    }

    private HorizontalLayout getContent() {
        HorizontalLayout content = new HorizontalLayout(tierDtoGrid, tierForm);
        content.setFlexGrow(2, tierDtoGrid);
        content.setFlexGrow(1, tierForm);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
    }

    /**
     * Builds the toolbar with a lazy name filter and a "New Tier" button.
     *
     * @return the toolbar layout to place above the grid
     */
    public HorizontalLayout getToolbarComponent() {
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setPrefixComponent(VaadinIcon.SEARCH.create());
        filterText.setValueChangeMode(ValueChangeMode.LAZY);

        Button addChainButton = UiUtil.addButton("New Tier", event -> addTier());
        HorizontalLayout toolbar = new HorizontalLayout(filterText, addChainButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private void addTier() {
        tierDtoGrid.asSingleSelect().clear();
        TierDto tierDto = new TierDto();
        tierDto.setType(TierTypeDto.PRICE);
        editTier(tierDto, FormAction.CREATE);
    }

    private void fetchTier() {
        loadingBar.start();
        asyncRestClientOrganizationService.getAllTierByBrandAsync(result -> ui.access(() -> {
            loadingBar.stop();
            tierDtoGrid.setItems(result);
        }),
                accessService.getUserDetail().getStoreDto().getChainDto().getBrandId(), TierTypeDto.PRICE);
    }
}
