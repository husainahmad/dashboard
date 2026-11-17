package com.harmoni.menu.dashboard.layout.component;

import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.ScopeService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.combobox.ComboBox;
import org.apache.commons.lang3.ObjectUtils;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Loads the brand → chain → store cascade shared by the floor and table forms.
 *
 * <p>Selecting a brand loads its chains, and selecting a chain loads its
 * stores. The caller supplies the ids to preselect (the edit target, or the
 * session store) and whether a missing match should fall back to the first
 * option. The store selection itself is left to the caller, which usually
 * reacts to it by loading a dependent list such as floors.</p>
 */
public class StoreCascade implements Serializable {

    private static final int PAGE_SIZE = 1000;

    /**
     * Ids to preselect at each level. A {@code null} id means "no preference".
     *
     * @param brandId the brand to select
     * @param chainId the chain to select
     * @param storeId the store to select
     */
    public record Defaults(Integer brandId, Integer chainId, Integer storeId) {
    }

    private final transient AsyncRestClientOrganizationService organizationService;
    private final transient Supplier<UI> uiSupplier;
    private final ComboBox<BrandDto> brandBox;
    private final ComboBox<ChainDto> chainBox;
    private final ComboBox<StoreDto> storeBox;
    private final Defaults defaults;
    private final boolean selectFirstWhenMissing;

    /**
     * @param organizationService    client loading brands, chains and stores
     * @param uiSupplier             resolves the UI used to marshal callbacks
     * @param brandBox               the brand selector
     * @param chainBox               the chain selector
     * @param storeBox               the store selector
     * @param defaults               ids to preselect at each level
     * @param selectFirstWhenMissing whether to fall back to the first option
     */
    public StoreCascade(AsyncRestClientOrganizationService organizationService,
                        Supplier<UI> uiSupplier,
                        ComboBox<BrandDto> brandBox,
                        ComboBox<ChainDto> chainBox,
                        ComboBox<StoreDto> storeBox,
                        Defaults defaults,
                        boolean selectFirstWhenMissing) {
        this.organizationService = organizationService;
        this.uiSupplier = uiSupplier;
        this.brandBox = brandBox;
        this.chainBox = chainBox;
        this.storeBox = storeBox;
        this.defaults = defaults;
        this.selectFirstWhenMissing = selectFirstWhenMissing;

        configure();
        brandBox.addValueChangeListener(event -> loadChains());
        chainBox.addValueChangeListener(event -> loadStores());
    }

    /**
     * Starts the cascade by loading the brands.
     */
    public void load() {
        loadBrands();
    }

    private void configure() {
        brandBox.setItemLabelGenerator(BrandDto::getName);
        brandBox.setPlaceholder(Messages.get(Messages.Keys.PLACEHOLDER_SELECT_BRAND));
        brandBox.setAllowCustomValue(false);

        chainBox.setItemLabelGenerator(ChainDto::getName);
        chainBox.setPlaceholder(Messages.get(Messages.Keys.LABEL_CHAIN));
        chainBox.setAllowCustomValue(false);

        storeBox.setItemLabelGenerator(StoreDto::getName);
        storeBox.setPlaceholder(Messages.get("placeholder.selectStore"));
        storeBox.setAllowCustomValue(false);
    }

    private void loadBrands() {
        organizationService.getAllBrandAsync(
                result -> UiUtil.safeAccess(ui(), () -> {
                    if (ObjectUtils.isEmpty(result)) {
                        return;
                    }
                    ScopeService.ScopeSnapshot scope = scope();
                    List<BrandDto> allowed = scope == null ? result : scope.filterBrands(result);
                    if (ObjectUtils.isEmpty(allowed)) {
                        return;
                    }
                    brandBox.setItems(allowed);
                    brandBox.setEnabled(scope == null ? allowed.size() > 1
                            : scope.canSelectBrand(allowed.size()));
                    selectDefaultBrand(allowed, scope);
                }),
                error -> UiUtil.safeAccess(ui(), () ->
                        UiUtil.errorWithRetry(Messages.get(Messages.Keys.NOTIFICATION_BRAND_LOAD_FAILED),
                                this::loadBrands)));
    }

