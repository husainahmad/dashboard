package com.harmoni.menu.dashboard.layout.menu.category;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.rest.data.RestClientMenuService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.*;
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

@Route(value = "category", layout = MainLayout.class)
@PageTitle("Category | POSHarmoni")
@Slf4j
public class CategoryListView extends VerticalLayout {

    Registration broadcasterRegistration;
    private final Grid<CategoryDto> categoryDtoGrid = new Grid<>(CategoryDto.class);
    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientMenuService restClientMenuService;

    private final TextField filterText = new TextField();
    private CategoryForm categoryForm;

    private UI ui;

    public CategoryListView(@Autowired AsyncRestClientMenuService asyncRestClientMenuService,
                            @Autowired AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                            @Autowired RestClientMenuService restClientMenuService) {

        this.asyncRestClientMenuService = asyncRestClientMenuService;
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
        this.restClientMenuService = restClientMenuService;

        addClassName("list-view");
        setSizeFull();

        configureGrid();
        configureForm();

        add(getToolbar(), getContent());
        closeEditor();

    }

    private void configureGrid() {
        categoryDtoGrid.setSizeFull();
        categoryDtoGrid.removeAllColumns();
        categoryDtoGrid.addColumn(CategoryDto::getName).setHeader("Name");
        categoryDtoGrid.addColumn("brandDto.name").setHeader("Brand Name");

        categoryDtoGrid.getColumns().forEach(categoryDtoColumn -> categoryDtoColumn.setAutoWidth(true));
        categoryDtoGrid.asSingleSelect().addValueChangeListener(valueChangeEvent ->
                editCategory(valueChangeEvent.getValue(), FormAction.EDIT));
    }

    private void configureForm() {
        categoryForm = new CategoryForm(
                this.asyncRestClientOrganizationService,
                this.restClientMenuService);
        categoryForm.setWidth("25em");
    }

    private HorizontalLayout getContent() {
        HorizontalLayout content = new HorizontalLayout(categoryDtoGrid, categoryForm);
        content.setFlexGrow(2, categoryDtoGrid);
        content.setFlexGrow(1, categoryForm);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
    }

    private HorizontalLayout getToolbar() {
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setValueChangeMode(ValueChangeMode.LAZY);

        Button addBrandButton = new Button("Add Category");
        addBrandButton.addClickListener((ComponentEventListener<ClickEvent<Button>>) buttonClickEvent -> CategoryListView.this.addCategory());
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
                    if (broadcastMessage.getType().equals(BroadcastMessage.CATEGORY_INSERT_SUCCESS) ||
                    broadcastMessage.getType().equals(BroadcastMessage.CATEGORY_UPDATED_SUCCESS)) {
                        fetchCategories();
                    } else {
                        UiUtil.showErrorDialog(ui, this, message);
                    }
                }
            } catch (JsonProcessingException e) {
                log.error("Broadcast Handler Error", e);
            }
        });
        fetchCategories();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    private void addCategory() {
        categoryDtoGrid.asSingleSelect().clear();
        editCategory(new CategoryDto(), FormAction.CREATE);
    }

    public void editCategory(CategoryDto categoryDto, FormAction formAction) {
        if (categoryDto == null) {
            closeEditor();
        } else {
            categoryForm.setCategoryDto(categoryDto);
            categoryForm.restructureButton(formAction);
            categoryForm.setVisible(true);
            addClassName("editing");
        }
    }

    private void closeEditor() {
        categoryForm.setVisible(false);
        removeClassName("editing");
    }

    private void fetchCategories() {
        asyncRestClientMenuService.getAllCategoryAsync(result -> ui.access(()->
                categoryDtoGrid.setItems(result)), 1);
    }
}
