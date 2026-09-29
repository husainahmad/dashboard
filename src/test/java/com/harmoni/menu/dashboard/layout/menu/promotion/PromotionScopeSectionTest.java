package com.harmoni.menu.dashboard.layout.menu.promotion;

import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.dto.PromotionDto;
import com.harmoni.menu.dashboard.dto.PromotionTargetDto;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.layout.enums.PromotionScopeType;
import com.harmoni.menu.dashboard.layout.enums.PromotionType;
import com.harmoni.menu.dashboard.layout.enums.PromotionTargetType;
import com.harmoni.menu.dashboard.layout.organization.FormAction;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientBase;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.select.Select;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the target section's scope narrowing.
 *
 * <p>Three things it used to get wrong. The organization fields were rebuilt on every
 * scope change, so the operator's choice lived on a component nobody held a reference to
 * and could never be read back. The four scopes each repeated the same three statements.
 * And {@code ALL_STORES} was offered even though a promotion now always names at least a
 * brand, so a half-filled section read as a deliberate "everywhere" promotion.</p>
 */
class PromotionScopeSectionTest {

    @Test
    void allStores_isNotOfferedAsAScope() throws Exception {
        PromotionForm form = newForm(FormAction.CREATE, null);
        Select<PromotionScopeType> scopeSelect = scopeSelect(form);

        assertFalse(scopeSelect.getListDataView().getItems()
                        .anyMatch(PromotionScopeType.ALL_STORES::equals),
                "a promotion always names at least a brand, so offering All Stores would let a half-filled "
                        + "section be saved as a deliberate promotion across the whole system");
    }

    @Test
    void aNewPromotion_startsAtBrand() throws Exception {
        PromotionForm form = newForm(FormAction.CREATE, null);

        assertEquals(PromotionScopeType.BRAND, scopeSelect(form).getValue(),
                "Brand is the narrowest level that is always meaningful, so it is what a new promotion starts on");
    }

    @Test
    void brandScope_showsTheBrandAndNothingBelow() throws Exception {
        DialoglessPromotionForm form = newDialoglessForm(FormAction.CREATE, null);
        setScope(form, PromotionScopeType.BRAND);

        assertTrue(brandSelect(form).isVisible(), "Brand scope is narrowed by the brand");
        assertFalse(chainSelect(form).isVisible(),
                "a brand names its own chains, so asking for one would contradict the scope");
        assertFalse(storeSelect(form).isVisible(),
                "a brand narrows to the brand, not to some of its stores");
    }

    @Test
    void chainScope_showsBrandAndChainButNotStores() throws Exception {
        DialoglessPromotionForm form = newDialoglessForm(FormAction.CREATE, null);
        setScope(form, PromotionScopeType.CHAIN);

        assertTrue(brandSelect(form).isVisible(), "a chain is only identified within its brand");
        assertTrue(chainSelect(form).isVisible(), "Chain scope is narrowed by the chain");
        assertFalse(storeSelect(form).isVisible(), "Chain scope names the chain, not the stores inside it");
    }

    @Test
    void storeScope_showsAllThree() throws Exception {
        PromotionForm form = newForm(FormAction.CREATE, null);
        setScope(form, PromotionScopeType.STORE);

        assertTrue(brandSelect(form).isVisible(), "a store sits inside a chain inside a brand");
        assertTrue(chainSelect(form).isVisible(), "a store is only identified within its chain");
        assertTrue(storeSelect(form).isVisible(), "Store scope is narrowed by the stores themselves");
    }

    @Test
    void theScopeSelector_andTheFieldsItDrives_shareARow() throws Exception {
        PromotionForm form = newForm(FormAction.CREATE, null);
        setScope(form, PromotionScopeType.STORE);
        FormLayout target = targetSection(form);

        assertEquals(1, target.getColspan(scopeSelect(form)),
                "the selector has to leave room beside it for the fields it drives, or the two stack into "
                        + "separate rows and stop reading as one decision");
        assertEquals(1, target.getColspan(field(form, "scopeFieldsLayout")),
                "the dynamic fields have to sit beside the selector rather than under it");
    }

