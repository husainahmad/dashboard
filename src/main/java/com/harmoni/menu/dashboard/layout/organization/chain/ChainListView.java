package com.harmoni.menu.dashboard.layout.organization.chain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.ChainDto;
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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

@RequiredArgsConstructor
@Route(value = "chain", layout = MainLayout.class)
@PageTitle(MainLayout.TITLE)
@Slf4j
public class ChainListView extends VerticalLayout  {

    Registration broadcasterRegistration;

    private final Grid<ChainDto> chainDtoGrid = new Grid<>(ChainDto.class);
    private ChainForm chainForm;

    private UI ui;

    private final TextField filterText = new TextField();
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;
    private static final int BRAND_ID = 1;

    private void renderLayout() {
        addClassName("list-view");
        setSizeFull();
        configureGrid();
        configureForm();

        add(getToolbar(), getContent());
        closeEditor();
        fetchChains();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(message -> {

            try {
                BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
                if (ObjectUtils.isNotEmpty(broadcastMessage) && ObjectUtils.isNotEmpty(broadcastMessage.getType())
                        && (broadcastMessage.getType().equals(BroadcastMessage.CHAIN_INSERT_SUCCESS) ||
                    broadcastMessage.getType().equals(BroadcastMessage.CHAIN_SUCCESS_UPDATED))) {
                        fetchChains();
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
        chainDtoGrid.setSizeFull();
        chainDtoGrid.setColumns("name");
        chainDtoGrid.getColumns().forEach(chainDtoColumn -> chainDtoColumn.setAutoWidth(true));
        chainDtoGrid.asSingleSelect().addValueChangeListener(valueChangeEvent ->
                editChain(valueChangeEvent.getValue(), FormAction.EDIT));
    }

    private void configureForm() {
        chainForm = new ChainForm(this.restClientOrganizationService, this.asyncRestClientOrganizationService);
        chainForm.setWidth("25em");
    }

    private HorizontalLayout getContent() {
        HorizontalLayout content = new HorizontalLayout(chainDtoGrid, chainForm);
        content.setFlexGrow(2, chainDtoGrid);
        content.setFlexGrow(1, chainForm);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
    }

    private HorizontalLayout getToolbar() {
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);

        Button addChainButton = new Button("Add Chain");
        addChainButton.addClickListener(_ -> addChain());
        HorizontalLayout toolbar = new HorizontalLayout(filterText, addChainButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private void fetchChains() {
        asyncRestClientOrganizationService.getAllChainByBrandIdAsync(result ->
                ui.access(()-> chainDtoGrid.setItems(result)), BRAND_ID);
    }

    public void editChain(ChainDto chainDto, FormAction formAction) {
        if (chainDto == null) {
            closeEditor();
        } else {
            chainForm.setChainDto(chainDto);
            chainForm.restructureButton(formAction);
            chainForm.setVisible(true);
            addClassName("editing");
        }
    }

    private void closeEditor() {
        chainForm.setVisible(false);
        removeClassName("editing");
    }

    private void addChain() {
        chainDtoGrid.asSingleSelect().clear();
        editChain(new ChainDto(), FormAction.CREATE);
    }
}
