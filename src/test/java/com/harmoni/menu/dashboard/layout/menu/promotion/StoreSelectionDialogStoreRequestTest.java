package com.harmoni.menu.dashboard.layout.menu.promotion;

import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guards the request the store picker makes for stores.
 *
 * <p>The endpoint's URI is {@code ...&page=%d&size=%d&search=%s}, so every argument here
 * is interpolated straight into the query string. Asking for page 0, or handing over a
 * null search, asks the backend for a page that is not there and a search for the literal
 * text "null" - and it answers with an empty list rather than an error, leaving the
 * operator looking at an empty field with nothing to explain why.</p>
 *
 * <p>Asserts on the request rather than the list that comes back, because applying a reply
 * needs a live UI: {@code UiUtil.safeAccess} deliberately does nothing without a session,
 * so the options can only be filled - and therefore only be checked - in a browser.</p>
 */
class StoreSelectionDialogStoreRequestTest {

    private static final int FIRST_PAGE = 1;

    @Test
    void itAsksForTheFirstPageAndAnEmptySearch() throws Exception {
        RecordingService service = new RecordingService();

        chooseChain(newDialog(service, null));

        assertEquals(FIRST_PAGE, service.page,
                "page 0 asks for a page that is not there, and the endpoint answers with an "
                        + "empty list rather than complaining");
        assertEquals("", service.search,
                "a null search is interpolated into the URI as the literal text \"null\", which "
                        + "matches no store");
    }

    @Test
    void itAsksForTheChainThePromotionWasAlreadyNarrowedTo() throws Exception {
        RecordingService service = new RecordingService();

        chooseChain(newDialog(service, chain(2, "Tea House")));

        assertEquals(Integer.valueOf(2), service.chainId,
                "the form hands the picker the chain the promotion already had, and the stores "
                        + "asked for have to be that chain's rather than the first one listed");
    }

    @Test
    void withoutAPreferredChain_itFallsBackToTheFirstOne() throws Exception {
        RecordingService service = new RecordingService();

        chooseChain(newDialog(service, null));

        assertEquals(Integer.valueOf(1), service.chainId,
                "with nothing preferred the first chain is chosen, as everywhere else in the "
                        + "section, so the store level is never left unreachable");
    }

    /**
     * Runs the picker's own chain handling, which is what triggers the store request.
     *
     * <p>Reached by reflection because there is no public way in, and because the only
     * way to pick a chain is the very act of opening a dialog.</p>
     */
    private static void chooseChain(StoreSelectionDialog dialog) throws Exception {
        // Brands first, as they arrive in the dialog: the chain carry-over is guarded on
        // the preferred chain belonging to the brand in hand, so a chain chosen before any
        // brand is set would be discarded and the test would prove nothing.
        apply(dialog, "applyBrands", List.class, List.of(brand()));
        apply(dialog, "applyChains", List.class, List.of(chain(1, "Harmoni Coffee"), chain(2, "Tea House")));
    }

    private static void apply(StoreSelectionDialog dialog, String name, Class<?> parameter, Object argument)
            throws Exception {
        Method method = StoreSelectionDialog.class.getDeclaredMethod(name, parameter);
        method.setAccessible(true);
        method.invoke(dialog, argument);
    }

    private static StoreSelectionDialog newDialog(RecordingService service, ChainDto preferredChain) {
        return new StoreSelectionDialog(service, brand(), preferredChain, List.of(), selection -> {
        });
    }

    private static BrandDto brand() {
        BrandDto brand = new BrandDto();
        brand.setId(7);
        brand.setName("Kopi Harmoni");
        return brand;
    }

    private static ChainDto chain(Integer id, String name) {
        ChainDto chain = new ChainDto();
        chain.setId(id);
        chain.setName(name);
        return chain;
    }

    /** Records the store request, answering with nothing so the list stays out of it. */
    private static final class RecordingService extends AsyncRestClientOrganizationService {

        private Integer chainId;
        private int page;
        private String search;

        RecordingService() {
            super(null, null);
        }

        @Override
        public void getAllChainByBrandIdAsync(
                com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientBase.AsyncRestCallback<List<ChainDto>>
                        callback,
                com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientBase.AsyncRestCallback<Throwable>
                        errorCallback,
                Integer brandId) {
            // Left unanswered on purpose: the chains arrive through applyChains, which is
            // what the test drives directly. Answering here would refill the field behind
            // the assertions.
        }

        @Override
        public void getAllStoreAsync(com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientBase
                .AsyncRestCallback<Map<String, Object>> callback,
                                     com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientBase
                                             .AsyncRestCallback<Throwable> errorCallback,
                                     Integer chainId, int page, int size, String search) {
            this.chainId = chainId;
            this.page = page;
            this.search = search;
        }
    }
}
