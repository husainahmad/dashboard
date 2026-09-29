package com.harmoni.menu.dashboard.layout.menu.promotion;

import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.ItemLabelGenerator;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.ToIntFunction;
import java.util.function.ToLongFunction;
import java.util.stream.Collectors;

/**
 * Picks the stores a promotion is limited to, narrowing through brand and chain first.
 *
 * <p>Reached by choosing {@code Store} in the scope field. The three levels live in here
 * rather than in the form because a store-scoped promotion needs all three at once, and
 * the form only has room for the levels its own scope reaches.</p>
 *
 * <p>Seeded from the form's current brand and chain, so stepping down to stores does not
 * make the operator re-pick the brand the promotion was already narrowed to.</p>
 */
public class StoreSelectionDialog extends Dialog {

    private static final int STORE_PAGE_SIZE = 1000;

    /** What the operator settled on, handed back to the form. */
    public record Selection(BrandDto brand, ChainDto chain, List<StoreDto> stores) {
    }

    private final AsyncRestClientOrganizationService organizationService;
    private final transient Consumer<Selection> onConfirm;
    private final transient List<StoreDto> preSelectedStores;
    private final Long preferredBrandId;
    private final Long preferredChainId;

    private final Select<BrandDto> brandSelect = new Select<>();
    private final Select<ChainDto> chainSelect = new Select<>();
    private final MultiSelectComboBox<StoreDto> storeSelect = new MultiSelectComboBox<>();

    private UI ui;
    private boolean initialLoadStarted;

    /**
     * @param organizationService the service brands, chains and stores are read from
     * @param preferredBrand      the brand the form is already narrowed to, may be null
     * @param preferredChain      the chain the form is already narrowed to, may be null
     * @param preSelectedStores   the stores already chosen, may be empty
     * @param onConfirm           called with the settled brand, chain and stores
     */
    public StoreSelectionDialog(AsyncRestClientOrganizationService organizationService,
                                BrandDto preferredBrand,
                                ChainDto preferredChain,
                                List<StoreDto> preSelectedStores,
                                Consumer<Selection> onConfirm) {
        this.organizationService = organizationService;
        this.onConfirm = onConfirm;
        this.preSelectedStores = preSelectedStores == null ? List.of() : List.copyOf(preSelectedStores);
        this.preferredBrandId = brandId(preferredBrand);
        this.preferredChainId = chainId(preferredChain);

        setHeaderTitle(Messages.get("dialog.storeSelection.title"));
        setWidth("560px");
        setMaxWidth("90vw");
        setDraggable(true);
        setModal(true);
        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);

