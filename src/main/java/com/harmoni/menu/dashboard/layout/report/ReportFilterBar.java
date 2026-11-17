package com.harmoni.menu.dashboard.layout.report;

import com.harmoni.menu.dashboard.dto.OrganizationTreeDto;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.service.ScopeService;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * Report filter bar: Brand, Chain, Store, From, To and Apply.
 *
 * <p>Brand, Chain and Store cascade. Leaving Chain and Store empty reports on
 * every store in the selected brand; leaving Store empty reports on every store
 * in the selected chain; selecting a store narrows the report to that store.</p>
 */
public class ReportFilterBar extends HorizontalLayout {

    private static final int DEFAULT_RANGE_DAYS = 7;

    private final ComboBox<OrganizationTreeDto.BrandNode> brandBox =
            new ComboBox<>(Messages.get("report.filter.brand"));
    private final ComboBox<OrganizationTreeDto.ChainNode> chainBox =
            new ComboBox<>(Messages.get("report.filter.chain"));
    private final ComboBox<OrganizationTreeDto.StoreNode> storeBox =
            new ComboBox<>(Messages.get("report.filter.store"));
    private final DatePicker startPicker = new DatePicker(Messages.get("report.range.start"));
    private final DatePicker endPicker = new DatePicker(Messages.get("report.range.end"));
    private final BiConsumer<DateRange, List<Integer>> onApply;

    private boolean initializing;

    /**
     * @param onApply receives the selected range and the store ids it covers
     */
    public ReportFilterBar(BiConsumer<DateRange, List<Integer>> onApply) {
        this.onApply = onApply;

        LocalDate today = LocalDate.now();
        startPicker.setValue(today.minusDays(DEFAULT_RANGE_DAYS - 1L));
        endPicker.setValue(today);
        startPicker.setRequired(true);
        endPicker.setRequired(true);

        brandBox.setItemLabelGenerator(OrganizationTreeDto.BrandNode::getName);
        chainBox.setItemLabelGenerator(OrganizationTreeDto.ChainNode::getName);
        storeBox.setItemLabelGenerator(OrganizationTreeDto.StoreNode::getName);
        brandBox.setClearButtonVisible(true);
        chainBox.setClearButtonVisible(true);
        storeBox.setClearButtonVisible(true);
        brandBox.setWidth("180px");
        chainBox.setWidth("180px");
        storeBox.setWidth("180px");

        brandBox.addValueChangeListener(event -> onBrandChanged(event.getValue()));
        chainBox.addValueChangeListener(event -> onChainChanged(event.getValue()));

        Button apply = new Button(Messages.get("report.range.apply"), VaadinIcon.REFRESH.create(),
                event -> apply());
        apply.addClassName("small-button");

        add(brandBox, chainBox, storeBox, startPicker, endPicker, apply);
        addClassName(Css.TOOLBAR);
        setWidthFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setPadding(false);
    }

    /**
     * Populates the brand list and selects the brand/chain/store of the given
     * store, when it can be found.
     *
     * @param tree           the organization tree, may be {@code null}
     * @param defaultStoreId the store to preselect, may be {@code null}
     */
    public void setOrganization(OrganizationTreeDto tree, Integer defaultStoreId) {
        initializing = true;
        ScopeService.ScopeSnapshot scope = scope();
        List<OrganizationTreeDto.BrandNode> brands = tree == null || tree.getBrands() == null
                ? List.of() : tree.getBrands().stream()
                        .filter(brand -> scope == null || scope.canAccessBrand(brand.getId()))
                        .toList();
        brandBox.setItems(brands);
        brandBox.setEnabled(scope == null ? brands.size() > 1
                : scope.canSelectBrand(brands.size()));
        chainBox.setItems(List.of());
        storeBox.setItems(List.of());
        brandBox.clear();
        chainBox.clear();
        storeBox.clear();
        Integer storeId = defaultStoreId != null ? defaultStoreId
                : scope == null ? null : scope.getLoginStoreId();
        if (storeId != null) {
            selectDefault(brands, storeId, scope);
        }
        initializing = false;
    }

