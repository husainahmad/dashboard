package com.harmoni.menu.dashboard.layout.menu.category;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.event.category.CategoryDeleteEventListener;
import com.harmoni.menu.dashboard.layout.AbstractListView;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.GridSkeleton;
import com.harmoni.menu.dashboard.layout.util.LoadingBar;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientMenuService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.tabs.TabSheet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Set;

/**
 * The "All Categories" grid view inside {@link CategoryTabs}.
 *
 * <p>
 * Renders the category list in a {@link Grid} with a name filter and per-row
 * edit and delete actions; deletion is delegated to
 * {@link CategoryDeleteEventListener} and editing opens a {@link CategoryForm}
 * in a new tab. The list refreshes itself whenever a category-insert or
 * category-updated broadcast is received via {@link Broadcaster}.
 * </p>
 */
@RequiredArgsConstructor
@Slf4j
public class CategoryListView extends AbstractListView {

    private final Grid<CategoryDto> categoryDtoGrid = new Grid<>(CategoryDto.class);
    private final AsyncRestClientMenuService asyncRestClientMenuService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientMenuService restClientMenuService;
    private final AccessService accessService;

    LoadingBar loadingBar = new LoadingBar();
    private final GridSkeleton gridSkeleton = new GridSkeleton(8);

    private void renderLayout() {
        setSizeFull();
        setPadding(false);

        configureGrid();

        add(loadingBar, getContent());
    }

    private void configureGrid() {
        categoryDtoGrid.setSizeFull();
        categoryDtoGrid.removeAllColumns();
        categoryDtoGrid.setEmptyStateText("No categories yet \u2014 click \u201CNew Category\u201D to add one.");
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
        return gridSlot(categoryDtoGrid, gridSkeleton);
    }

    /**
     * Builds the list toolbar containing the name filter and the "New Category" button.
     *
     * @return the toolbar row for this list view
     */
    public HorizontalLayout getToolbarComponent() {
        configureSearchFilter();

        Button addBrandButton = UiUtil.addButton("New Category",
                (ComponentEventListener<ClickEvent<Button>>) event -> CategoryListView.this.addCategory());
        configureBrandFilter(this::fetchCategories);
        HorizontalLayout toolbar = new HorizontalLayout(brandFilter, filterText, addBrandButton);
        toolbar.addClassName("toolbar");
        toolbar.setAlignItems(FlexComponent.Alignment.BASELINE);
        registerNewShortcut(this::addCategory);
        return toolbar;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        refreshOnBroadcast(Set.of(BroadcastMessage.CATEGORY_INSERT_SUCCESS,
                BroadcastMessage.CATEGORY_UPDATED_SUCCESS), this::fetchCategories);
        renderLayout();
        loadBrands(asyncRestClientOrganizationService, accessService, this::fetchCategories);
    }

    /**
     * Opens the {@link CategoryForm} for a new category after loading the
     */
    private void addCategory() {
        categoryDtoGrid.asSingleSelect().clear();
        editCategory(new CategoryDto(), FormAction.CREATE);
    }

    /**
     * Opens the {@link CategoryForm} in a new tab for the given category — create
     * or edit depending on {@code formAction} — after loading the available brands.
     *
     * @param categoryDto the category to bind, or an empty DTO for a new one
     * @param formAction  whether the form should create or update
     */
    public void editCategory(CategoryDto categoryDto, FormAction formAction) {
        loadingBar.start();
        asyncRestClientOrganizationService.getAllBrandAsync(brands ->
                UiUtil.safeAccess(ui, () -> {
                    loadingBar.stop();
                    if (!(this.getParent().orElseThrow() instanceof TabSheet tabSheet)) {
                        return;
                    }
                    TabManager tabManager = new TabManager(tabSheet);
                    String tabLabel = formAction == FormAction.EDIT && ObjectUtils.isNotEmpty(categoryDto.getName())
                            ? "Edit ".concat(categoryDto.getName()) : "New Category";
                    tabManager.addOrSelect(tabLabel, tab -> new CategoryForm(this.asyncRestClientOrganizationService,
                            this.restClientMenuService, tabManager, tab, formAction, categoryDto, brands));
                }));
    }

    /**
     * Fetches the list of categories for the selected brand and updates the grid.
     */
    private void fetchCategories() {
        gridSkeleton.show();
        asyncRestClientMenuService.getAllCategoryAsync(result -> UiUtil.safeAccess(ui, () -> {
            gridSkeleton.hide();
            categoryDtoGrid.setItems(result);
        }), error -> UiUtil.safeAccess(ui, () -> {
            gridSkeleton.hide();
            UiUtil.errorWithRetry("Couldn't load categories", this::fetchCategories);
        }), selectedBrandId(accessService));
    }
}
