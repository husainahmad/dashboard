package com.harmoni.menu.dashboard.layout.menu.product;

import com.harmoni.menu.dashboard.dto.SkuDto;
import com.harmoni.menu.dashboard.dto.SkuTierPriceDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.vaadin.flow.component.UI;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the seam the product preview is fed through.
 *
 * <p>The preview reads {@link SkuSection#skuItems()}, so two things have to hold: the
 * list it gets back is the live one, and the form is told when it changes. The second
 * matters because a row edited inside the grid mutates a {@link SkuTreeItem} in place,
 * with no event the form could otherwise see.</p>
 */
class SkuSectionLiveModelTest {

    @Test
    void itSeedsAPriceForEveryTier() {
        SkuSection section = new SkuSection(tiers(), new RecordingDelegate());

        section.addSku(null);

        // addSku fills every tier so the grid's cells are never blank, which is also
        // why the preview's matrix has a cell to show for each one.
        assertEquals(List.of(1, 2), List.copyOf(section.skuItems().get(0).getTierPrices().keySet()));
    }

    @Test
    void itSeedsThePricesOfAnExistingSku() {
        SkuSection section = new SkuSection(tiers(), new RecordingDelegate());

        section.addSku(sku("Large", 25_000.0));

        assertEquals(25_000.0, section.skuItems().get(0).getTierPrices().get(1));
    }

    @Test
    void itTellsTheFormWhenARowIsAdded() {
        RecordingDelegate delegate = new RecordingDelegate();
        SkuSection section = new SkuSection(tiers(), delegate);

        section.addSku(null);

        assertEquals(1, delegate.changes,
                "a new row changes what the preview shows, so it has to be told");
    }

    @Test
    void thePreviewSeesAnEditMadeAfterTheSectionWasBuilt() {
        SkuSection section = new SkuSection(tiers(), new RecordingDelegate());
        section.addSku(sku("Large", 25_000.0));
        List<SkuTreeItem> first = section.skuItems();

        // The price a NumberField inside the grid would write on a value change.
        first.get(0).getTierPrices().put(1, 27_500.0);

        assertEquals(27_500.0, section.skuItems().get(0).getTierPrices().get(1),
                "the preview re-reads the list on every repaint, so an in-progress edit has "
                        + "to still be in it");
    }

    @Test
    void theLiveListCannotBeAlteredByAReader() {
        SkuSection section = new SkuSection(tiers(), new RecordingDelegate());
        section.addSku(sku("Large", 25_000.0));

        List<SkuTreeItem> items = section.skuItems();

        assertThrows(UnsupportedOperationException.class, () -> items.add(null),
                "a read-only consumer must not be able to change what the form will save");
    }

    @Test
    void itRefusesToDeleteTheLastVariant() throws Exception {
        RecordingDelegate delegate = new RecordingDelegate();
        SkuSection section = new SkuSection(tiers(), delegate);
        section.addSku(null);

        delete(section, section.skuItems().get(0));

        assertEquals(1, section.skuItems().size(),
                "a product must keep at least one variant");
        assertEquals(1, delegate.errors,
                "and the operator has to be told why the row did not go");
    }

    @Test
    void itDeletesAnyVariantOnceThereIsMoreThanOne() throws Exception {
        RecordingDelegate delegate = new RecordingDelegate();
        SkuSection section = new SkuSection(tiers(), delegate);
        section.addSku(sku("Large", 25_000.0));
        section.addSku(sku("Small", 20_000.0));
        int before = delegate.changes;

        delete(section, section.skuItems().get(0));

        assertEquals(1, section.skuItems().size());
        assertEquals("Small", section.skuItems().get(0).getSkuName());
        assertEquals(before + 1, delegate.changes,
                "the surviving matrix changed, so the preview has to repaint");
    }

    /** Reached by reflection: the only other route in is a rendered delete button. */
    private static void delete(SkuSection section, SkuTreeItem item) throws Exception {
        java.lang.reflect.Method removeSku =
                SkuSection.class.getDeclaredMethod("removeSku", SkuTreeItem.class);
        removeSku.setAccessible(true);
        removeSku.invoke(section, item);
    }

    // ---------------------------------------------------------------- helpers

    private static List<TierDto> tiers() {
        return List.of(tier(1, "Retail"), tier(2, "Whole"));
    }

    private static TierDto tier(Integer id, String name) {
        TierDto tier = new TierDto();
        tier.setId(id);
        tier.setName(name);
        return tier;
    }

    private static SkuDto sku(String name, Double retailPrice) {
        SkuDto sku = new SkuDto();
        sku.setName(name);
        SkuTierPriceDto price = new SkuTierPriceDto();
        price.setTierId(1);
        price.setPrice(retailPrice);
        sku.setSkuTierPriceDtos(List.of(price));
        return sku;
    }

    /** Counts the repaints the form has been asked for. */
    private static final class RecordingDelegate implements ProductFormDelegate {

        private int changes;

        private int errors;

        @Override
        public UI getUi() {
            return null;
        }

        @Override
        public void showNotification(String message) {
        }

        @Override
        public void showErrorDialog(String message) {
            errors++;
        }

        @Override
        public Integer getProductId() {
            return null;
        }

        @Override
        public void onContentChanged() {
            changes++;
        }
    }
}