        buildContent();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        this.ui = attachEvent.getUI();
        if (initialLoadStarted) {
            return;
        }
        initialLoadStarted = true;
        loadBrands();
    }

    private void buildContent() {
        configure(brandSelect, Messages.Keys.LABEL_BRAND, Messages.Keys.PLACEHOLDER_SELECT_BRAND, BrandDto::getName);
        brandSelect.addValueChangeListener(change -> {
            if (change.isFromClient()) {
                onBrandChanged();
            }
        });

        configure(chainSelect, Messages.Keys.LABEL_CHAIN, Messages.Keys.PLACEHOLDER_SELECT_CHAIN, ChainDto::getName);
        chainSelect.addValueChangeListener(change -> {
            if (change.isFromClient()) {
                onChainChanged();
            }
        });

        configure(storeSelect, Messages.Keys.LABEL_STORE);

        Button cancel = new Button(Messages.get(Messages.Keys.ACTION_CANCEL), event -> close());
        Button confirm = new Button(Messages.get("action.confirm"), event -> confirm());
        confirm.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(new VerticalLayout(brandSelect, chainSelect, storeSelect));
        getFooter().add(new HorizontalLayout(cancel, confirm));
    }

    /**
     * Applies the settings the three fields share.
     *
     * <p>The name generator is what keeps an option readable. Left to itself a Select
     * falls back to the option's {@code toString()}, which for these DTOs is the whole
     * record - id, name, timestamps, the lot - so the operator would be picking between
     * lines of field assignments rather than between brands.</p>
     *
     * @param field          the field being configured
     * @param labelKey       message key for its label
     * @param placeholderKey message key shown while nothing is chosen
     * @param nameGenerator  reads the display name off an option
     * @param <T>            the option type
     */
    private <T> void configure(Select<T> field, String labelKey, String placeholderKey,
                              ItemLabelGenerator<T> nameGenerator) {
        field.setLabel(Messages.get(labelKey));
        field.setEmptySelectionCaption(Messages.get(placeholderKey));
        field.setItemLabelGenerator(nameGenerator);
        field.setWidthFull();
    }

    /**
     * The same settings for the multi-select, which is not a {@link Select}.
     *
     * <p>No empty-selection caption: a multi-select shows its placeholder text while
     * empty on its own, so there is nothing to override here.</p>
     */
    private void configure(MultiSelectComboBox<StoreDto> field, String labelKey) {
        field.setLabel(Messages.get(labelKey));
        field.setItemLabelGenerator(StoreDto::getName);
        field.setWidthFull();
    }

    /**
     * Hands the settled choice back, provided the levels above it were chosen.
     *
     * <p>Confirming without a brand or chain would be a store list that belongs to
     * nothing, so the button leaves the dialog open instead.</p>
     */
    private void confirm() {
        BrandDto brand = brandSelect.getValue();
        ChainDto chain = chainSelect.getValue();
        if (brand == null || chain == null) {
            return;
        }
        onConfirm.accept(new Selection(brand, chain, new ArrayList<>(storeSelect.getValue())));
        close();
    }

    /** Drops the chain and stores of the brand left behind, then refetches its chains. */
    private void onBrandChanged() {
        chainSelect.clear();
        storeSelect.clear();
        loadChains();
    }

    /** Drops the stores of the chain left behind, then refetches the new chain's. */
    private void onChainChanged() {
        storeSelect.clear();
        loadStores();
    }

    private void loadBrands() {
        organizationService.getAllBrandAsync(
                brands -> access(() -> applyBrands(orEmpty(brands))),
                error -> access(() -> UiUtil.errorWithRetry(
                        Messages.get("notification.brand.loadFailed"), this::loadBrands)));
    }

    private void applyBrands(List<BrandDto> brands) {
        brandSelect.setItems(brands);
        BrandDto preferred = pickById(brands, preferredBrandId, StoreSelectionDialog::brandId);
        brandSelect.setValue(preferred != null ? preferred : firstOrNull(brands));
        loadChains();
    }

    private void loadChains() {
        BrandDto brand = brandSelect.getValue();
        if (brand == null || brand.getId() == null) {
            return;
        }
        organizationService.getAllChainByBrandIdAsync(
                chains -> access(() -> applyChains(orEmpty(chains))),
                error -> access(() -> UiUtil.errorWithRetry(
                        Messages.get(Messages.Keys.NOTIFICATION_CHAIN_LOAD_FAILED), this::loadChains)),
                brand.getId());
    }

    private void applyChains(List<ChainDto> chains) {
        chainSelect.setItems(chains);
        // These chains were fetched for the brand now in hand, so finding the preferred id
        // among them is itself the check that it belongs to this brand. An earlier version
        // also compared the chain id against the brand id, which compares two unrelated
        // numbers: it was almost never equal, so the form's chain was quietly dropped
        // every time the picker was opened and reopened on an existing promotion.
        ChainDto preferred = pickById(chains, preferredChainId, StoreSelectionDialog::chainId);
        chainSelect.setValue(preferred != null ? preferred : firstOrNull(chains));
        loadStores();
    }

    private void loadStores() {
        ChainDto chain = chainSelect.getValue();
        if (chain == null || chain.getId() == null) {
            return;
        }
        organizationService.getAllStoreAsync(
                page -> access(() -> applyStores(extractStores(page))),
                error -> access(() -> UiUtil.errorWithRetry(
                        Messages.get(Messages.Keys.NOTIFICATION_STORE_LOAD_FAILED), this::loadStores)),
                // Page 1 and an empty search, the same shape the form asks with. The
                // endpoint's URI is "...&page=%d&size=%d&search=%s", so a null search
                // formats into the literal text "null" and a page of 0 asks for a page
                // that is not there: either one returns nothing at all, and the operator
                // is left staring at an empty list with no error to explain it.
                chain.getId(), 1, STORE_PAGE_SIZE, "");
    }

    private void applyStores(List<StoreDto> stores) {
        storeSelect.setItems(stores);
        // Linked, not a plain Set: the stores keep the order the chain listed them in,
        // so the ones already chosen do not shuffle each time the list is refilled.
        storeSelect.setValue(stores.stream()
                .filter(store -> storeId(store) != null)
                .filter(store -> containsId(preSelectedStores, StoreDto::getId, storeId(store)))
                .collect(Collectors.toCollection(LinkedHashSet::new)));
    }

    /**
     * Runs a reply on the UI thread, and only while the dialog is still up.
     *
     * <p>A request can land after the operator has closed the dialog - a retry after an
     * error, or a slow chain - and filling fields nobody can see would leave the store
     * list holding choices that were never made.</p>
     */
    private void access(Runnable action) {
        if (ui == null) {
            return;
        }
        UiUtil.safeAccess(ui, () -> {
            if (isOpened()) {
                action.run();
            }
        });
    }

    /**
     * Reads the store list out of the endpoint's page envelope.
     *
     * <p>The store endpoint answers with a page rather than a plain list, so the rows sit
     * under a {@code data} key as untyped maps until they are converted.</p>
     */
    private static List<StoreDto> extractStores(Map<String, Object> page) {
        if (page == null || !(page.get("data") instanceof List<?> rows)) {
            return new ArrayList<>();
        }
        List<StoreDto> stores = new ArrayList<>();
        for (Object row : rows) {
            if (ObjectUtil.convertValueToObject(row, StoreDto.class) instanceof StoreDto store) {
                stores.add(store);
            }
        }
        return stores;
    }

    private static <T> List<T> orEmpty(List<T> items) {
        return items == null ? List.of() : items;
    }

    private static <T> T pickById(List<T> items, Long id, Function<T, Long> idOf) {
        if (id == null) {
            return null;
        }
        return items.stream().filter(item -> id.equals(idOf.apply(item))).findFirst().orElse(null);
    }

    /**
     * Whether any of the already-chosen stores carries this id.
     *
     * <p>Every store passed in has been narrowed to one with an id by
     * {@link #applyStores(List)}, so the lookup is safe to read as a primitive
     * {@code long} through {@link ToLongFunction}. An unsaved store reporting a
     * {@code null} id is filtered out before it reaches here; without that filter a
     * primitive read would throw on those rows rather than skipping them.</p>
     *
     * @param stores the stores to search, each already known to carry an id
     * @param id     the id to look for, may be null
     * @return true when one of the stores matches
     */
    private static boolean containsId(List<StoreDto> stores, ToLongFunction<StoreDto> idOf, Long id) {
        return id != null && stores.stream().anyMatch(store -> id.equals(idOf.applyAsLong(store)));
    }

    private static Long brandId(BrandDto brand) {
        return brand == null || brand.getId() == null ? null : widen(brand, BrandDto::getId);
    }

    private static Long chainId(ChainDto chain) {
        return chain == null || chain.getId() == null ? null : widen(chain, ChainDto::getId);
    }

    private static Long storeId(StoreDto store) {
        return store == null || store.getId() == null ? null : widen(store, StoreDto::getId);
    }

    /**
     * Widens a row's id to the {@code Long} the seed and selection lookups compare against.
     *
     * <p>The DTOs type their id as {@code Integer} because that is what the API sends,
     * while the ids held onto by the form are {@code Long}. Reading through
     * {@link ToIntFunction} keeps the value primitive all the way out, so the id is
     * unboxed once here instead of being boxed into an {@code Integer} only to be
     * widened again on the next line.</p>
     *
     * <p>Callers must confirm the id is present before calling. A row that has not been
     * saved has a {@code null} id, and unboxing that here would throw where returning
     * {@code null} is what the callers check for.</p>
     *
     * @param item  the row to read, may be null
     * @param getId reads the id off the row
     * @param <T>   the row type
     * @return the id as a {@code Long}, or null when the row is null
     */
    private static <T> Long widen(T item, ToIntFunction<T> getId) {
        if (item == null) {
            return null;
        }
        return (long) getId.applyAsInt(item);
    }

    private static <T> T firstOrNull(List<T> items) {
        return items.isEmpty() ? null : items.getFirst();
    }
}
