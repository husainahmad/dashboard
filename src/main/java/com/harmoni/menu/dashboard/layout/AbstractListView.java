package com.harmoni.menu.dashboard.layout;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.layout.util.GridSkeleton;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.shared.Registration;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Shared foundation for every grid list view in the dashboard.
 *
 * <p>Centralises the logic that was previously copy-pasted across all lists:
 * UI locking via {@link UiUtil#safeAccess}, broadcast refresh registration
 * with automatic unregister on detach, the standard search filter, the brand
 * selector, the grid + skeleton overlay slot and the pagination footer.</p>
 */
@Slf4j
public abstract class AbstractListView extends VerticalLayout {

    /** The UI this view is attached to; set on attach. */
    protected UI ui;

    /** Standard lazy search filter shared by the toolbars. */
    protected final TextField filterText = new TextField();

    /** Brand selector shown in brand-scoped toolbars. */
    protected final ComboBox<BrandDto> brandFilter = new ComboBox<>();

    /** Brands backing {@link #brandFilter}. */
    protected List<BrandDto> brandOptions = new ArrayList<>();

    /** Fields that suppress the {@code n} shortcut while focused. */
    protected final Set<Component> shortcutTypingFields = new HashSet<>();

    /** Pagination state shared by the paged lists. */
    protected int currentPage = 1;
    protected int totalPages;
    protected final Text pageInfoText = new Text("");
    protected Button previousPageButton;
    protected Button nextPageButton;

    private Registration broadcasterRegistration;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        ui = attachEvent.getUI();
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        if (broadcasterRegistration != null) {
            broadcasterRegistration.remove();
            broadcasterRegistration = null;
        }
    }

    /**
     * Registers a broadcast listener that runs {@code refresh} on the UI
     * thread whenever one of the given message types arrives. The registration
     * is removed automatically on detach.
     *
     * @param types   broadcast types that trigger the refresh
     * @param refresh action refreshing the view, e.g. a grid fetch
     */
    protected void refreshOnBroadcast(Set<String> types, Runnable refresh) {
        trackBroadcast(Broadcaster.register(message -> {
            try {
                BroadcastMessage broadcast =
                        (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
                if (ObjectUtils.isNotEmpty(broadcast) && ObjectUtils.isNotEmpty(broadcast.getType())
                        && types.contains(broadcast.getType())) {
                    UiUtil.safeAccess(ui, refresh);
                }
            } catch (JsonProcessingException e) {
                log.error("Broadcast handler error", e);
            }
        }));
    }

    /**
     * Tracks a custom broadcast registration so it is removed on detach.
     *
     * @param registration the registration to track
     */
    protected void trackBroadcast(Registration registration) {
        if (broadcasterRegistration != null) {
            broadcasterRegistration.remove();
        }
        broadcasterRegistration = registration;
    }

    /**
     * Applies the standard search-filter setup: placeholder, clear button,
     * search icon and lazy value-change mode.
     */
    protected void configureSearchFilter() {
        filterText.setPlaceholder(Messages.get("placeholder.filterByName"));
        filterText.setClearButtonVisible(true);
        filterText.setPrefixComponent(VaadinIcon.SEARCH.create());
        filterText.setValueChangeMode(ValueChangeMode.LAZY);
    }

    /**
     * Registers the shared list shortcuts ({@code /} focuses the filter,
     * {@code n} runs the new-item action).
     *
     * @param newAction the "create new" action bound to {@code n}
     */
    protected void registerNewShortcut(Runnable newAction) {
        UiUtil.registerListShortcuts(this, shortcutTypingFields, filterText, newAction);
    }

    /**
     * Wraps a grid with the shimmer skeleton overlay used while data reloads.
     *
     * @param grid     the grid to show
     * @param skeleton the skeleton covering the grid while loading
     * @return the content layout holding both
     */
    protected HorizontalLayout gridSlot(Component grid, GridSkeleton skeleton) {
        VerticalLayout slot = new VerticalLayout(grid, skeleton);
        slot.addClassName("grid-slot");
        slot.setSizeFull();
        slot.setPadding(false);
        slot.setFlexGrow(1, grid);
        HorizontalLayout content = new HorizontalLayout(slot);
        content.setFlexGrow(1, slot);
        content.addClassNames("content");
        content.setSizeFull();
        return content;
    }

    /**
     * Configures the shared brand selector; {@code onChange} runs only for
     * user-driven selection changes.
     *
     * @param onChange action reloading the view for the selected brand
     */
    protected void configureBrandFilter(Runnable onChange) {
        brandFilter.setLabel(Messages.get(Messages.Keys.LABEL_BRAND));
        brandFilter.setItemLabelGenerator(BrandDto::getName);
        brandFilter.addValueChangeListener(change -> {
            if (!change.isFromClient()) {
                return;
            }
            if (change.getValue() == null && change.getOldValue() != null) {
                // The client combo box auto-clears a programmatically selected
                // value when its lazy item data confirms (Vaadin items/value
                // round-trip race). These views require a brand, so restore it.
                restoreBrandSelection(change.getOldValue());
                return;
            }
            if (change.getValue() != null) {
                onChange.run();
            }
        });
    }

    /**
     * Loads the brands into {@link #brandFilter}, selecting the first brand on
     * first visit, then runs {@code afterLoad} (typically the grid fetch).
     *
     * @param orgService organization client loading the brands
     * @param access     access service resolving the session brand fallback
     * @param afterLoad  action running once the selection is ready
     */
    protected void loadBrands(AsyncRestClientOrganizationService orgService,
                              AccessService access, Runnable afterLoad) {
        orgService.getAllBrandAsync(result -> UiUtil.safeAccess(ui, () -> {
            if (result == null || result.isEmpty()) {
                log.warn("Brand reload returned no brands; keeping current selection");
                afterLoad.run();
                return;
            }
            Integer keepId = brandFilter.getValue() != null ? brandFilter.getValue().getId() : null;
            brandOptions = result;
            brandFilter.setItems(brandOptions);
            BrandDto keep = null;
            if (keepId != null) {
                keep = brandOptions.stream()
                        .filter(brand -> keepId.equals(brand.getId()))
                        .findFirst()
                        .orElse(null);
            }
            BrandDto selected = keep != null ? keep : brandOptions.getFirst();
            brandFilter.setValue(selected);
            afterLoad.run();
        }), error -> UiUtil.safeAccess(ui, () ->
                UiUtil.errorWithRetry(Messages.get(Messages.Keys.NOTIFICATION_BRAND_LOAD_FAILED),
                        () -> loadBrands(orgService, access, afterLoad))));
    }

    /**
     * Returns the selected brand id, falling back to the session brand when
     * nothing is selected yet.
     *
     * @param access access service resolving the session brand
     * @return the brand id to query
     */
    protected Integer selectedBrandId(AccessService access) {
        BrandDto selected = brandFilter.getValue();
        if (selected != null && selected.getId() != null) {
            return selected.getId();
        }
        return sessionBrandId(access);
    }

    /**
     * Re-selects a brand that the client combo box auto-cleared. Only restores
     * the selection if the previous value still exists among the current
     * {@link #brandOptions}.
     *
     * @param previous the value the combo box cleared on the client side
     */
    private void restoreBrandSelection(BrandDto previous) {
        if (previous == null || previous.getId() == null) {
            return;
        }
        brandOptions.stream()
                .filter(brand -> previous.getId().equals(brand.getId()))
                .findFirst()
                .ifPresent(brandFilter::setValue);
    }

    /**
     * Returns the session user's brand id.
     *
     * @param access access service resolving the session brand
     * @return the session brand id
     */
    protected Integer sessionBrandId(AccessService access) {
        return access.getUserDetail().getStoreDto().getChainDto().getBrandId();
    }

    /**
     * Builds the standard pagination footer wired to the given actions. The
     * buttons are also kept in {@link #previousPageButton} /
     * {@link #nextPageButton} so {@link #updatePagination()} can enable them
     * according to the current page.
     *
     * @param onPrevious action going to the previous page
     * @param onNext     action going to the next page
     * @return the footer layout
     */
    protected HorizontalLayout paginationFooter(Runnable onPrevious, Runnable onNext) {
        previousPageButton = new Button(Messages.get("action.previous"), event -> onPrevious.run());
        nextPageButton = new Button(Messages.get("action.next"), event -> onNext.run());
        HorizontalLayout footer = new HorizontalLayout(previousPageButton, pageInfoText, nextPageButton);
        footer.addClassName("pagination");
        footer.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        footer.setWidthFull();
        footer.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        updatePagination();
        return footer;
    }

    /**
     * Refreshes the page info caption and the pagination button states from
     * {@link #currentPage} and {@link #totalPages}.
     */
    protected void updatePagination() {
        pageInfoText.setText(Messages.get("pagination.page", currentPage, totalPages));
        if (previousPageButton != null) {
            previousPageButton.setEnabled(currentPage > 1);
            nextPageButton.setEnabled(currentPage < totalPages);
        }
    }
}
