package com.harmoni.menu.dashboard.layout.organization.tier;

import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.layout.AbstractListView;
import com.harmoni.menu.dashboard.layout.component.TabManager;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.layout.organization.tier.service.TreeLevel;
import com.harmoni.menu.dashboard.layout.util.GridSkeleton;
import com.harmoni.menu.dashboard.layout.util.LoadingBar;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientBase.AsyncRestCallback;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.HasValue;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.treegrid.TreeGrid;
import com.vaadin.flow.data.provider.hierarchy.TreeData;
import org.apache.commons.lang3.ObjectUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.harmoni.menu.dashboard.layout.util.Css;

/**
 * Shared scaffold for the service-tier and menu-tier tree-grid views: grid
 * columns, the checkbox cell with its save/rollback flow, the edit/delete
 * actions and the add/edit tab opening. Item-specific behaviour (loading,
 * expansion tracking, listeners and tree building) is delegated to subclasses.
 *
 * @param <T> the tree node type rendered by the grid
 */
public abstract class AbstractTierTreeListView<T extends TierTreeItem> extends AbstractListView {

    protected final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    protected final AccessService accessService;
    protected final RestClientOrganizationService restClientOrganizationService;

    protected final LoadingBar loadingBar = new LoadingBar();
    protected final GridSkeleton gridSkeleton = new GridSkeleton(8);
    protected Button[] buttonEdits;
    protected Button[] buttonDeletes;
    protected final Map<String, Checkbox> checkBoxes = new HashMap<>();
    protected final Map<Integer, Date> lastSavedByTier = new HashMap<>();
    protected transient BrandDto brandDto = new BrandDto();

    protected AbstractTierTreeListView(AsyncRestClientOrganizationService asyncRestClientOrganizationService,
                                       AccessService accessService,
                                       RestClientOrganizationService restClientOrganizationService) {
        this.asyncRestClientOrganizationService = asyncRestClientOrganizationService;
        this.accessService = accessService;
        this.restClientOrganizationService = restClientOrganizationService;
    }

    protected abstract TreeGrid<T> treeGrid();

    protected abstract String emptyStateText();

    protected abstract String newTierLabel();

    protected abstract String loadingErrorMessage();

    /** Attaches the grid's expand/collapse listeners tracking expanded nodes. */
    protected abstract void wireExpansionTracking();

    /** Re-opens the previously expanded rows after a reload. */
    protected abstract void restoreExpansion();

    /** Resolves the tier root owning the given (checkbox) node. */
    protected abstract T tierRoot(T item);

    /** Builds the tier the given node belongs to. */
    protected abstract TierDto tierDtoOf(T item);

    /** Persists the checkbox toggle via the domain listener, rolling back on failure. */
    protected abstract void saveCheckbox(T item, T rootItem, Runnable rollback);

    /** Builds the row delete button. */
    protected abstract Button deleteButtonFor(T item);

    /** Fires the domain-specific tier fetch; the success channel feeds the caller. */
    protected abstract void fetchTierData(AsyncRestCallback<Throwable> errorHandler);

    /** Opens the domain-specific form tab. */
    protected abstract Component openForm(RestClientOrganizationService sync,
                                          AsyncRestClientOrganizationService async, TabManager tabManager,
                                          Tab currentTab, FormAction formAction, TierDto tierDto,
                                          List<BrandDto> brands);

    /**
     * Renders the grid and loads the tiers for the current user's brand. Adds
     * the loading bar and skeleton to the layout.
     */
    protected void renderLayout() {
        setSizeFull();
        setPadding(false);
        brandDto.setId(sessionBrandId(accessService));
        configureGrid();
        add(loadingBar, gridSlot(treeGrid(), gridSkeleton));
    }

    /**
     * Configures the grid columns and headers, sets the empty state text and
     * attaches the expand/collapse listeners tracking expanded nodes.
     */
    protected void configureGrid() {
        TreeGrid<T> grid = treeGrid();
        grid.setSizeFull();
        grid.removeAllColumns();
        grid.setEmptyStateText(emptyStateText());

        grid.addHierarchyColumn(TierTreeItem::getName).setHeader(Messages.get("grid.header.tierName"));
        grid.addComponentColumn(this::applyCheckbox).setHeader(Messages.get(Messages.Keys.GRID_HEADER_ACTIVE));
        grid.addComponentColumn(this::applyButton).setHeader(Messages.get(Messages.Keys.GRID_HEADER_ACTION));
        grid.getColumns().forEach(column -> column.setAutoWidth(true));
        wireExpansionTracking();
    }