    @Test
    void theStoreField_takesSeveralValues() throws Exception {
        PromotionForm form = newForm(FormAction.CREATE, null);
        setScope(form, PromotionScopeType.STORE);
        MultiSelectComboBox<StoreDto> storeSelect = storeSelect(form);

        storeSelect.setItems(List.of(store(3, "Bintara"), store(4, "Sunter")));
        storeSelect.setValue(Set.of(store(3, "Bintara"), store(4, "Sunter")));

        assertEquals(2, storeSelect.getValue().size(),
                "the field is a MultiSelectComboBox, so several stores can be narrowed to at once; this "
                        + "guards against a plain Select being substituted for it");
    }

    @Test
    void noStore_isChosenByDefault() throws Exception {
        PromotionForm form = newForm(FormAction.CREATE, null);
        setScope(form, PromotionScopeType.STORE);
        applyStores(form, store(3, "Bintara"), store(4, "Sunter"));

        assertTrue(storeSelect(form).getValue() == null || storeSelect(form).getValue().isEmpty(),
                "pre-ticking a store would quietly narrow the promotion to a single store the operator never "
                        + "picked, unlike the single-valued levels above it which default to their first option");
    }

    @Test
    void theFirstBrand_isChosenOnceTheBrandsArrive() throws Exception {
        PromotionForm form = newForm(FormAction.CREATE, null);
        applyBrands(form, brand(7, "Kopi Harmoni"), brand(9, "Tea House"));

        assertEquals("Kopi Harmoni", brandSelect(form).getValue().getName(),
                "the brand field is filled from the fetched list, defaulting to its first entry so the section "
                        + "is never left asking for a brand it is able to name");
    }

    @Test
    void aChosenBrand_survivesTheBrandListReloading() throws Exception {
        PromotionForm form = newForm(FormAction.CREATE, null);
        applyBrands(form, brand(7, "Kopi Harmoni"), brand(9, "Tea House"));
        brandSelect(form).setValue(offeredOption(brandSelect(form), BrandDto::getName, "Tea House"));

        applyBrands(form, brand(7, "Kopi Harmoni"), brand(9, "Tea House"));

        assertEquals("Tea House", brandSelect(form).getValue().getName(),
                "a reload must not reset an operator who had already chosen a brand, or the selection they made "
                        + "would be silently undone behind them");
    }

    @Test
    void theFirstChain_isChosenOnceTheChainsArrive() throws Exception {
        PromotionForm form = newForm(FormAction.CREATE, null);
        applyBrands(form, brand(7, "Kopi Harmoni"));
        applyChains(form, chain(1, "Harmoni Coffee"), chain(2, "Kopi Nusantara"));

        assertEquals("Harmoni Coffee", chainSelect(form).getValue().getName(),
                "the store level is reached through the chain, so a brand with no chain chosen has nothing to "
                        + "offer underneath it");
    }

    @Test
    void changingTheBrand_clearsTheChainBelowIt() throws Exception {
        PromotionForm form = newForm(FormAction.CREATE, null);
        applyBrands(form, brand(7, "Kopi Harmoni"), brand(9, "Tea House"));
        applyChains(form, chain(1, "Harmoni Coffee"));

        pickBrand(form, "Tea House");

        assertNull(chainSelect(form).getValue(),
                "a chain left over from the previous brand would submit a brand and a chain that contradict "
                        + "each other");
    }

