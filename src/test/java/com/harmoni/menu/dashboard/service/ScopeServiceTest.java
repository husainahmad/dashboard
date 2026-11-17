package com.harmoni.menu.dashboard.service;

import com.harmoni.menu.dashboard.dto.AssignmentDto;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.dto.StoreDto;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ScopeServiceTest {

    private final AuthorizationService authorization = mock(AuthorizationService.class);
    private final AccessService access = mock(AccessService.class);
    private final ScopeService service = new ScopeService(authorization, access);

    private void loginStore(int brandId, int chainId, int storeId) {
        StoreDto store = new StoreDto();
        store.setId(storeId);
        store.setChainId(chainId);
        when(access.getStore()).thenReturn(store);
        when(access.getBrandId()).thenReturn(brandId);
        when(access.getChainId()).thenReturn(chainId);
        when(access.getStoreId()).thenReturn(storeId);
    }

    private static AssignmentDto scope(String type, long id) {
        return AssignmentDto.builder().scopeType(type).scopeId(id).build();
    }

    @Test
    void brandScopeShowsOnlyThatBrandAndUnlocksChains() {
        loginStore(1, 10, 100);
        when(authorization.getScopes()).thenReturn(List.of(scope("BRAND", 1)));

        ScopeService.ScopeSnapshot snapshot = service.snapshot();

        assertTrue(snapshot.isBrandScoped());
        assertFalse(snapshot.isBrandLocked());
        assertTrue(snapshot.canSelectBrand(1));
        assertFalse(snapshot.isChainLocked());
        assertFalse(snapshot.isStoreScoped());
        assertEquals(List.of(1), snapshot.filterBrands(List.of(brand(1), brand(2)))
                .stream().map(BrandDto::getId).toList());
    }

    @Test
    void storeScopeLocksBrandChainAndStore() {
        loginStore(1, 10, 100);
        when(authorization.getScopes()).thenReturn(List.of(scope("STORE", 100)));

        ScopeService.ScopeSnapshot snapshot = service.snapshot();

        assertTrue(snapshot.isStoreScoped());
        assertTrue(snapshot.isBrandLocked());
        assertFalse(snapshot.canSelectBrand(1));
        assertTrue(snapshot.isChainLocked());
        assertTrue(snapshot.isStoreLocked());
        assertEquals(List.of(100), snapshot.filterStores(List.of(store(100, 10), store(101, 10)))
                .stream().map(StoreDto::getId).toList());
    }

    @Test
    void chainScopeKeepsOnlyAssignedChainsAndTheirStores() {
        loginStore(1, 10, 100);
        when(authorization.getScopes()).thenReturn(List.of(scope("CHAIN", 10)));

        ScopeService.ScopeSnapshot snapshot = service.snapshot();

        assertFalse(snapshot.isStoreScoped());
        assertTrue(snapshot.isBrandLocked());
        assertFalse(snapshot.canSelectBrand(1));
        assertTrue(snapshot.isChainLocked());
        assertEquals(List.of(10), snapshot.filterChains(List.of(chain(10, 1), chain(11, 1)))
                .stream().map(ChainDto::getId).toList());
        assertEquals(List.of(100), snapshot.filterStores(List.of(store(100, 10), store(200, 20)))
                .stream().map(StoreDto::getId).toList());
    }

    @Test
    void noScopesFallsBackToTheSignedInStore() {
        loginStore(1, 10, 100);
        when(authorization.getScopes()).thenReturn(List.of());

        ScopeService.ScopeSnapshot snapshot = service.snapshot();

        assertFalse(snapshot.isGlobal());
        assertTrue(snapshot.isBrandLocked());
        assertTrue(snapshot.isChainLocked());
        assertTrue(snapshot.isStoreLocked());
    }

    @Test
    void noScopesAndNoStoreKeepsLegacyAccess() {
        when(authorization.getScopes()).thenReturn(List.of());
        when(access.getStore()).thenReturn(null);

        ScopeService.ScopeSnapshot snapshot = service.snapshot();

        assertTrue(snapshot.isGlobal());
        assertFalse(snapshot.isBrandLocked());
        assertEquals(List.of(1, 2), snapshot.filterBrands(List.of(brand(1), brand(2)))
                .stream().map(BrandDto::getId).toList());
    }

    private static BrandDto brand(int id) {
        BrandDto brand = new BrandDto();
        brand.setId(id);
        return brand;
    }

    private static ChainDto chain(int id, int brandId) {
        ChainDto chain = new ChainDto();
        chain.setId(id);
        chain.setBrandId(brandId);
        return chain;
    }

    private static StoreDto store(int id, int chainId) {
        StoreDto store = new StoreDto();
        store.setId(id);
        store.setChainId(chainId);
        return store;
    }
}