    /**
     * Loads the brands for the current user, then opens a tab containing the
     * form for the given tier. Brands are resolved before the tab opens so the
     * brand combo box is populated synchronously on attach and the tier's brand
     * can be pre-selected.
     *
     * @param tierDto the tier to edit, or a new empty one to create
     * @param action  whether the tab is in create or edit mode
     */
    protected void editTier(TierDto tierDto, FormAction action) {
        if (tierDto.getBrandId() == null) {
            tierDto.setBrandId(brandDto.getId());
        }
        loadingBar.start();
        asyncRestClientOrganizationService.getAllBrandAsync(brands ->
                UiUtil.safeAccess(ui, () -> {
                    loadingBar.stop();
                    if (!(this.getParent().orElseThrow() instanceof TabSheet tabSheet)) {
                        return;
                    }
                    TabManager tabManager = new TabManager(tabSheet);
                    String tabLabel = action == FormAction.EDIT && ObjectUtils.isNotEmpty(tierDto.getName())
                            ? Messages.get(Messages.Keys.ACTION_EDIT_NAME, tierDto.getName()) : newTierLabel();
                    tabManager.addOrSelect(tabLabel, tab -> openForm(restClientOrganizationService,
                            asyncRestClientOrganizationService, tabManager, tab, action, tierDto, brands));
                }));
    }

    /**
     * Reloads the tiers behind the skeleton, retrying through the standard
     * error toast when the fetch fails.
     */
    protected void fetchTier() {
        gridSkeleton.show();
        fetchTierData(error -> UiUtil.safeAccess(ui, () -> {
            gridSkeleton.hide();
            UiUtil.errorWithRetry(loadingErrorMessage(), this::fetchTier);
        }));
    }

    /**
     * Creates a checkbox for the given tier item, unless it is a non-leaf node
     * (root or parent). The checkbox is bound to the item's active state and
     * triggers a save operation on change, rolling back on failure.
     *
     * @param item the tier item for which to create the checkbox
     * @return a component containing the checkbox and its status span, or null
     *         for non-leaf nodes
     */
    protected Component applyCheckbox(T item) {
        if (isNonLeafNode(item)) {
            return null;
        }

        Checkbox checkbox = new Checkbox(item.isActive());
        VerticalLayout cell = new VerticalLayout();
        cell.setPadding(false);
        cell.setSpacing(false);
        Span status = new Span();
        applySavedStatus(item, status);
        final boolean[] rollingBack = {false};
        checkbox.addValueChangeListener(event -> onCheckboxChanged(item, checkbox, status, rollingBack, event));
        checkBoxes.put(item.getId(), checkbox);
        cell.add(checkbox, status);
        return cell;
    }

    /**
     * Determines whether the given item is a non-leaf node (root or parent) in
     * the tree. Non-leaf nodes do not have checkboxes.
     *
     * @param item the tier item to check
     * @return true if the item is a root or parent node, false otherwise
     */
    private boolean isNonLeafNode(T item) {
        TreeLevel level = item.getTreeLevel();
        return TreeLevel.ROOT.equals(level) || TreeLevel.PARENT.equals(level);
    }

    /**
     * Updates the status span to show the last saved timestamp for the given
     * item. Applies the "saved" CSS class and hides the span if there is no
     * saved timestamp.
     *
     * @param item   the tier item associated with the status
     * @param status the status span to update
     */
    private void applySavedStatus(T item, Span status) {
        status.setText(UiUtil.tierSavedText(lastSavedByTier.get(tierDtoOf(item).getId())));
        status.addClassName(Css.TIER_SAVED_AT);
        status.setVisible(status.getText() != null);
    }

