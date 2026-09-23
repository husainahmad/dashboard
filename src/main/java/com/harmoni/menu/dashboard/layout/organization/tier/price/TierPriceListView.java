package com.harmoni.menu.dashboard.layout.organization.tier.price;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.dto.TierTypeDto;
import com.harmoni.menu.dashboard.event.tier.TierDeleteEventListener;
import com.harmoni.menu.dashboard.layout.AbstractListView;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.GridSkeleton;
import com.harmoni.menu.dashboard.layout.util.LoadingBar;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Set;
import com.harmoni.menu.dashboard.layout.util.Css;

/**
 * Vaadin grid view listing the price tiers (type {@code PRICE}) of the current
 * user's brand. Refreshes on TIER BROADCAST insert/update/delete and opens a
 * {@link TierPriceForm} tab for add/edit.
 */
@RequiredArgsConstructor
@Slf4j
public class TierPriceListView extends AbstractListView {

    private final Grid<TierDto> tierDtoGrid = new Grid<>(TierDto.class);
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final AccessService accessService;

    private final LoadingBar loadingBar = new LoadingBar();
    private final GridSkeleton gridSkeleton = new GridSkeleton(8);

    private void renderLayout() {
        setSizeFull();
        setPadding(false);
        configureGrid();

        add(loadingBar, gridSlot(tierDtoGrid, gridSkeleton));

        loadBrands(asyncRestClientOrganizationService, accessService, this::fetchTier);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        refreshOnBroadcast(Set.of(BroadcastMessage.TIER_INSERT_SUCCESS,
                BroadcastMessage.TIER_UPDATED_SUCCESS,
                BroadcastMessage.TIER_DELETED_SUCCESS), this::fetchTier);

        renderLayout();
    }

    private void configureGrid() {
        tierDtoGrid.setSizeFull();
        tierDtoGrid.removeAllColumns();
        tierDtoGrid.setEmptyStateText(Messages.get("grid.empty.tierPrices"));
        tierDtoGrid.addClassName("tier-grid");
        tierDtoGrid.addColumn(TierDto::getName).setHeader(Messages.get(Messages.Keys.GRID_HEADER_NAME));
        tierDtoGrid.addColumn("brandDto.name").setHeader(Messages.get(Messages.Keys.GRID_HEADER_BRAND_NAME));
        tierDtoGrid.addComponentColumn(this::applyButton).setHeader(Messages.get(Messages.Keys.GRID_HEADER_ACTION));
        tierDtoGrid.getColumns().forEach(tierDtoColumn -> tierDtoColumn.setAutoWidth(true));
    }

    private Button applyEditButton(TierDto tierDto) {
        return UiUtil.editButton(Messages.get(Messages.Keys.ACTION_EDIT_NAME_FLAT), event -> editTier(tierDto, FormAction.EDIT));
    }

    private Button applyDeleteButton(TierDto tierDto) {
        return UiUtil.deleteButton(new TierDeleteEventListener(this.ui, tierDto.getId(),
                this.restClientOrganizationService));
    }

    private Component applyButton(TierDto tierDto) {
        HorizontalLayout layout = new HorizontalLayout();
        layout.add(applyEditButton(tierDto));
        layout.add(applyDeleteButton(tierDto));
        return layout;
    }

    /**
     * Loads the brands for the current user, then opens a tab containing a
     * {@link TierPriceForm} for the given tier. Brands are resolved before the
     * tab opens so the brand combo box is populated synchronously on attach and
     * the tier's brand can be pre-selected.
     *
     * @param tierDto    the tier to edit, or a new empty one to create
     * @param formAction whether the tab is in create or edit mode
     */
    private void editTier(TierDto tierDto, FormAction formAction) {
        if (tierDto.getBrandId() == null) {
            tierDto.setBrandId(selectedBrandId(accessService));
        }
        loadingBar.start();
        asyncRestClientOrganizationService.getAllBrandAsync(brands ->
                UiUtil.safeAccess(ui, () -> {
                    loadingBar.stop();
                    if (!(this.getParent().orElseThrow() instanceof TabSheet tabSheet)) {
                        return;
                    }
                    TabManager tabManager = new TabManager(tabSheet);
                    String tabLabel = formAction == FormAction.EDIT && ObjectUtils.isNotEmpty(tierDto.getName())
                            ? Messages.get(Messages.Keys.ACTION_EDIT_NAME, tierDto.getName()) : Messages.get("action.newTierPrice");
                    tabManager.addOrSelect(tabLabel, tab -> new TierPriceForm(restClientOrganizationService,
                            asyncRestClientOrganizationService, tabManager, tab, formAction, tierDto, brands));
                }));
    }

    /**
     * Builds the toolbar with a lazy name filter and a "New Tier" button.
     *
     * @return the toolbar layout to place above the grid
     */
    public HorizontalLayout getToolbarComponent() {
        configureSearchFilter();

        Button addChainButton = UiUtil.addButton(Messages.get("action.newTier"), event -> addTier());
        configureBrandFilter(this::fetchTier);
        HorizontalLayout toolbar = new HorizontalLayout(brandFilter, filterText, addChainButton);
        registerNewShortcut(this::addTier);
        toolbar.addClassName(Css.TOOLBAR);
        toolbar.setAlignItems(FlexComponent.Alignment.BASELINE);
        return toolbar;
    }

    private void addTier() {
        tierDtoGrid.asSingleSelect().clear();
        TierDto tierDto = new TierDto();
        tierDto.setType(TierTypeDto.PRICE);
        editTier(tierDto, FormAction.CREATE);
    }

    private void fetchTier() {
        gridSkeleton.show();
        asyncRestClientOrganizationService.getAllTierByBrandAsync(result -> UiUtil.safeAccess(ui, () -> {
            gridSkeleton.hide();
            tierDtoGrid.setItems(result);
        }), error -> UiUtil.safeAccess(ui, () -> {
            gridSkeleton.hide();
            UiUtil.errorWithRetry(Messages.get("notification.tierPrice.loadFailed"), this::fetchTier);
        }),
                selectedBrandId(accessService), TierTypeDto.PRICE);
    }
}