    @Test
    void theSelectedOrganization_reachesThePayload() throws Exception {
        PromotionForm form = newForm(FormAction.CREATE, null);
        setScope(form, PromotionScopeType.STORE);
        applyBrands(form, brand(7, "Kopi Harmoni"));
        applyChains(form, chain(1, "Harmoni Coffee"));
        applyStores(form, store(3, "Bintara"), store(4, "Sunter"));
        storeSelect(form).setValue(Set.of(
                offeredOption(storeSelect(form), StoreDto::getName, "Bintara"),
                offeredOption(storeSelect(form), StoreDto::getName, "Sunter")));
        select(form, "applyToSelect").setValue(PromotionTargetType.CATEGORY);
        selectedCategories(form).add(category(5));

        PromotionDto promotion = form.buildAggregate();

        assertEquals(PromotionScopeType.STORE, promotion.getScope(),
                "the scope is what tells the backend which organizational level the ids below belong to");
        List<PromotionTargetDto> targets = promotion.getTargets();
        assertEquals(1, targets.size(), "one category was selected, so exactly one target row is written");
        PromotionTargetDto target = targets.getFirst();
        assertEquals(7L, target.getBrandId(), "the brand the operator narrowed to");
        assertEquals(1L, target.getChainId(), "the chain the operator narrowed to");
        assertEquals(List.of(3L, 4L), target.getStoreIds().stream().sorted().toList(),
                "every selected store is submitted, not just the first");
    }

    @Test
    void aSavedAllStoresPromotion_restoresAsBrand() throws Exception {
        PromotionDto saved = promotion(PromotionScopeType.ALL_STORES);

        PromotionForm form = newForm(FormAction.EDIT, saved);

        assertEquals(PromotionScopeType.BRAND, scopeSelect(form).getValue(),
                "All Stores is no longer offered, so a promotion saved that way comes back as the default scope "
                        + "rather than as a selector showing a value that is not in its own list");
    }

    @Test
    void aSavedPromotion_putsItsOwnBrandBack() throws Exception {
        PromotionDto saved = promotion(PromotionScopeType.BRAND,
                PromotionTargetDto.builder()
                        .targetType(PromotionTargetType.CATEGORY)
                        .categoryId(5L)
                        .scope(PromotionScopeType.BRAND)
                        .brandId(9L)
                        .build());

        PromotionForm form = newForm(FormAction.EDIT, saved);
        applyBrands(form, brand(7, "Kopi Harmoni"), brand(9, "Tea House"));

        assertEquals("Tea House", brandSelect(form).getValue().getName(),
                "the brand is parked while the list is still being fetched and put back once it arrives, rather "
                        + "than being overwritten by the first-brand default");
    }

    @Test
    void aBrandThatNoLongerExists_fallsBackToTheFirst() throws Exception {
        PromotionDto saved = promotion(PromotionScopeType.BRAND,
                PromotionTargetDto.builder()
                        .targetType(PromotionTargetType.CATEGORY)
                        .scope(PromotionScopeType.BRAND)
                        .brandId(404L)
                        .build());

        PromotionForm form = newForm(FormAction.EDIT, saved);
        applyBrands(form, brand(7, "Kopi Harmoni"), brand(9, "Tea House"));

        assertEquals("Kopi Harmoni", brandSelect(form).getValue().getName(),
                "a brand id the backend no longer offers cannot be selected, so the field falls back rather than "
                        + "being left empty with nothing to narrow by");
    }

    // --- fixtures -------------------------------------------------------------------------------

    private static PromotionDto promotion(PromotionScopeType scope, PromotionTargetDto... targets) {
        PromotionDto dto = new PromotionDto();
        dto.setName("Lunch special");
        dto.setCode("LUNCH");
        dto.setScope(scope);
        dto.setTargets(List.of(targets));
        return dto;
    }

    private static BrandDto brand(Integer id, String name) {
        BrandDto brand = new BrandDto();
        brand.setId(id);
        brand.setName(name);
        return brand;
    }

    private static ChainDto chain(Integer id, String name) {
        ChainDto chain = new ChainDto();
        chain.setId(id);
        chain.setName(name);
        return chain;
    }

