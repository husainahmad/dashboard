package com.harmoni.menu.dashboard.layout.menu.category;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.event.category.CategoryDeleteEventListener;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.LoadingBar;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.*;
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

@RequiredArgsConstructor
@Slf4j
public class CategoryListView extends VerticalLayout {

    Registration broadcasterRegistration;
    private final Grid<CategoryDto> categoryDtoGrid = new Grid<>(CategoryDto.class);
    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientMenuService restClientMenuService;
    private final AccessService accessService;

    TextField filterText = new TextField();
    LoadingBar loadingBar = new LoadingBar();

    UI ui;

    private void renderLayout() {
        setSizeFull();
        setPadding(false);

        configureGrid();

        add(loadingBar, getContent());
    }

    private void configureGrid() {
        categoryDtoGrid.setSizeFull();
        categoryDtoGrid.removeAllColumns();
        categoryDtoGrid.setEmptyStateText(UiUtil.NO_RECORDS);
        categoryDtoGrid.addColumn(CategoryDto::getName).setHeader("Name");
        categoryDtoGrid.addColumn("brandDto.name").setHeader("Brand Name");

        categoryDtoGrid.getColumns().forEach(categoryDtoColumn -> categoryDtoColumn.setAutoWidth(true));
        categoryDtoGrid.addComponentColumn(this::applyButton).setHeader("Action");
    }

    private Component applyButton(CategoryDto categoryDto) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.add(applyButtonEdit(categoryDto));
        layout.add(applyButtonDelete(categoryDto));
        return layout;
    }

    private Button applyButtonEdit(CategoryDto categoryDto) {
        return UiUtil.editButton(event -> editCategory(categoryDto, FormAction.EDIT));
    }

    private Button applyButtonDelete(CategoryDto categoryDto) {
        return UiUtil.deleteButton(
                new CategoryDeleteEventListener(categoryDto, restClientMenuService));
    }

    private HorizontalLayout getContent() {
        HorizontalLayout content = new HorizontalLayout(categoryDtoGrid);
        content.setFlexGrow(1, categoryDtoGrid);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
    }

    public HorizontalLayout getToolbarComponent() {
        filterText.setPlaceholder("Filter by name...");
        filterText.setClearButtonVisible(true);
        filterText.setPrefixComponent(VaadinIcon.SEARCH.create());
        filterText.setValueChangeMode(ValueChangeMode.LAZY);

        Button addBrandButton = UiUtil.addButton("New Category",
                (ComponentEventListener<ClickEvent<Button>>) event -> CategoryListView.this.addCategory());
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
                        && (broadcastMessage.getType().equals(BroadcastMessage.CATEGORY_INSERT_SUCCESS) ||
                    broadcastMessage.getType().equals(BroadcastMessage.CATEGORY_UPDATED_SUCCESS))) {
                        fetchCategories();
                    }

            } catch (JsonProcessingException e) {
                log.error("Broadcast Handler Error", e);
            }
        });
        renderLayout();
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
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(formAction == FormAction.EDIT ? "Edit Category" : "Add Category");
        dialog.setWidth("420px");
        dialog.add(new CategoryForm(this.asyncRestClientOrganizationService,
                this.restClientMenuService, dialog, formAction, categoryDto));
        dialog.open();
    }

    private void fetchCategories() {
        loadingBar.start();
        asyncRestClientMenuService.getAllCategoryAsync(result -> ui.access(() -> {
            loadingBar.stop();
            categoryDtoGrid.setItems(result);
        }), accessService.getUserDetail().getStoreDto().getChainDto().getBrandId());
    }
}
