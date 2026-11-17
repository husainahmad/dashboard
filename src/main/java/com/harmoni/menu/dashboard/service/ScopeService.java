package com.harmoni.menu.dashboard.service;

import com.harmoni.menu.dashboard.dto.AssignmentDto;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.layout.security.ScopeLabels;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Resolves the signed-in user's organisation scope from the RBAC assignments
 * stored in the session.
 *
 * <p>Access is the union of every assignment: a {@code BRAND} row grants the
 * brand and everything under it, a {@code CHAIN} row grants the chain and its
 * stores, and a {@code STORE} row grants only that store. When the user has no
 * assignments the signed-in store becomes the scope. A user with neither
 * assignments nor a store keeps the legacy unrestricted behaviour.</p>
 *
 * <p>The resulting {@link ScopeSnapshot} is what the UI uses to filter brand,
 * chain and store selectors and to decide whether those selectors can still be
 * changed.</p>
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class ScopeService implements Serializable {

    private static volatile ScopeService instance;

    private final AuthorizationService authorizationService;
    private final AccessService accessService;

    @PostConstruct
    void register() {
        instance = this;
    }

    /**
     * Returns the Spring-managed instance for views that are created manually
     * (Vaadin route components are not always constructed by the container).
     *
     * @return the shared service, or {@code null} during early startup
     */
    public static ScopeService current() {
        return instance;
    }

    /**
     * Builds the current scope from the session authorities and store.
     *
     * @return the resolved scope, never {@code null}
     */
    public ScopeSnapshot snapshot() {
        List<AssignmentDto> scopes = authorizationService.getScopes();
        Set<Integer> brandIds = new LinkedHashSet<>();
        Set<Integer> chainIds = new LinkedHashSet<>();
        Set<Integer> storeIds = new LinkedHashSet<>();
        boolean hasBrand = false;
        boolean hasChain = false;
        boolean hasStore = false;
        boolean hasGlobal = false;

        for (AssignmentDto scope : scopes) {
            Integer id = scope.getScopeId() == null ? null : scope.getScopeId().intValue();
            if (id == null) {
                continue;
            }
            switch (scope.getScopeType() == null ? "" : scope.getScopeType()) {
                case ScopeLabels.BRAND -> {
                    brandIds.add(id);
                    hasBrand = true;
                }
                case ScopeLabels.CHAIN -> {
                    chainIds.add(id);
                    hasChain = true;
                }
                case ScopeLabels.STORE -> {
                    storeIds.add(id);
                    hasStore = true;
                }
                case "GLOBAL" -> hasGlobal = true;
                default -> {
                    // Unknown scope types are ignored rather than widening access.
                }
            }
        }

        StoreDto store = accessService.getStore();
        boolean hasStoreContext = store != null;
        if (scopes.isEmpty() && hasStoreContext) {
            addIfNotNull(brandIds, accessService.getBrandId());
            addIfNotNull(chainIds, accessService.getChainId());
            addIfNotNull(storeIds, accessService.getStoreId());
            hasStore = true;
        }
        if (!hasBrand && (hasChain || hasStore || scopes.isEmpty())) {
            addIfNotNull(brandIds, accessService.getBrandId());
        }
        if (hasStore && chainIds.isEmpty()) {
            addIfNotNull(chainIds, accessService.getChainId());
        }

        boolean global = hasGlobal || (scopes.isEmpty() && !hasStoreContext);
        if (global) {
            brandIds.clear();
            chainIds.clear();
            storeIds.clear();
        }
        boolean storeScoped = !global && !hasBrand && !hasChain;
        return new ScopeSnapshot(global, storeScoped, hasBrand, brandIds, chainIds, storeIds,
                accessService.getBrandId(), accessService.getChainId(), accessService.getStoreId());
    }

    private static void addIfNotNull(Set<Integer> target, Integer value) {
        if (value != null) {
            target.add(value);
        }
    }

    /**
     * Immutable view of the user's accessible organisation ids.
     */
    public static final class ScopeSnapshot implements Serializable {

        private final boolean global;
        private final boolean storeScoped;
        private final boolean brandScoped;
        private final Set<Integer> brandIds;
        private final Set<Integer> chainIds;
        private final Set<Integer> storeIds;
        private final Integer loginBrandId;
        private final Integer loginChainId;
        private final Integer loginStoreId;

        ScopeSnapshot(boolean global, boolean storeScoped, boolean brandScoped,
                      Set<Integer> brandIds, Set<Integer> chainIds, Set<Integer> storeIds,
                      Integer loginBrandId, Integer loginChainId, Integer loginStoreId) {
            this.global = global;
            this.storeScoped = storeScoped;
            this.brandScoped = brandScoped;
            this.brandIds = Collections.unmodifiableSet(new LinkedHashSet<>(brandIds));
            this.chainIds = Collections.unmodifiableSet(new LinkedHashSet<>(chainIds));
            this.storeIds = Collections.unmodifiableSet(new LinkedHashSet<>(storeIds));
            this.loginBrandId = loginBrandId;
            this.loginChainId = loginChainId;
            this.loginStoreId = loginStoreId;
        }

        public boolean isGlobal() {
            return global;
        }

        public boolean isStoreScoped() {
            return storeScoped;
        }

        /**
         * Whether the user holds a BRAND-level assignment. Brand owners may
         * switch between the brands assigned to them; chain- and store-scoped
         * users are pinned to the brand of their chain or store.
         *
         * @return {@code true} when a BRAND assignment is present
         */
        public boolean isBrandScoped() {
            return brandScoped;
        }

        public Integer getLoginBrandId() {
            return loginBrandId;
        }

        public Integer getLoginChainId() {
            return loginChainId;
        }

        public Integer getLoginStoreId() {
            return loginStoreId;
        }

        public boolean isBrandLocked() {
            return !global && !brandScoped;
        }

        /**
         * Whether the brand selector should be enabled for the given number of
         * options. Brand owners may switch between their assigned brands even
         * when only one is available; chain- and store-scoped users are locked.
         *
         * @param optionCount the number of brands the user can access
         * @return {@code true} when the selector should be enabled
         */
        public boolean canSelectBrand(long optionCount) {
            if (global) {
                return optionCount > 1;
            }
            return brandScoped && optionCount > 0;
        }

        public boolean isChainLocked() {
            return !global && (storeScoped || chainIds.size() == 1);
        }

        public boolean isStoreLocked() {
            return !global && storeIds.size() == 1;
        }

        public boolean canAccessBrand(Integer brandId) {
            return global || brandId == null || brandIds.contains(brandId);
        }

        /**
         * Whether a chain can be shown. A brand-level assignment covers every
         * chain of that brand; a chain-level assignment covers only its chains.
         */
        public boolean canAccessChain(Integer chainId, Integer brandId) {
            if (global) {
                return true;
            }
            if (!chainIds.isEmpty()) {
                return chainIds.contains(chainId);
            }
            return brandIds.contains(brandId);
        }

        /**
         * Whether a store can be shown. Store-level users see only their
         * stores; chain-level users see every store of the chain; brand-level
         * users see every store of the already-authorised chain.
         */
        public boolean canAccessStore(Integer storeId, Integer chainId) {
            if (global) {
                return true;
            }
            if (storeScoped) {
                return storeIds.contains(storeId);
            }
            if (!chainIds.isEmpty()) {
                return chainIds.contains(chainId);
            }
            return true;
        }

        public List<BrandDto> filterBrands(List<BrandDto> brands) {
            if (brands == null) {
                return new ArrayList<>();
            }
            if (global) {
                return new ArrayList<>(brands);
            }
            return brands.stream()
                    .filter(brand -> canAccessBrand(brand.getId()))
                    .toList();
        }

        public List<ChainDto> filterChains(List<ChainDto> chains) {
            if (chains == null) {
                return new ArrayList<>();
            }
            if (global) {
                return new ArrayList<>(chains);
            }
            if (!chainIds.isEmpty()) {
                return chains.stream()
                        .filter(chain -> chainIds.contains(chain.getId()))
                        .toList();
            }
            return chains.stream()
                    .filter(chain -> brandIds.contains(chain.getBrandId()))
                    .toList();
        }

        public List<StoreDto> filterStores(List<StoreDto> stores) {
            if (stores == null) {
                return new ArrayList<>();
            }
            if (global) {
                return new ArrayList<>(stores);
            }
            if (storeScoped) {
                return stores.stream()
                        .filter(store -> storeIds.contains(store.getId()))
                        .toList();
            }
            if (!chainIds.isEmpty()) {
                return stores.stream()
                        .filter(store -> chainIds.contains(store.getChainId()))
                        .toList();
            }
            return new ArrayList<>(stores);
        }

        public BrandDto defaultBrand(List<BrandDto> brands) {
            if (brands == null || brands.isEmpty()) {
                return null;
            }
            return brands.stream()
                    .filter(brand -> Objects.equals(brand.getId(), loginBrandId))
                    .findFirst()
                    .orElse(brands.getFirst());
        }

        public ChainDto defaultChain(List<ChainDto> chains) {
            if (chains == null || chains.isEmpty()) {
                return null;
            }
            return chains.stream()
                    .filter(chain -> Objects.equals(chain.getId(), loginChainId))
                    .findFirst()
                    .orElse(chains.getFirst());
        }

        public StoreDto defaultStore(List<StoreDto> stores) {
            if (stores == null || stores.isEmpty()) {
                return null;
            }
            return stores.stream()
                    .filter(store -> Objects.equals(store.getId(), loginStoreId))
                    .findFirst()
                    .orElse(stores.getFirst());
        }
    }
}
