package com.harmoni.menu.dashboard.layout.menu.sku;

import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.SkuDto;
import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.component.DialogClosing;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientMenuService;
import com.vaadin.flow.component.AttachEvent;
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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

import java.text.MessageFormat;

@Route(value = "sku", layout = MainLayout.class)
@PageTitle("SKU | POSHarmoni")
public class SkuListView extends VerticalLayout {

    Registration broadcasterRegistration;
    private final Grid<SkuDto> skuDtoGrid = new Grid<>(SkuDto.class);
    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final TextField filterText = new TextField();
    private UI ui;
    public SkuListView(@Autowired AsyncRestClientMenuService asyncRestClientMenuService) {
        this.asyncRestClientMenuService = asyncRestClientMenuService;
        addClassName("list-view");
        setSizeFull();

        configureGrid();
        configureForm();

        add(getToolbar(), getContent());

    }

    private void configureGrid() {
        skuDtoGrid.setSizeFull();
        skuDtoGrid.removeAllColumns();
        skuDtoGrid.addColumn(SkuDto::getName).setHeader("Name");
        skuDtoGrid.addColumn("productDto.name").setHeader("Product");

        skuDtoGrid.getColumns().forEach(skuDtoColumn -> skuDtoColumn.setAutoWidth(true));
        skuDtoGrid.asSingleSelect().addValueChangeListener(valueChangeEvent ->
                editSku(valueChangeEvent.getValue(), FormAction.EDIT));
    }

    private void configureForm() {
//        productForm = new ProductForm(this.asyncRestClientMenuService,
//                this.asyncRestClientOrganizationService,
//                this.restClientMenuService);
//        productForm.setWidth("25em");
    }

    private HorizontalLayout getToolbar() {
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);

        Button addBrandButton = new Button("Add Sku");
        addBrandButton.addClickListener(buttonClickEvent -> {
            addSku();
        });
        HorizontalLayout toolbar = new HorizontalLayout(filterText, addBrandButton);
        toolbar.addClassName("toolbar");
        return toolbar;
    }

    private HorizontalLayout getContent() {
        HorizontalLayout content = new HorizontalLayout(skuDtoGrid);
        content.setFlexGrow(1, skuDtoGrid);
//        content.setFlexGrow(1, productForm);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        ui = attachEvent.getUI();
        broadcasterRegistration = Broadcaster.register(message -> {
//            if (message.equals(BroadcastMessage.PRODUCT_INSERT_SUCCESS)) {
//                fetchProducts();
//            }
            if (message.startsWith(MessageFormat.format("{0}|", String.valueOf(HttpStatus.NO_CONTENT.value())))) {
                showErrorDialog(message);
            }
            if (message.startsWith(MessageFormat.format("{0}|", String.valueOf(HttpStatus.BAD_REQUEST.value())))) {
                showErrorDialog(message);
            }
            if (message.startsWith(MessageFormat.format("{0}|", String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value())))) {
                showErrorDialog(message);
            }
        });
        fetchSkus();
    }

    private void addSku() {
        skuDtoGrid.asSingleSelect().clear();
        editSku(new SkuDto(), FormAction.CREATE);
    }

    public void editSku(SkuDto skuDto, FormAction formAction) {
        if (skuDto == null) {
            closeEditor();
        } else {
//            productForm.setProductDto(productDto);
//            productForm.restructureButton(formAction);
//            productForm.setVisible(true);
            addClassName("editing");
        }
    }

    private void closeEditor() {
//        productForm.setVisible(false);
        removeClassName("editing");
    }

    private void showErrorDialog(String message) {
        DialogClosing dialog = new DialogClosing(message);
        ui.access(()-> {
            add(dialog);
            dialog.open();
        });
    }

    private void fetchSkus() {
        asyncRestClientMenuService.getAllSkuAsync(result -> {
            ui.access(()-> {
                skuDtoGrid.setItems(result);
            });
        });
    }
}
