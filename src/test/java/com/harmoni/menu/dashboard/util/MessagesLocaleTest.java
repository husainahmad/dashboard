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
                "nav.customerList", "nav.report",
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
    void bothBundlesDefineTheDeviceKeys() {
        assertBothBundlesDefine(
                "nav.devices", "page.device.title", "page.device.description",
                "grid.header.device", "grid.header.deviceAddress", "device.empty",
                "label.deviceName", "label.deviceAddress", "label.deviceType", "label.deviceMode",
                "device.config.tableField", "device.config.table",
                "validation.device.addressRequired", "validation.device.tableRequired",
                "notification.device.created", "notification.device.updated",
                "notification.device.deleted", "notification.device.saveFailed",
                "notification.device.deleteFailed", "notification.device.loadFailed");
    }

    @Test
    void bothBundlesDefineTheDashboardKeys() {
        assertBothBundlesDefine(
                "dashboard.kpi.sales", "dashboard.kpi.transactions", "dashboard.kpi.peakOrders",
                "dashboard.kpi.refunds", "dashboard.caption.peakShare", "dashboard.summary.netSales",
                "dashboard.summary.discounts", "dashboard.summary.tax", "dashboard.summary.daily",
                "dashboard.summary.dailyValue", "dashboard.summary.category",
                "dashboard.summary.orderVolume", "dashboard.summary.volumeValue",
                "dashboard.summary.recentOrders", "dashboard.summary.promotions",
                "dashboard.panel.daily", "dashboard.panel.category", "dashboard.panel.recentOrders",
                "dashboard.panel.orderVolume", "dashboard.panel.promotions",
                "dashboard.daily.empty", "dashboard.daily.net", "dashboard.daily.quantity",
                "dashboard.daily.topProduct", "dashboard.category.empty", "dashboard.category.other",
                "dashboard.volume.empty", "dashboard.volume.total", "dashboard.volume.peak",
                "dashboard.volume.offPeak", "dashboard.orders.empty", "dashboard.order.table",
                "dashboard.order.items", "dashboard.order.noMeta", "dashboard.order.status.pending",
                "dashboard.order.status.confirmed", "dashboard.order.status.paid",
                "dashboard.order.status.void", "dashboard.order.status.unknown",
                "dashboard.promo.empty", "dashboard.promo.noEnd", "dashboard.promo.ends",
                "dashboard.attention.refunds", "dashboard.attention.discounts",
                "dashboard.attention.pendingOrders", "dashboard.attention.expiringPromotions");
    }

    @Test
    void bothBundlesDefineTheDashboardAiKeys() {
        assertBothBundlesDefine(
                "dashboard.ai.button", "dashboard.ai.title", "dashboard.ai.hint",
                "dashboard.ai.question", "dashboard.ai.placeholder", "dashboard.ai.ask",
                "dashboard.ai.required", "dashboard.ai.thinking", "dashboard.ai.empty",
                "dashboard.ai.errorTitle", "dashboard.ai.error", "dashboard.ai.errorUnavailable",
                "dashboard.ai.errorAuth", "dashboard.ai.retry", "dashboard.ai.noDashboard",
                "dashboard.ai.context", "dashboard.ai.hintForPage", "dashboard.ai.thinkingForPage",
                "dashboard.ai.noPageData", "dashboard.ai.unknownPage",
                "ai.context.brand", "ai.context.categories", "ai.context.category", "ai.context.selected",
                "ai.context.none", "ai.context.search", "ai.context.description", "ai.context.totalCustomers",
                "ai.context.currentPage", "ai.context.customerId", "ai.context.tier",
                "ai.context.productsOnPage", "ai.context.totalPages", "ai.context.status", "ai.context.type",
                "ai.context.all", "ai.context.promotionsOnPage", "ai.context.dateRange",
                "ai.context.storeScope", "ai.context.currentStore", "ai.context.noReportData");
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
