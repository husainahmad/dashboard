package com.harmoni.menu.dashboard.layout.organization.chain;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.event.chain.ChainDeleteEventListener;
import com.harmoni.menu.dashboard.layout.AbstractListView;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.util.GridSkeleton;
import com.harmoni.menu.dashboard.layout.util.LoadingBar;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
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

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Vaadin grid view listing the chains of the current user's brand. Resolves the
 * brand name of each chain for display, refreshes on BROADCAST insert/update,
 * and opens a {@link ChainForm} tab for add/edit.
 */
@RequiredArgsConstructor
@Slf4j
public class ChainListView extends AbstractListView {

    private final Grid<ChainDto> chainDtoGrid = new Grid<>(ChainDto.class);

    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final RestClientOrganizationService restClientOrganizationService;
    private final AccessService accessService;
    private final LoadingBar loadingBar = new LoadingBar();
    private final GridSkeleton gridSkeleton = new GridSkeleton(8);

    private void renderLayout() {
        setSizeFull();
        setPadding(false);
        configureGrid();

        add(loadingBar, getContent());
        loadBrands(asyncRestClientOrganizationService, accessService, this::fetchChains);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        refreshOnBroadcast(Set.of(BroadcastMessage.CHAIN_INSERT_SUCCESS,
                BroadcastMessage.CHAIN_SUCCESS_UPDATED), this::fetchChains);

        renderLayout();
    }

    private void configureGrid() {
        chainDtoGrid.setSizeFull();
        chainDtoGrid.setEmptyStateText("No chains yet \u2014 click \u201CNew Chain\u201D to add one.");
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
        return gridSlot(chainDtoGrid, gridSkeleton);
    }

    /**
     * Builds the toolbar with a lazy name filter and a "New Chain" button.
     *
     * @return the toolbar layout to place above the grid
     */
    public HorizontalLayout getToolbarComponent() {
        configureSearchFilter();

        Button addChainButton = UiUtil.addButton("New Chain", event -> addChain());
        configureBrandFilter(this::fetchChains);
        HorizontalLayout toolbar = new HorizontalLayout(brandFilter, filterText, addChainButton);
        registerNewShortcut(this::addChain);
        toolbar.addClassName("toolbar");
        toolbar.setAlignItems(FlexComponent.Alignment.BASELINE);
        return toolbar;
    }

    private void fetchChains() {
        gridSkeleton.show();
        Integer brandId = selectedBrandId(accessService);
        asyncRestClientOrganizationService.getAllBrandAsync(brands ->
                UiUtil.safeAccess(ui, () -> {
                    Map<Integer, String> brandNames = brands.stream()
                            .collect(Collectors.toMap(BrandDto::getId, BrandDto::getName, (a, b) -> a));
                    asyncRestClientOrganizationService.getAllChainByBrandIdAsync(chains ->
                            UiUtil.safeAccess(ui, () -> {
                                gridSkeleton.hide();
                                chains.forEach(chain -> applyBrandName(chain, brandNames));
                                chainDtoGrid.setItems(chains);
                            }), error -> UiUtil.safeAccess(ui, () -> {
                                gridSkeleton.hide();
                                UiUtil.errorWithRetry("Couldn't load chains", this::fetchChains);
                            }), brandId);
                }), error -> UiUtil.safeAccess(ui, () -> {
                    gridSkeleton.hide();
                    UiUtil.errorWithRetry("Couldn't load chains", this::fetchChains);
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
     * Loads the brands for the current user's brand, then opens a tab
     * containing a {@link ChainForm} for the given chain.
     *
     * @param chainDto   the chain to edit, or a new empty one to create
     * @param formAction whether the tab is in create or edit mode
     */
    public void editChain(ChainDto chainDto, FormAction formAction) {
        loadingBar.start();
        asyncRestClientOrganizationService.getAllBrandAsync(brands ->
                UiUtil.safeAccess(ui, () -> {
                    loadingBar.stop();
                    if (!(this.getParent().orElseThrow() instanceof TabSheet tabSheet)) {
                        return;
                    }
                    TabManager tabManager = new TabManager(tabSheet);
                    String tabLabel = formAction == FormAction.EDIT && ObjectUtils.isNotEmpty(chainDto.getName())
                            ? "Edit ".concat(chainDto.getName()) : "New Chain";
                    tabManager.addOrSelect(tabLabel, tab -> new ChainForm(this.restClientOrganizationService,
                            this.asyncRestClientOrganizationService, tabManager, tab, formAction, chainDto, brands));
                }));
    }

    private void addChain() {
        chainDtoGrid.asSingleSelect().clear();
        editChain(new ChainDto(), FormAction.CREATE);
    }
}
