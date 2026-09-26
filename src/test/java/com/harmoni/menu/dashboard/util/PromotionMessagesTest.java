package com.harmoni.menu.dashboard.util;

import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Guards the promotion message keys in both shipped locales, so a new label can
 * never reach production half-translated or showing a raw key.
 */
class PromotionMessagesTest {

    /** Every promotion key the views look up at runtime. */
    private static final Set<String> PROMOTION_KEYS = Set.of(
            "nav.promotion",
            "tab.promotionList", "tab.promotionNew", "tab.promotionEdit",
            "tab.promotionStatus", "tab.promotionStatusNamed",
            "tab.promotionSchedules", "tab.promotionTargets",
            "tab.promotionRules", "tab.promotionSpecialPrices",
            "action.newPromotion", "action.applyStatus",
            "action.addSchedule", "action.addTarget", "action.addRule", "action.addSpecialPrice",
            "label.name",
            "label.promotion.code", "label.promotion.priority", "label.promotion.status",
            "label.promotion.type", "label.promotion.startDate", "label.promotion.endDate",
            "label.promotion.enabled",
            "grid.empty.promotionList",
            "grid.header.code", "grid.header.status", "grid.header.priority",
            "grid.header.dateRange", "grid.header.actions", "grid.header.window",
            "grid.header.target", "grid.header.rule", "grid.header.specialPrice",
            "promotion.day.MONDAY", "promotion.day.TUESDAY", "promotion.day.WEDNESDAY",
            "promotion.day.THURSDAY", "promotion.day.FRIDAY", "promotion.day.SATURDAY",
            "promotion.day.SUNDAY",
            "promotion.schedule.none",
            "validation.promotion.codeRequired", "validation.promotion.codeMinLength",
            "validation.promotion.codeFormat", "validation.promotion.nameRequired",
            "validation.promotion.nameMinLength", "validation.promotion.typeRequired",
            "validation.promotion.statusRequired", "validation.promotion.priorityRequired",
            "validation.promotion.dateRange", "validation.promotion.formErrors",
            "validation.promotion.ruleRequired", "validation.promotion.specialPriceRequired",
            "validation.promotion.scheduleIncomplete", "validation.promotion.targetIncomplete",
            "validation.promotion.ruleIncomplete", "validation.promotion.specialPriceIncomplete",
            "notification.promotion.loadFailed", "notification.promotion.saveFailed",
            "notification.promotion.saved", "notification.promotion.deleted",
            "notification.promotion.deleteFailed", "notification.promotion.statusUpdated",
            "notification.promotion.statusFailed",
            "dialog.confirmDeletePromotion");

    @Test
    void everyPromotionKey_existsInTheDefaultLocale() {
        ResourceBundle bundle = ResourceBundle.getBundle("messages", Locale.ENGLISH);
        for (String key : PROMOTION_KEYS) {
            assertPresent(bundle, key);
        }
    }

    @Test
    void everyPromotionKey_existsInTheIndonesianLocale() {
        ResourceBundle bundle = ResourceBundle.getBundle("messages", new Locale("id"));
        for (String key : PROMOTION_KEYS) {
            assertPresent(bundle, key);
        }
    }

    @Test
    void indonesianBundle_isUsedRatherThanFallingBackToEnglish() {
        ResourceBundle bundle = ResourceBundle.getBundle("messages", new Locale("id"));

        assertTrue("Promosi".equals(bundle.getString("nav.promotion")),
                bundle.getString("nav.promotion"));
        assertTrue(bundle.getString("action.newPromotion").contains("Promosi"),
                bundle.getString("action.newPromotion"));
    }

    @Test
    void keysUsedAsMessagePatterns_declareTheirPlaceholders() {
        ResourceBundle english = ResourceBundle.getBundle("messages", Locale.ENGLISH);
        assertTrue(english.getString("notification.promotion.statusUpdated").contains("{0}"));
        assertTrue(english.getString("dialog.confirmDeletePromotion").contains("{0}"));
        assertTrue(english.getString("tab.promotionStatusNamed").contains("{0}"));
        assertTrue(english.getString("action.edit.name").contains("{0}"));
    }

    private static void assertPresent(ResourceBundle bundle, String key) {
        try {
            String value = bundle.getString(key);
            assertFalse(value.isBlank(), key + " must not be blank");
            assertFalse(value.startsWith("!!"), key + " has no translation");
        } catch (MissingResourceException e) {
            fail("Missing message key: " + key);
        }
    }
}
