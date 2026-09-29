package com.harmoni.menu.dashboard.layout.menu.promotion;

import com.harmoni.menu.dashboard.dto.PromotionDto;
import com.harmoni.menu.dashboard.dto.PromotionTargetDto;
import com.harmoni.menu.dashboard.layout.enums.PromotionTargetType;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the preview's "Target" row.
 *
 * <p>The row names the targets a promotion applies to. It falls back to the id when no
 * name is known, which is what a target restored from a saved promotion used to show:
 * opening a promotion to edit it printed "Category 12" where the same promotion, chosen
 * through the pickers, showed a name. These tests pin the naming once it is available,
 * and pin the fallback so it is still a deliberate choice rather than a symptom.</p>
 */
class PromotionPreviewTargetRowTest {

    @Test
    void aCategoryTargetIsNamedByItsCategoryName() {
        PromotionDto promotion = new PromotionDto();
        promotion.setTargets(List.of(target(PromotionTargetType.CATEGORY, 12L, null)));

        String text = targetText(promotion, Map.of(
                PromotionPreview.targetKey(PromotionTargetType.CATEGORY, 12L), "Coffee"));

        assertTrue(text.contains("Coffee"),
                "the operator has to recognise the target by name, not by an id they "
                        + "would have to look up: got " + text);
    }

    @Test
    void aProductTargetIsNamedByItsProductName() {
        PromotionDto promotion = new PromotionDto();
        promotion.setTargets(List.of(target(PromotionTargetType.PRODUCT, null, 34L)));

        String text = targetText(promotion, Map.of(
                PromotionPreview.targetKey(PromotionTargetType.PRODUCT, 34L), "Americano"));

        assertTrue(text.contains("Americano"), "expected the product name, got " + text);
    }

    @Test
    void aCategoryAndAProductAreNamedByTheRightKindOfName() {
        PromotionDto promotion = new PromotionDto();
        promotion.setTargets(List.of(
                target(PromotionTargetType.CATEGORY, 12L, null),
                target(PromotionTargetType.PRODUCT, null, 34L)));

        String text = targetText(promotion, Map.of(
                PromotionPreview.targetKey(PromotionTargetType.CATEGORY, 12L), "Coffee",
                PromotionPreview.targetKey(PromotionTargetType.PRODUCT, 34L), "Americano"));

        assertTrue(text.contains("Coffee") && text.contains("Americano"),
                "each target is named by the entity it points at, not by a shared "
                        + "label: got " + text);
    }

    @Test
    void aTargetWithNoNameYetFallsBackToItsId() {
        PromotionDto promotion = new PromotionDto();
        promotion.setTargets(List.of(target(PromotionTargetType.CATEGORY, 12L, null)));

        String text = targetText(promotion, Map.of());

        assertTrue(text.contains("preview.target.category"),
                "names are fetched after the form restores its targets, so until one lands "
                        + "the fallback is all there is; it is a deliberate choice, not the "
                        + "symptom of a name that failed to arrive: got " + text);
    }

    @Test
    void aBlankNameIsTreatedAsNoName() {
        PromotionDto promotion = new PromotionDto();
        promotion.setTargets(List.of(target(PromotionTargetType.CATEGORY, 12L, null)));

        String blank = targetText(promotion, Map.of(
                PromotionPreview.targetKey(PromotionTargetType.CATEGORY, 12L), "   "));
        String absent = targetText(promotion, Map.of());

        assertEquals(absent, blank,
                "a name of whitespace is not a name. Rendering it would put a bare pair of "
                        + "parentheses where the name should be");
    }

    @Test
    void aPromotionWithNoTargetsShowsNothing() {
        assertEquals(null, targetText(new PromotionDto(), Map.of()),
                "no targets yet is the ordinary state of a promotion being written, and it "
                        + "is not the same as a target that cannot be named");
    }

    private static String targetText(PromotionDto promotion, Map<String, String> names) {
        return new PromotionPreview().buildTargetText(promotion, names);
    }

    private static PromotionTargetDto target(PromotionTargetType type, Long categoryId, Long productId) {
        return PromotionTargetDto.builder()
                .targetType(type)
                .categoryId(categoryId)
                .productId(productId)
                .build();
    }
}