    private void selectDefaultBrand(List<BrandDto> brands, ScopeService.ScopeSnapshot scope) {
        BrandDto match = findBy(brands, BrandDto::getId, defaults.brandId());
        if (match == null && scope != null) {
            match = scope.defaultBrand(brands);
        }
        if (match == null && selectFirstWhenMissing) {
            match = brands.getFirst();
        }
        if (match != null) {
            brandBox.setValue(match);
        }
    }

    private void loadChains() {
        BrandDto brand = brandBox.getValue();
        resetDownstream();
        if (brand == null || brand.getId() == null) {
            return;
        }
        organizationService.getAllChainByBrandIdAsync(
                result -> UiUtil.safeAccess(ui(), () -> {
                    if (ObjectUtils.isEmpty(result)) {
                        return;
                    }
                    ScopeService.ScopeSnapshot scope = scope();
                    List<ChainDto> allowed = scope == null ? result : scope.filterChains(result);
                    if (ObjectUtils.isEmpty(allowed)) {
                        return;
                    }
                    chainBox.setItems(allowed);
                    chainBox.setEnabled(scope == null || (!scope.isChainLocked() && allowed.size() > 1));
                    selectDefaultChain(allowed, scope);
                }),
                error -> UiUtil.safeAccess(ui(), () ->
                        UiUtil.errorWithRetry(Messages.get(Messages.Keys.NOTIFICATION_CHAIN_LOAD_FAILED),
                                this::loadChains)),
                brand.getId());
    }

    private void selectDefaultChain(List<ChainDto> chains, ScopeService.ScopeSnapshot scope) {
        ChainDto match = findBy(chains, ChainDto::getId, defaults.chainId());
        if (match == null && scope != null) {
            match = scope.defaultChain(chains);
        }
        if (match == null && selectFirstWhenMissing) {
            match = chains.getFirst();
        }
        if (match != null) {
            chainBox.setValue(match);
        }
    }

    private void loadStores() {
        ChainDto chain = chainBox.getValue();
        storeBox.setItems(Collections.emptyList());
        storeBox.clear();
        if (chain == null || chain.getId() == null) {
            return;
        }
        organizationService.getAllStoreAsync(
                result -> UiUtil.safeAccess(ui(), () -> {
                    ScopeService.ScopeSnapshot scope = scope();
                    List<StoreDto> stores = ObjectUtil.extractDataList(result, StoreDto.class);
                    List<StoreDto> allowed = scope == null ? stores : scope.filterStores(stores);
                    storeBox.setItems(allowed);
                    storeBox.setEnabled(scope == null || (!scope.isStoreLocked() && allowed.size() > 1));
                    selectDefaultStore(allowed, scope);
                }),
                error -> UiUtil.safeAccess(ui(), () ->
                        UiUtil.errorWithRetry(Messages.get(Messages.Keys.NOTIFICATION_STORE_LOAD_FAILED),
                                this::loadStores)),
                chain.getId(), 1, PAGE_SIZE, "");
    }

    private void selectDefaultStore(List<StoreDto> stores, ScopeService.ScopeSnapshot scope) {
        Integer storeId = defaults.storeId();
        StoreDto match = stores.stream()
                .filter(store -> Objects.equals(store.getId(), storeId))
                .findFirst()
                .orElse(null);
        if (match == null && scope != null) {
            match = scope.defaultStore(stores);
        }
        if (match == null && selectFirstWhenMissing) {
            match = stores.isEmpty() ? null : stores.getFirst();
        }
        if (match != null) {
            storeBox.setValue(match);
        }
    }

    private static ScopeService.ScopeSnapshot scope() {
        ScopeService service = ScopeService.current();
        return service == null ? null : service.snapshot();
    }

    private void resetDownstream() {
        chainBox.setItems(Collections.emptyList());
        chainBox.clear();
        storeBox.setItems(Collections.emptyList());
        storeBox.clear();
    }

    private UI ui() {
        return uiSupplier.get();
    }

    private static <T> T findBy(List<T> items, Function<T, Integer> idOf, Integer wanted) {
        if (wanted == null) {
            return null;
        }
        return items.stream()
                .filter(item -> Objects.equals(idOf.apply(item), wanted))
                .findFirst()
                .orElse(null);
    }
}
