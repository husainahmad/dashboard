package com.harmoni.menu.dashboard.util;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.support.ResourceBundleMessageSource;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Verifies the translation lookup used by {@link Messages}: the two bundles
 * must resolve for their own locale and the session locale must win over the
 * JVM default when one is stored.
 */
class MessagesLocaleTest {

    private static final String PROBE_KEY = "nav.language";

    /**
     * Builds a message source the same way Spring Boot does by default: the
     * {@code messages} basename resolved against the classpath bundles.
     */
    private static ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        return source;
    }

    @Test
    void resolvesEnglishFromDefaultBundle() {
        assertEquals("Language", messageSource().getMessage(PROBE_KEY, null, PROBE_KEY, Locale.ENGLISH));
    }

    @Test
    void resolvesIndonesianFromIdBundle() {
        assertEquals("Bahasa", messageSource().getMessage(PROBE_KEY, null, PROBE_KEY, new Locale("id", "ID")));
    }

    @Test
    void bothBundlesDefineTheCustomerKeys() {
        assertBothBundlesDefine(
                "tab.customerList", "action.view.name", "grid.empty.customers",
                "notification.customer.notFound", "nav.language.id");
    }

    @Test
    void bothBundlesDefineTheNavigationKeys() {
        assertBothBundlesDefine(
                "nav.overview", "nav.dashboard", "nav.organization", "nav.catalog", "nav.customers",
                "nav.customerList", "nav.inventory", "nav.inventoryStock", "nav.report",
                "nav.administration", "nav.settings",
                "nav.report.settlement", "nav.report.topProducts", "nav.report.daily",
                "nav.report.sales", "nav.report.orderVolume");
    }

    @Test
    void bothBundlesDefineTheReportKeys() {
        assertBothBundlesDefine(
                "report.range.start", "report.range.end", "report.range.apply", "report.range.invalid",
                "report.column.date", "report.column.quantity", "report.column.gross",
                "report.column.discount", "report.column.netSales", "report.column.sales",
                "report.settlement.description", "report.settlement.orders", "report.settlement.gross",
                "report.settlement.net", "report.settlement.discount", "report.settlement.tax",
                "report.settlement.refunds", "report.settlement.byPayment", "report.settlement.noPayments",
                "report.topProducts.description", "report.topProducts.empty",
                "report.daily.description", "report.daily.empty",
                "report.sales.description", "report.sales.empty",
                "report.orderVolume.description", "report.orderVolume.total", "report.orderVolume.peak",
                "report.orderVolume.offPeak", "report.orderVolume.split", "report.orderVolume.share",
                "report.orderVolume.empty",
                "notification.report.loadFailed");
    }

    @Test
    void customerListItemIsDistinctFromItsGroupLabel() {
        // "Customers" group containing a "Customers" item reads as a bug.
        assertNotEquals(
                messageSource().getMessage("nav.customers", null, "nav.customers", Locale.ENGLISH),
                messageSource().getMessage("nav.customerList", null, "nav.customerList", Locale.ENGLISH));
    }

    /**
     * Asserts every key resolves to a real translation in both bundles.
     *
     * @param keys the message keys to check
     */
    private static void assertBothBundlesDefine(String... keys) {
        ResourceBundleMessageSource source = messageSource();
        for (Locale locale : new Locale[]{Locale.ENGLISH, new Locale("id", "ID")}) {
            for (String key : keys) {
                String value = source.getMessage(key, null, key, locale);
                assertNotEquals(key, value, "missing " + key + " for " + locale);
            }
        }
    }
    @Test
    void sessionLocaleIsNullWithoutASession() {
        // No VaadinSession is bound to this thread, so the accessor must stay
        // null-safe and let Messages fall back to the JVM default locale.
        assertNull(VaadinSessionUtil.getLocale());
    }

    @Test
    void setLocaleIsNullSafeWithoutASession() {
        VaadinSessionUtil.setLocale(Locale.ENGLISH);
        assertNull(VaadinSessionUtil.getLocale());
    }

    @Test
    void unknownKeyFallsBackToTheKeyItself() {
        assertEquals("does.not.exist",
                messageSource().getMessage("does.not.exist", null, "does.not.exist", Locale.ENGLISH));
    }

    @Test
    void placeholderArgumentsAreFormatted() {
        String value = messageSource()
                .getMessage("action.view.name", new Object[]{"Ahmad"}, "action.view.name", Locale.ENGLISH);

        assertEquals("View Ahmad", value);
    }

    @Test
    void springContextExposesTheMessagesAccessor() {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
            context.register(Messages.class);
            context.refresh();

            assertNotNull(context.getBean(Messages.class));
            // No session bound here, so resolution falls back to the JVM locale.
            assertFalse(Messages.get("does.not.exist").isEmpty());
        }
    }
}