    private static StoreDto store(Integer id, String name) {
        StoreDto store = new StoreDto();
        store.setId(id);
        store.setName(name);
        return store;
    }

    private static CategoryDto category(Integer id) {
        CategoryDto category = new CategoryDto();
        category.setId(id);
        category.setName("Coffee");
        return category;
    }

    /**
     * Completes the brand fetch, standing in for the REST layer the form really uses.
     */
    private static void applyBrands(PromotionForm form, BrandDto... brands) throws Exception {
        invoke(form, "applyBrands", List.class, List.of(brands));
    }

    /** The target rows the form would submit right now. */
    @SuppressWarnings("unchecked")
    private static List<PromotionTargetDto> buildTargets(PromotionForm form) throws Exception {
        Method method = PromotionForm.class
                .getDeclaredMethod("buildTargets", PromotionTargetType.class);
        method.setAccessible(true);
        return (List<PromotionTargetDto>) method.invoke(form, applyToSelect(form).getValue());
    }

    private static void applyChains(PromotionForm form, ChainDto... chains) throws Exception {
        invoke(form, "applyChains", List.class, List.of(chains));
    }

    private static void applyStores(PromotionForm form, StoreDto... stores) throws Exception {
        invoke(form, "applyStores", List.class, List.of(stores));
    }

    @Test
    void aNewPromotion_offersOnlyCategoriesAndProducts() throws Exception {
        PromotionForm form = newForm(FormAction.CREATE, null);

        assertEquals(List.of(PromotionTargetType.CATEGORY, PromotionTargetType.PRODUCT),
                applyToSelect(form).getListDataView().getItems().toList(),
                "SKUs were never a real choice - the dialog picked products and expanded "
                        + "whatever SKUs they carried - so it should not be offered");
    }

    @Test
    void aPromotionSavedAgainstSkus_stillShowsThemAsItsTarget() throws Exception {
        PromotionDto saved = new PromotionDto();
        saved.setId(1L);
        saved.setPromotionType(PromotionType.PERCENTAGE);
        PromotionTargetDto skuTarget = new PromotionTargetDto();
        skuTarget.setTargetType(PromotionTargetType.SKU);
        skuTarget.setSkuId(88L);
        saved.setTargets(List.of(skuTarget));
        PromotionForm form = newForm(FormAction.EDIT, saved);

        assertTrue(applyToSelect(form).getListDataView().getItems()
                        .anyMatch(type -> PromotionTargetType.SKU == type),
                "the field has to be able to show the type this promotion is saved against; an "
                        + "option it does not hold leaves the operator looking at an empty or "
                        + "mismatched field, and saving would rewrite the targets as categories");
        assertEquals(PromotionTargetType.SKU, applyToSelect(form).getValue());
        assertEquals(List.of(88L), buildTargets(form).stream()
                        .map(PromotionTargetDto::getSkuId).toList(),
                "its SKU targets have to survive the round trip");
    }

    @Test
    void choosingStore_opensThePickerAndLeavesTheSectionAsItWas() throws Exception {
        DialoglessPromotionForm form = newDialoglessForm(FormAction.CREATE, null);
        setScope(form, PromotionScopeType.BRAND);

        chooseScopeFromClient(form, PromotionScopeType.STORE);

        assertTrue(form.storePickerOpened, "choosing Store is what should bring the picker up");
        assertFalse(chainSelect(form).isVisible(),
                "the section must not switch to the store layout before the operator has settled "
                        + "on some stores; a half-switched section is the state a cancelled picker "
                        + "would have left behind");
    }

