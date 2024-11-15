package com.harmoni.menu.dashboard.layout.organization.brand;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.component.DialogClosing;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
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
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.beans.factory.annotation.Autowired;

@Route(value = "brand", layout = MainLayout.class)
@PageTitle("Brand | POSHarmoni")
@Slf4j
public class BrandListView extends VerticalLayout {

    Registration broadcasterRegistration;

    private final Grid<BrandDto> brandDtoGrid = new Grid<>(BrandDto.class);
    private BrandForm brandForm;

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;

    private final RestClientOrganizationService restClientOrganizationService;
    private UI ui;
    private final TextField filterText = new TextField();

    public BrandListView(@Autowired AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                         @Autowired RestClientOrganizationService restClientOrganizationService) {
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
        this.restClientOrganizationService = restClientOrganizationService;

        addClassName("list-view");
        setSizeFull();

        configureGrid();
        configureForm();

        add(getToolbar(), getContent());
        closeEditor();

        fetchBrands();
    }

    private HorizontalLayout getContent() {
        HorizontalLayout content = new HorizontalLayout(brandDtoGrid, brandForm);
        content.setFlexGrow(2, brandDtoGrid);
        content.setFlexGrow(1, brandForm);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
    }

    private void configureGrid() {
        brandDtoGrid.setSizeFull();
        brandDtoGrid.removeAllColumns();
        brandDtoGrid.addColumn(BrandDto::getName).setHeader("Name");

        brandDtoGrid.getColumns().forEach(brandDtoColumn -> brandDtoColumn.setAutoWidth(true));
        brandDtoGrid.asSingleSelect().addValueChangeListener(valueChangeEvent ->
                editBrand(valueChangeEvent.getValue(), FormAction.EDIT));
    }

    private HorizontalLayout getToolbar() {
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);

        Button addBrandButton = new Button("Add Brand");
        addBrandButton.addClickListener(buttonClickEvent -> addBrand());
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
                if (ObjectUtils.isNotEmpty(broadcastMessage) && ObjectUtils.isNotEmpty(broadcastMessage.getType())) {
                    if (broadcastMessage.getType().equals(BroadcastMessage.BRAND_INSERT_SUCCESS) ||
                            broadcastMessage.getType().equals(BroadcastMessage.BRAND_SUCCESS_UPDATED)) {
                        fetchBrands();
                    } else {
                        UiUtil.showErrorDialog(ui, this, message);
                    }
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

    private void showErrorDialog(String message) {
        DialogClosing dialog = new DialogClosing(message);
        ui.access(()-> {
            add(dialog);
            dialog.open();
        });
    }

    private void configureForm() {
        brandForm = new BrandForm(this.restClientOrganizationService);
        brandForm.setWidth("25em");
    }

    public void editBrand(BrandDto brandDto, FormAction formAction) {
        if (brandDto == null) {
            closeEditor();
        } else {
            brandForm.setBrandDto(brandDto);
            brandForm.restructureButton(formAction);
            brandForm.setVisible(true);
            addClassName("editing");
        }
    }

    private void closeEditor() {
        brandForm.setVisible(false);
        removeClassName("editing");
    }

    private void addBrand() {
        brandDtoGrid.asSingleSelect().clear();
        editBrand(new BrandDto(), FormAction.CREATE);
    }

    private void fetchBrands() {
        asyncRestClientOrganizationService.getAllBrandAsync(result -> ui.access(()-> brandDtoGrid.setItems(result)));
    }
}
