package com.harmoni.menu.dashboard.layout.menu.promotion;

import com.harmoni.menu.dashboard.dto.PromotionDto;
import com.harmoni.menu.dashboard.layout.enums.PromotionScopeType;
import com.vaadin.flow.component.html.Span;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Guards the preview's "Scope To" row, which names the organization the promotion is
 * limited to.
 *
 * <p>The form asks the operator to narrow a promotion to a brand, chain or store, and the
 * preview had no row for any of it, so the narrowing was only ever visible in the form
 * itself. The row names the level as well as the organization, because a bare name like
 * "Kopi Harmoni" under a chain scope reads as a brand.</p>
 */
class PromotionPreviewScopeRowTest {

    @Test
    void aBrandScope_namesBothTheLevelAndTheBrand() {
        PromotionDto promotion = new PromotionDto();
        promotion.setScope(PromotionScopeType.BRAND);

        String text = scopeText(promotion, Map.of(PromotionPreview.BRAND_KEY, "Kopi Harmoni"));

        assertEquals("Brand: Kopi Harmoni", text,
                "the level has to be named too, or the operator cannot tell a brand from a chain");
    }

    @Test
    void aStoreScope_namesEveryStoreItCovers() {
        PromotionDto promotion = new PromotionDto();
        promotion.setScope(PromotionScopeType.STORE);

        String text = scopeText(promotion, Map.of(PromotionPreview.STORE_KEY, "Bintara, Sunter"));

        assertEquals("Store: Bintara, Sunter", text,
                "the operator picked specific stores, so the preview has to name all of them");
    }

    @Test
    void aScopeWithNoOrganizationYet_showsTheLevelRatherThanNothing() {
        PromotionDto promotion = new PromotionDto();
        promotion.setScope(PromotionScopeType.CHAIN);

        String text = scopeText(promotion, Map.of());

        assertEquals("Chain", text,
                "the names arrive a moment after the scope, and a blank row would read as "
                        + "though the operator had chosen nothing");
    }

    @Test
    void theRowIsWrittenByThePreview_underTheScopeKey() throws Exception {
        PromotionPreview preview = new PromotionPreview();
        PromotionDto promotion = new PromotionDto();
        promotion.setScope(PromotionScopeType.BRAND);
        promotion.setName("Lunch");

        preview.updatePreview(promotion, Map.of(PromotionPreview.BRAND_KEY, "Kopi Harmoni"));

        // Guards the wiring, not just the text: the row is only useful if the preview
        // actually puts it in the panel.
        assertEquals("Brand: Kopi Harmoni", rowValue(preview, "scope").getText());
    }

    private static String scopeText(PromotionDto promotion, Map<String, String> names) {
        return new PromotionPreview().buildScopeText(promotion, names);
    }

    private static Span rowValue(PromotionPreview preview, String key) throws Exception {
        Field field = PromotionPreview.class.getDeclaredField("values");
        field.setAccessible(true);
        @SuppressWarnings("unchecked")
        Map<String, Span> values = (Map<String, Span>) field.get(preview);
        return values.get(key);
    }
}