    @Test
    void cancellingTheStorePicker_putsTheScopeBackAndLeavesTheNarrowingAlone() throws Exception {
        DialoglessPromotionForm form = newDialoglessForm(FormAction.CREATE, null);
        setScope(form, PromotionScopeType.CHAIN);
        applyBrands(form, brand(7, "Kopi Harmoni"));
        applyChains(form, chain(1, "Harmoni Coffee"));

        chooseScopeFromClient(form, PromotionScopeType.STORE);
        cancelStoreDialog(form);

        assertEquals(PromotionScopeType.CHAIN, scopeSelect(form).getValue());
        assertEquals("Harmoni Coffee", chainSelect(form).getValue().getName(),
                "a cancelled picker must not disturb the narrowing the promotion already had");
    }

    @Test
    void theWholeNarrowingReachesTheForm_whenTheOperatorSettlesOnStores() throws Exception {
        DialoglessPromotionForm form = newDialoglessForm(FormAction.CREATE, null);
        selectedCategories(form).add(category(5));
        chooseScopeFromClient(form, PromotionScopeType.STORE);
        confirmStoreDialog(form, brand(7, "Kopi Harmoni"), chain(1, "Harmoni Coffee"),
                List.of(store(3, "Bintara"), store(4, "Sunter")));

        assertEquals(PromotionScopeType.STORE, scopeSelect(form).getValue());
        assertEquals("Kopi Harmoni", brandSelect(form).getValue().getName(),
                "a store-scoped promotion is meaningless without the brand and chain above it, so "
                        + "the whole narrowing has to reach the form");
        assertEquals("Harmoni Coffee", chainSelect(form).getValue().getName());
        assertEquals(List.of(3L, 4L), buildTargets(form).getFirst().getStoreIds(),
                "the operator picked two specific stores, so both have to reach the payload");
    }

    @Test
    void aStoreScopedPromotion_namesItsStoresInThePreview() throws Exception {
        DialoglessPromotionForm form = newDialoglessForm(FormAction.CREATE, null);
        chooseScopeFromClient(form, PromotionScopeType.STORE);
        confirmStoreDialog(form, brand(7, "Kopi Harmoni"), chain(1, "Harmoni Coffee"),
                List.of(store(3, "Bintara"), store(4, "Sunter")));

        assertEquals("Bintara, Sunter", scopeNamesOf(form).get(PromotionPreview.STORE_KEY),
                "the preview has to show the same stores the form is holding, or the operator "
                        + "cannot tell what the promotion ended up limited to");
    }

    @Test
    void switchingScopeAwayFromStore_putsTheBrandListBackInReach() throws Exception {
        DialoglessPromotionForm form = newDialoglessForm(FormAction.CREATE, null);
        applyBrands(form, brand(7, "Kopi Harmoni"), brand(9, "Tea House"));
        chooseScopeFromClient(form, PromotionScopeType.STORE);
        confirmStoreDialog(form, brand(9, "Tea House"), chain(1, "Harmoni Coffee"),
                List.of(store(3, "Bintara")));

        setScope(form, PromotionScopeType.BRAND);

        assertEquals(2, brandSelect(form).getListDataView().getItems().count(),
                "adopting one brand must not reduce the field to that single brand, or the "
                        + "operator could not widen the promotion again");
    }

    @Test
    void changingTheBrand_asksTheBackendForTheChainsOfTheNewBrand() throws Exception {
        RecordingOrganizationService service = new RecordingOrganizationService();
        service.chainsByBrand.put(7, List.of(chain(1, "Harmoni Coffee")));
        service.chainsByBrand.put(9, List.of(chain(4, "Tea House Central")));
        PromotionForm form = newForm(FormAction.CREATE, null, service);
        applyBrands(form, brand(7, "Kopi Harmoni"), brand(9, "Tea House"));

        pickBrand(form, "Tea House");

        assertEquals(List.of(7, 9), service.requestedChainBrands,
                "the new brand's chains have to be asked for; clearing the field without "
                        + "refilling it would leave the chain and store levels unreachable for "
                        + "the rest of this editing session");
        assertNull(chainSelect(form).getValue(),
                "the chain of the brand left behind must not be kept");
    }

