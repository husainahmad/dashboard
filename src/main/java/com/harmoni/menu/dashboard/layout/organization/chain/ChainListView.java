package com.harmoni.menu.dashboard.layout.organization.chain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.event.chain.ChainDeleteEventListener;
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
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.Registration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Vaadin grid view listing the chains of the current user's brand. Resolves the
 * brand name of each chain for display, refreshes on BROADCAST insert/update,
 * and opens a {@link ChainForm} dialog for add/edit.
 */
@RequiredArgsConstructor
@Slf4j
public class ChainListView extends VerticalLayout  {

    Registration broadcasterRegistration;

    private final Grid<ChainDto> chainDtoGrid = new Grid<>(ChainDto.class);

    private UI ui;

    private final TextField filterText = new TextField();
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final AccessService accessService;
    private final LoadingBar loadingBar = new LoadingBar();

    private void renderLayout() {
        setSizeFull();
        setPadding(false);
        configureGrid();

        add(loadingBar, getContent());
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
        chainDtoGrid.setEmptyStateText(UiUtil.NO_RECORDS);
        chainDtoGrid.setColumns("name");
        chainDtoGrid.addColumn(this::brandName).setHeader("Brand").setSortable(true);
        chainDtoGrid.getColumns().forEach(chainDtoColumn -> chainDtoColumn.setAutoWidth(true));
        chainDtoGrid.addComponentColumn(this::applyButton).setHeader("Action");
    }

    private String brandName(ChainDto chainDto) {
        return chainDto.getBrandDto() == null ? "" : chainDto.getBrandDto().getName();
    }

    private Component applyButton(ChainDto chainDto) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.add(applyButtonEdit(chainDto));
        layout.add(applyButtonDelete(chainDto));
        return layout;
    }

    private Button applyButtonEdit(ChainDto chainDto) {
        return UiUtil.editButton(event -> editChain(chainDto, FormAction.EDIT));
    }

    private Button applyButtonDelete(ChainDto chainDto) {
        return UiUtil.deleteButton(
                new ChainDeleteEventListener(chainDto, restClientOrganizationService));
    }

    private HorizontalLayout getContent() {
        HorizontalLayout content = new HorizontalLayout(chainDtoGrid);
        content.setFlexGrow(1, chainDtoGrid);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
    }

    /**
     * Builds the toolbar with a lazy name filter and a "New Chain" button.
     *
     * @return the toolbar layout to place above the grid
     */
    public HorizontalLayout getToolbarComponent() {
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setPrefixComponent(VaadinIcon.SEARCH.create());
        filterText.setValueChangeMode(ValueChangeMode.LAZY);

        Button addChainButton = UiUtil.addButton("New Chain", event -> addChain());
        HorizontalLayout toolbar = new HorizontalLayout(filterText, addChainButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private void fetchChains() {
        loadingBar.start();
        Integer brandId = accessService.getUserDetail().getStoreDto().getChainDto().getBrandId();
        asyncRestClientOrganizationService.getAllBrandAsync(brands ->
                ui.access(() -> {
                    Map<Integer, String> brandNames = brands.stream()
                            .collect(Collectors.toMap(BrandDto::getId, BrandDto::getName, (a, b) -> a));
                    asyncRestClientOrganizationService.getAllChainByBrandIdAsync(chains ->
                            ui.access(() -> {
                                loadingBar.stop();
                                chains.forEach(chain -> applyBrandName(chain, brandNames));
                                chainDtoGrid.setItems(chains);
                            }), brandId);
                }));
    }

    private void applyBrandName(ChainDto chain, Map<Integer, String> brandNames) {
        String name = brandNames.get(chain.getBrandId());
        if (name != null) {
            BrandDto brandDto = new BrandDto();
            brandDto.setId(chain.getBrandId());
            brandDto.setName(name);
            chain.setBrandDto(brandDto);
        }
    }

    /**
     * Loads the brands for the current user's brand, then opens a dialog
     * containing a {@link ChainForm} for the given chain.
     *
     * @param chainDto   the chain to edit, or a new empty one to create
     * @param formAction whether the dialog is in create or edit mode
     */
    public void editChain(ChainDto chainDto, FormAction formAction) {
        loadingBar.start();
        asyncRestClientOrganizationService.getAllBrandAsync(brands ->
                ui.access(() -> {
                    loadingBar.stop();
                    Dialog dialog = new Dialog();
                    dialog.setHeaderTitle(formAction == FormAction.EDIT ? "Edit Chain" : "Add Chain");
                    dialog.setWidth("400px");
                    ChainForm chainForm = new ChainForm(this.restClientOrganizationService,
                            this.asyncRestClientOrganizationService, dialog, formAction, chainDto, brands);
                    dialog.add(chainForm);
                    dialog.open();
                }));
    }

    private void addChain() {
        chainDtoGrid.asSingleSelect().clear();
        editChain(new ChainDto(), FormAction.CREATE);
    }
}
