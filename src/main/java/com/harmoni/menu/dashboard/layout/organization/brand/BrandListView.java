package com.harmoni.menu.dashboard.layout.organization.brand;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.event.brand.BrandDeleteEventListener;
import com.harmoni.menu.dashboard.layout.AbstractListView;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.GridSkeleton;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Set;
import com.harmoni.menu.dashboard.layout.util.Css;

/**
 * Vaadin grid view listing all brands. Refreshes on BROADCAST insert/update,
 * exposes edit/delete actions per row, and opens a {@link BrandForm} tab
 * for add/edit.
 */
@RequiredArgsConstructor
@Slf4j
public class BrandListView extends AbstractListView {

    Grid<BrandDto> brandDtoGrid = new Grid<>(BrandDto.class);

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;

    private final GridSkeleton gridSkeleton = new GridSkeleton(8);

    private void renderLayout() {
        setSizeFull();
        setPadding(false);

        configureGrid();

        add(getContent());
        fetchBrands();
    }

    private HorizontalLayout getContent() {
        return gridSlot(brandDtoGrid, gridSkeleton);
    }

    private void configureGrid() {
        brandDtoGrid.setSizeFull();
        brandDtoGrid.removeAllColumns();
        brandDtoGrid.setEmptyStateText(Messages.get("grid.empty.brands"));
        brandDtoGrid.addColumn(BrandDto::getName).setHeader(Messages.get(Messages.Keys.GRID_HEADER_NAME));

        brandDtoGrid.getColumns().forEach(brandDtoColumn -> brandDtoColumn.setAutoWidth(true));
        brandDtoGrid.addComponentColumn(this::applyButton).setHeader(Messages.get(Messages.Keys.GRID_HEADER_ACTION));
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
        configureSearchFilter();

        Button addBrandButton = UiUtil.addButton(Messages.get(Messages.Keys.ACTION_NEW_BRAND), event -> addBrand());
        HorizontalLayout toolbar = new HorizontalLayout(filterText, addBrandButton);
        registerNewShortcut(this::addBrand);
        toolbar.addClassName(Css.TOOLBAR);
        return toolbar;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        refreshOnBroadcast(Set.of(BroadcastMessage.BRAND_INSERT_SUCCESS,
                BroadcastMessage.BRAND_SUCCESS_UPDATED), this::fetchBrands);
        renderLayout();
    }

    /**
     * Opens a tab containing a {@link BrandForm} for the given brand.
     *
     * @param brandDto   the brand to edit, or a new empty one to create
     * @param formAction whether the tab is in create or edit mode
     */
    public void editBrand(BrandDto brandDto, FormAction formAction) {
        if (!(this.getParent().orElseThrow() instanceof TabSheet tabSheet)) {
            return;
        }
        TabManager tabManager = new TabManager(tabSheet);
        String tabLabel = formAction == FormAction.EDIT && ObjectUtils.isNotEmpty(brandDto.getName())
                ? Messages.get(Messages.Keys.ACTION_EDIT_NAME, brandDto.getName()) : Messages.get(Messages.Keys.ACTION_NEW_BRAND);
        tabManager.addOrSelect(tabLabel, tab ->
                new BrandForm(this.restClientOrganizationService, tabManager, tab, formAction, brandDto));
    }

    /**
     * Opens a tab containing a {@link BrandForm} for creating a new brand.
     */
    private void addBrand() {
        brandDtoGrid.asSingleSelect().clear();
        editBrand(new BrandDto(), FormAction.CREATE);
    }

    /**
     * Fetches all brands from the REST API and populates the grid. Shows a
     * skeleton while loading and an error dialog if the fetch fails.
     */
    private void fetchBrands() {
        gridSkeleton.show();
        asyncRestClientOrganizationService.getAllBrandAsync(result -> UiUtil.safeAccess(ui, () -> {
            gridSkeleton.hide();
            brandDtoGrid.setItems(result);
        }), error -> UiUtil.safeAccess(ui, () -> {
            gridSkeleton.hide();
            UiUtil.errorWithRetry(Messages.get(Messages.Keys.NOTIFICATION_BRAND_LOAD_FAILED), this::fetchBrands);
        }));
    }
}