    @Test
    void changingTheChain_asksTheBackendForTheStoresOfTheNewChain() throws Exception {
        RecordingOrganizationService service = new RecordingOrganizationService();
        service.chainsByBrand.put(7, List.of(chain(1, "Harmoni Coffee"), chain(2, "Tea House")));
        PromotionForm form = newForm(FormAction.CREATE, null, service);
        setScope(form, PromotionScopeType.STORE);
        applyBrands(form, brand(7, "Kopi Harmoni"));
        applyChains(form, chain(1, "Harmoni Coffee"), chain(2, "Tea House"));

        pickChain(form, "Tea House");

        assertEquals(Integer.valueOf(2), service.lastStoreRequest,
                "the new chain's stores have to be asked for, or the store level stays empty "
                        + "once the old chain's stores are cleared");
        assertTrue(storeSelect(form).getValue().isEmpty(),
                "no store of the chain left behind may be kept");
    }

    /**
     * An organization service that answers from memory and records what it was asked
     * for, standing in for the backend.
     *
     * <p>Only the requests are asserted against. The form applies responses inside
     * {@code UiUtil.safeAccess}, which deliberately does nothing while no session is
     * attached, so filling the fields from a reply is not observable here; that half is
     * covered directly against {@code applyBrands} and {@code applyChains} above.
     *
     * <p>Subclassed rather than mocked because the project carries no mocking
     * library, and adding one for a single test would be a heavier change than the
     * cascade it is meant to prove.</p>
     */
    private static final class RecordingOrganizationService extends AsyncRestClientOrganizationService {

        private final Map<Integer, List<ChainDto>> chainsByBrand = new LinkedHashMap<>();
        private final List<Integer> requestedChainBrands = new ArrayList<>();
        private Integer lastStoreRequest;

        RecordingOrganizationService() {
            super(null, null);
        }

        @Override
        public void getAllBrandAsync(AsyncRestCallback<List<BrandDto>> callback,
                                     AsyncRestCallback<Throwable> errorCallback) {
            callback.operationFinished(List.of(brand(7, "Kopi Harmoni"), brand(9, "Tea House")));
        }

        @Override
        public void getAllChainByBrandIdAsync(AsyncRestCallback<List<ChainDto>> callback,
                                              AsyncRestCallback<Throwable> errorCallback, Integer brandId) {
            requestedChainBrands.add(brandId);
            callback.operationFinished(chainsByBrand.getOrDefault(brandId, List.of()));
        }

        @Override
        public void getAllStoreAsync(AsyncRestCallback<Map<String, Object>> callback,
                                     AsyncRestCallback<Throwable> errorCallback, Integer chainId,
                                     int page, int size, String search) {
            lastStoreRequest = chainId;
            callback.operationFinished(Map.of("data", List.of(store(3, "Bintara"), store(4, "Sunter"))));
        }
    }

    /**
     * Changes the scope the way picking it in the UI would.
     *
     * <p>The field's own listener is guarded by {@code isFromClient()}, so a
     * programmatic {@code setValue} would leave the visibility alone and the test would
     * pass against a section the operator would never actually see. The handler is
     * therefore called directly.</p>
     */
    private static void setScope(PromotionForm form, PromotionScopeType scope) throws Exception {
        scopeSelect(form).setValue(scope);
        invoke(form, "updateScopeSelect", PromotionScopeType.class, scope);
    }

    /**
     * Picks a brand the way picking it in the UI would.
     *
     * <p>The field's listener is guarded by {@code isFromClient()}, so a programmatic
     * {@code setValue} would leave the chain below it untouched and the test would pass
     * against a section the operator would never see. The handler is called directly.</p>
     */
    /**
     * Chooses a scope the way choosing it in the dropdown would, by driving the form's
     * own handler rather than the guarded listener.
     */
    private static void chooseScopeFromClient(PromotionForm form, PromotionScopeType scope) throws Exception {
        PromotionScopeType previous = scopeSelect(form).getValue();
        scopeSelect(form).setValue(scope);
        invoke(form, "onScopeChosen", PromotionScopeType.class, PromotionScopeType.class, scope, previous);
    }

