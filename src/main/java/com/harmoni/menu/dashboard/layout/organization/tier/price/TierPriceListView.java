package com.harmoni.menu.dashboard.layout.organization.tier.price;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.dto.TierTypeDto;
import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.Registration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "tier-price", layout = MainLayout.class)
@PageTitle("Tier | POSHarmoni")
@Slf4j
public class TierPriceListView extends VerticalLayout {

    Registration broadcasterRegistration;
    private final Grid<TierDto> tierDtoGrid = new Grid<>(TierDto.class);
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final TextField filterText = new TextField();
    private UI ui;
    private final RestClientOrganizationService restClientOrganizationService;
    private TierPriceForm tierForm;

    public TierPriceListView(@Autowired AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                             @Autowired RestClientOrganizationService restClientOrganizationService) {
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
        this.restClientOrganizationService = restClientOrganizationService;
        addClassName("list-view");
        setSizeFull();
        configureGrid();
        configureForm();

        add(getToolbar(), getContent());
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
                            broadcastMessage.getType().equals(BroadcastMessage.TIER_UPDATED_SUCCESS))) {
                        fetchTier();
                    }

            } catch (JsonProcessingException e) {
                log.error("Broadcast Handler Error", e);
            }
        });
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    private void configureGrid() {
        tierDtoGrid.setSizeFull();
        tierDtoGrid.removeAllColumns();
        tierDtoGrid.addColumn(TierDto::getName).setHeader("Name");
        tierDtoGrid.addColumn("brandDto.name").setHeader("Brand Name");

        tierDtoGrid.getColumns().forEach(tierDtoColumn -> tierDtoColumn.setAutoWidth(true));
        tierDtoGrid.asSingleSelect().addValueChangeListener(valueChangeEvent ->
                editTier(valueChangeEvent.getValue(), FormAction.EDIT));
    }

    private void editTier(TierDto tierDto, FormAction formAction) {
        if (tierDto == null) {
            closeEditor();
        } else {
            tierForm.setChangeTierDto(tierDto);
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

    private HorizontalLayout getToolbar() {
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);

        Button addChainButton = new Button("Add Tier");
        addChainButton.addClickListener(e -> addTier());
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
        asyncRestClientOrganizationService.getAllTierByBrandAsync(result -> ui.access(()->
                tierDtoGrid.setItems(result)), 1, TierTypeDto.PRICE);
    }
}
