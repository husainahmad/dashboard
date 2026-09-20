package com.harmoni.menu.dashboard.layout.organization.brand;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.event.brand.BrandDeleteEventListener;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.LoadingBar;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.Registration;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

/**
 * Vaadin grid view listing all brands. Refreshes on BROADCAST insert/update,
 * exposes edit/delete actions per row, and opens a {@link BrandForm} dialog
 * for add/edit.
 */
@RequiredArgsConstructor
@Slf4j
public class BrandListView extends VerticalLayout {

    Registration broadcasterRegistration;

    Grid<BrandDto> brandDtoGrid = new Grid<>(BrandDto.class);

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;

    UI ui;
    TextField filterText = new TextField();
    LoadingBar loadingBar = new LoadingBar();

    private void renderLayout() {
        setSizeFull();
        setPadding(false);

        configureGrid();

        add(loadingBar, getContent());
        fetchBrands();
    }

    private HorizontalLayout getContent() {
        HorizontalLayout content = new HorizontalLayout(brandDtoGrid);
        content.setFlexGrow(1, brandDtoGrid);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
    }

    private void configureGrid() {
        brandDtoGrid.setSizeFull();
        brandDtoGrid.removeAllColumns();
        brandDtoGrid.setEmptyStateText(UiUtil.NO_RECORDS);
        brandDtoGrid.addColumn(BrandDto::getName).setHeader("Name");

        brandDtoGrid.getColumns().forEach(brandDtoColumn -> brandDtoColumn.setAutoWidth(true));
        brandDtoGrid.addComponentColumn(this::applyButton).setHeader("Action");
    }

    private Component applyButton(BrandDto brandDto) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.add(applyButtonEdit(brandDto));
        layout.add(applyButtonDelete(brandDto));
        return layout;
    }

    private Button applyButtonEdit(BrandDto brandDto) {
        return UiUtil.editButton(event -> editBrand(brandDto, FormAction.EDIT));
    }

    private Button applyButtonDelete(BrandDto brandDto) {
        return UiUtil.deleteButton(
                new BrandDeleteEventListener(brandDto, restClientOrganizationService));
    }

    /**
     * Builds the toolbar with a lazy name filter and a "New Brand" button.
     *
     * @return the toolbar layout to place above the grid
     */
    public HorizontalLayout getToolbarComponent() {
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setPrefixComponent(VaadinIcon.SEARCH.create());
        filterText.setValueChangeMode(ValueChangeMode.LAZY);

        Button addBrandButton = UiUtil.addButton("New Brand", event -> addBrand());
        HorizontalLayout toolbar = new HorizontalLayout(filterText, addBrandButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(message -> {
            try {
                BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
                if (ObjectUtils.isNotEmpty(broadcastMessage) && ObjectUtils.isNotEmpty(broadcastMessage.getType())
                        && (broadcastMessage.getType().equals(BroadcastMessage.BRAND_INSERT_SUCCESS) ||
                            broadcastMessage.getType().equals(BroadcastMessage.BRAND_SUCCESS_UPDATED))) {
                        fetchBrands();
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

    /**
     * Opens a dialog containing a {@link BrandForm} for the given brand.
     *
     * @param brandDto   the brand to edit, or a new empty one to create
     * @param formAction whether the dialog is in create or edit mode
     */
    public void editBrand(BrandDto brandDto, FormAction formAction) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(formAction == FormAction.EDIT ? "Edit Brand" : "Add Brand");
        dialog.setWidth("400px");
        dialog.add(new BrandForm(this.restClientOrganizationService, dialog, formAction, brandDto));
        dialog.open();
    }

    private void addBrand() {
        brandDtoGrid.asSingleSelect().clear();
        editBrand(new BrandDto(), FormAction.CREATE);
    }

    private void fetchBrands() {
        loadingBar.start();
        asyncRestClientOrganizationService.getAllBrandAsync(result -> ui.access(() -> {
            loadingBar.stop();
            brandDtoGrid.setItems(result);
        }));
    }
}