    /**
     * Confirms a store picker holding the given choice, by running the form's own
     * handler with it.
     */
    private static void confirmStoreDialog(PromotionForm form, BrandDto brand, ChainDto chain,
                                          List<StoreDto> stores) throws Exception {
        invoke(form, "onStoresChosen", StoreSelectionDialog.Selection.class,
                new StoreSelectionDialog.Selection(brand, chain, stores));
    }

    /** Cancels a store picker, the way closing it does. */
    private static void cancelStoreDialog(PromotionForm form) throws Exception {
        invoke(form, "revertStoreScope");
    }

    /** The organization names the form would hand the preview. */
    @SuppressWarnings("unchecked")
    private static Map<String, String> scopeNamesOf(PromotionForm form) throws Exception {
        return (Map<String, String>) invoke(form, "buildTargetNames");
    }

    private static void pickBrand(PromotionForm form, String name) throws Exception {
        brandSelect(form).setValue(offeredOption(brandSelect(form), BrandDto::getName, name));
        invoke(form, "onBrandChanged");
    }

    /**
     * Picks a chain the way picking it in the UI would, for the same reason
     * {@link #pickBrand} calls the handler itself.
     */
    private static void pickChain(PromotionForm form, String name) throws Exception {
        chainSelect(form).setValue(offeredOption(chainSelect(form), ChainDto::getName, name));
        invoke(form, "onChainChanged");
    }

    /**
     * The option a field is actually offering under the name given.
     *
     * <p>Options are picked out of the field rather than rebuilt. A Select only holds a
     * value that is one of its own options, so handing it an equal-looking copy built
     * here is silently dropped and the test would go on to assert against the option
     * that was already there.</p>
     */
    private static <T> T offeredOption(Select<T> field, Function<T, String> nameOf, String name) {
        return offeredOption(field.getListDataView().getItems().toList(), nameOf, name);
    }

    private static <T> T offeredOption(MultiSelectComboBox<T> field, Function<T, String> nameOf, String name) {
        return offeredOption(field.getListDataView().getItems().toList(), nameOf, name);
    }

    private static <T> T offeredOption(List<T> options, Function<T, String> nameOf, String name) {
        return options.stream()
                .filter(option -> nameOf.apply(option).equals(name))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no option called " + name + " is on offer"));
    }