    /**
     * Handles the checkbox value change event by updating the item's active
     * state, saving it, and rolling back if necessary. Disables the checkbox
     * during the save operation and shows a "Saving..." status.
     *
     * @param item        the tier item associated with the checkbox
     * @param checkbox    the checkbox component that was changed
     * @param status      the status span to update with save information
     * @param rollingBack a flag indicating whether a rollback is in progress
     * @param event       the value change event containing the new and old values
     */
    private void onCheckboxChanged(T item, Checkbox checkbox, Span status, boolean[] rollingBack,
                                   HasValue.ValueChangeEvent<Boolean> event) {
        if (rollingBack[0]) {
            return;
        }
        boolean previous = event.getOldValue();
        item.setActive(event.getValue());
        T rootItem = tierRoot(item);
        if (ObjectUtils.isEmpty(rootItem)) {
            return;
        }
        Integer tierId = tierDtoOf(rootItem).getId();
        Date priorSaved = lastSavedByTier.get(tierId);
        lastSavedByTier.put(tierId, new Date());
        checkbox.setEnabled(false);
        showSaving(status);
        saveCheckbox(item, rootItem, () -> rollbackToggle(checkbox, status, tierId, priorSaved, previous, rollingBack));
    }

    /**
     * Updates the status span to show a "Saving..." message and applies the
     * saving CSS class.
     *
     * @param status the status span to update
     */
    private void showSaving(Span status) {
        status.removeClassName(Css.TIER_SAVED_AT);
        status.setText(Messages.get("grid.tier.saving"));
        status.addClassName(Css.TIER_SAVING);
        status.setVisible(true);
    }

    /**
     * Rolls back the checkbox toggle to its previous state, restoring the
     * saved timestamp and re-enabling the checkbox.
     *
     * @param checkbox   the checkbox to roll back
     * @param status     the status span to update
     * @param tierId     the ID of the tier being rolled back
     * @param priorSaved the previous saved timestamp, or null if none
     * @param previous   the previous checkbox value
     * @param rollingBack a flag indicating whether a rollback is in progress
     */
    private void rollbackToggle(Checkbox checkbox, Span status, Integer tierId, Date priorSaved,
                                boolean previous, boolean[] rollingBack) {
        rollingBack[0] = true;
        checkbox.setEnabled(true);
        if (priorSaved != null) {
            lastSavedByTier.put(tierId, priorSaved);
        } else {
            lastSavedByTier.remove(tierId);
        }
        status.removeClassName(Css.TIER_SAVING);
        String savedText = UiUtil.tierSavedText(priorSaved);
        status.setText(savedText);
        if (savedText == null) {
            status.setVisible(false);
        } else {
            status.addClassName(Css.TIER_SAVED_AT);
        }
        checkbox.setValue(previous);
        rollingBack[0] = false;
    }

    protected Component applyButton(T item) {
        if (item.getTreeLevel().equals(TreeLevel.ROOT)) {
            HorizontalLayout layout = new HorizontalLayout();
            layout.add(applyButtonEdit(item));
            layout.add(applyButtonDelete(item));
            return layout;
        }
        return null;
    }

    /** Creates the delete button for the given tier item and stores it in the
     * buttonDeletes array for later reference.
     *
     * @param item the tier item for which to create the delete button
     * @return the delete button component
     */
    protected Component applyButtonDelete(T item) {
        buttonDeletes[item.getRootIndex()] = deleteButtonFor(item);
        return buttonDeletes[item.getRootIndex()];
    }

    /** Creates the edit button for the given tier item and stores it in the
     * buttonEdits array for later reference.
     *
     * @param item the tier item for which to create the edit button
     * @return the edit button component
     */
    protected Component applyButtonEdit(T item) {
        buttonEdits[item.getRootIndex()] = UiUtil.editButton(Messages.get(Messages.Keys.ACTION_EDIT_NAME_FLAT),
                event -> editTier(tierDtoOf(item), FormAction.EDIT));
        return buttonEdits[item.getRootIndex()];
    }

    /**
     * Swaps the freshly built tree data onto the grid on the UI thread and
     * restores any previously expanded rows.
     *
     * @param treeData the grid content to show
     */
    protected void finishLoad(TreeData<T> treeData) {
        UiUtil.safeAccess(ui, () -> {
            gridSkeleton.hide();
            treeGrid().setTreeData(treeData);
            restoreExpansion();
        });
    }
}