    private void selectDefault(List<OrganizationTreeDto.BrandNode> brands, Integer storeId,
                               ScopeService.ScopeSnapshot scope) {
        for (OrganizationTreeDto.BrandNode brand : brands) {
            List<OrganizationTreeDto.ChainNode> chains = filterChains(brand, scope);
            for (OrganizationTreeDto.ChainNode chain : chains) {
                List<OrganizationTreeDto.StoreNode> stores = filterStores(chain, scope);
                for (OrganizationTreeDto.StoreNode store : stores) {
                    if (storeId.equals(store.getId())) {
                        brandBox.setValue(brand);
                        chainBox.setItems(chains);
                        chainBox.setEnabled(scope == null || (!scope.isChainLocked() && chains.size() > 1));
                        chainBox.setValue(chain);
                        storeBox.setItems(stores);
                        storeBox.setEnabled(scope == null || (!scope.isStoreLocked() && stores.size() > 1));
                        storeBox.setValue(store);
                        return;
                    }
                }
            }
        }
    }

    private void onBrandChanged(OrganizationTreeDto.BrandNode brand) {
        if (initializing) {
            return;
        }
        ScopeService.ScopeSnapshot scope = scope();
        List<OrganizationTreeDto.ChainNode> chains = brand == null
                ? List.of() : filterChains(brand, scope);
        chainBox.setItems(chains);
        chainBox.setEnabled(scope == null || (!scope.isChainLocked() && chains.size() > 1));
        chainBox.clear();
        storeBox.setItems(List.of());
        storeBox.clear();
    }

    private void onChainChanged(OrganizationTreeDto.ChainNode chain) {
        if (initializing) {
            return;
        }
        ScopeService.ScopeSnapshot scope = scope();
        List<OrganizationTreeDto.StoreNode> stores = chain == null
                ? List.of() : filterStores(chain, scope);
        storeBox.setItems(stores);
        storeBox.setEnabled(scope == null || (!scope.isStoreLocked() && stores.size() > 1));
        storeBox.clear();
    }

    private static List<OrganizationTreeDto.ChainNode> filterChains(
            OrganizationTreeDto.BrandNode brand, ScopeService.ScopeSnapshot scope) {
        if (scope == null || brand == null || brand.getChains() == null) {
            return brand == null || brand.getChains() == null ? List.of() : brand.getChains();
        }
        return brand.getChains().stream()
                .filter(chain -> scope.canAccessChain(chain.getId(), brand.getId()))
                .toList();
    }

    private static List<OrganizationTreeDto.StoreNode> filterStores(
            OrganizationTreeDto.ChainNode chain, ScopeService.ScopeSnapshot scope) {
        if (scope == null || chain == null || chain.getStores() == null) {
            return chain == null || chain.getStores() == null ? List.of() : chain.getStores();
        }
        return chain.getStores().stream()
                .filter(store -> scope.canAccessStore(store.getId(), chain.getId()))
                .toList();
    }

    private static ScopeService.ScopeSnapshot scope() {
        ScopeService service = ScopeService.current();
        return service == null ? null : service.snapshot();
    }

    /**
     * Returns the selected date range.
     *
     * @return the selected range
     */
    public DateRange getRange() {
        return new DateRange(startPicker.getValue(), endPicker.getValue());
    }

    /**
     * Resolves the selected scope to store ids.
     *
     * @return the store ids, empty when no scope is selected
     */
    public List<Integer> getStoreIds() {
        OrganizationTreeDto.StoreNode store = storeBox.getValue();
        if (store != null) {
            return List.of(store.getId());
        }
        OrganizationTreeDto.ChainNode chain = chainBox.getValue();
        if (chain != null) {
            return chain.getStores().stream()
                    .map(OrganizationTreeDto.StoreNode::getId)
                    .toList();
        }
        OrganizationTreeDto.BrandNode brand = brandBox.getValue();
        if (brand != null) {
            List<Integer> ids = new ArrayList<>();
            for (OrganizationTreeDto.ChainNode c : brand.getChains()) {
                for (OrganizationTreeDto.StoreNode s : c.getStores()) {
                    ids.add(s.getId());
                }
            }
            return ids;
        }
        return List.of();
    }

    /**
     * Validates the range and notifies the listener with the selected scope.
     */
    public void apply() {
        DateRange range = getRange();
        if (!range.isValid()) {
            UiUtil.error(Messages.get("report.range.invalid"));
            return;
        }
        onApply.accept(range, getStoreIds());
    }
}