    // --- reflection helpers ---------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private static Select<PromotionScopeType> scopeSelect(PromotionForm form) throws Exception {
        return (Select<PromotionScopeType>) field(form, "scopeSelect");
    }

    @SuppressWarnings("unchecked")
    private static Select<BrandDto> brandSelect(PromotionForm form) throws Exception {
        return (Select<BrandDto>) field(form, "brandSelect");
    }

    @SuppressWarnings("unchecked")
    private static Select<ChainDto> chainSelect(PromotionForm form) throws Exception {
        return (Select<ChainDto>) field(form, "chainSelect");
    }

    @SuppressWarnings("unchecked")
    private static Select<PromotionTargetType> applyToSelect(PromotionForm form) throws Exception {
        return (Select<PromotionTargetType>) field(form, "applyToSelect");
    }

    @SuppressWarnings("unchecked")
    private static MultiSelectComboBox<StoreDto> storeSelect(PromotionForm form) throws Exception {
        return (MultiSelectComboBox<StoreDto>) field(form, "storeSelect");
    }

    @SuppressWarnings("unchecked")
    private static <T> Select<T> select(PromotionForm form, String name) throws Exception {
        return (Select<T>) field(form, name);
    }

    @SuppressWarnings("unchecked")
    private static List<CategoryDto> selectedCategories(PromotionForm form) throws Exception {
        Field declared = PromotionForm.class.getDeclaredField("selectedCategories");
        declared.setAccessible(true);
        return (List<CategoryDto>) declared.get(form);
    }

    /**
     * The form's target section, found by the fields it holds rather than by its title,
     * so the lookup cannot drift when the labels are translated.
     */
    private static FormLayout targetSection(PromotionForm form) throws Exception {
        Component scopeSelect = field(form, "scopeSelect");
        List<FormLayout> layouts = new ArrayList<>();
        collectLayouts(form, layouts);
        return layouts.stream()
                .filter(layout -> layout.getChildren().anyMatch(scopeSelect::equals))
                .findFirst()
                .orElseThrow(() -> new AssertionError("no section holds the scope selector"));
    }

    private static void collectLayouts(Component node, List<FormLayout> found) {
        if (node instanceof FormLayout layout) {
            found.add(layout);
        }
        node.getChildren().forEach(child -> collectLayouts(child, found));
    }

    /**
     * Reads a field off the very form being inspected. Looking it up on a second form would
     * compare a component from one instance against a layout from another, and find neither.
     */
    private static Component field(PromotionForm form, String name) throws Exception {
        Field declared = PromotionForm.class.getDeclaredField(name);
        declared.setAccessible(true);
        return (Component) declared.get(form);
    }

    private static PromotionForm newForm(FormAction formAction, PromotionDto promotion) throws Exception {
        return newForm(formAction, promotion, null);
    }

    /**
     * A form whose store picker is stood in for.
     *
     * <p>Opening a dialog needs a live UI, which a unit test does not have. Replacing it
     * keeps the part worth testing - that choosing {@code Store} defers the scope, and
     * that confirming or cancelling settles it - without standing up Vaadin.</p>
     */
    private static final class DialoglessPromotionForm extends PromotionForm {

        private boolean storePickerOpened;

        DialoglessPromotionForm(FormAction formAction, PromotionDto promotion) {
            super(null, null, null, null,
                    PromotionEditorContext.builder().formAction(formAction).promotionDto(promotion).build());
        }

        @Override
        void openStoreSelectionDialog() {
            storePickerOpened = true;
        }
    }

    private static DialoglessPromotionForm newDialoglessForm(FormAction action, PromotionDto promotion)
            throws Exception {
        PromotionForm form = new DialoglessPromotionForm(action, promotion);
        invoke(form, "configureFields");
        invoke(form, "configureSpecialPriceGrid");
        invoke(form, "addValidation");
        invoke(form, "addFields");
        invoke(form, "restoreBean");
        return (DialoglessPromotionForm) form;
    }

    private static PromotionForm newForm(FormAction formAction, PromotionDto promotion,
                                         AsyncRestClientOrganizationService service) throws Exception {
        PromotionForm form = new PromotionForm(null, null, service, null,
                PromotionEditorContext.builder().formAction(formAction).promotionDto(promotion).build());
        invoke(form, "configureFields");
        invoke(form, "configureSpecialPriceGrid");
        invoke(form, "addValidation");
        invoke(form, "addFields");
        invoke(form, "restoreBean");
        return form;
    }

    /** Calls a no-argument method, returning whatever it produced. */
    private static Object invoke(PromotionForm form, String name) throws Exception {
        Method method = PromotionForm.class.getDeclaredMethod(name);
        method.setAccessible(true);
        return method.invoke(form);
    }

    private static void invoke(PromotionForm form, String name, Class<?> parameter, Object argument)
            throws Exception {
        Method method = PromotionForm.class.getDeclaredMethod(name, parameter);
        method.setAccessible(true);
        method.invoke(form, argument);
    }

    private static void invoke(PromotionForm form, String name, Class<?> first, Class<?> second,
                               Object firstArgument, Object secondArgument) throws Exception {
        Method method = PromotionForm.class.getDeclaredMethod(name, first, second);
        method.setAccessible(true);
        method.invoke(form, firstArgument, secondArgument);
    }


}
