package com.harmoni.menu.dashboard.service.data.rest;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harmoni.menu.dashboard.configuration.ReportProperties;
import com.harmoni.menu.dashboard.configuration.ReportUrlProperties;
import com.harmoni.menu.dashboard.dto.report.DailyReportDto;
import com.harmoni.menu.dashboard.dto.report.OrderVolumeReportDto;
import com.harmoni.menu.dashboard.dto.report.ProductSalesReportDto;
import com.harmoni.menu.dashboard.dto.report.SettlementReportDto;
import com.harmoni.menu.dashboard.dto.report.TopProductReportDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the report client against the order service's contract: report
 * resources under {@code /api/v1/reports}, a mandatory ISO local date-time
 * range, and a {@code RestAPIResponse} envelope.
 */
class AsyncRestClientReportServiceTest {

    private static final String BASE_URL = "http://localhost:8083/api/v1/reports";

    private static final LocalDateTime START = LocalDateTime.of(2026, 8, 17, 0, 0, 0);

    private static final LocalDateTime END = LocalDateTime.of(2026, 8, 23, 23, 59, 59);

    private AsyncRestClientReportService client;

    private ReportUrlProperties urls;

    @BeforeEach
    void setUp() {
        urls = new ReportUrlProperties();
        urls.setSettlement(BASE_URL + "/settlement");
        urls.setTopProducts(BASE_URL + "/top-products");
        urls.setDaily(BASE_URL + "/daily");
        urls.setSales(BASE_URL + "/sales");
        urls.setOrderVolume(BASE_URL + "/order-volume");
        ReportProperties properties = new ReportProperties();
        properties.setUrl(urls);
        client = new AsyncRestClientReportService(properties, null);
    }

    @Test
    void buildReportUri_appendsIsoLocalDateTimeRange() {
        String uri = client.buildReportUri(urls.getSettlement(), START, END, null);

        assertTrue(uri.startsWith(BASE_URL + "/settlement"), uri);
        assertTrue(uri.contains("start=2026-08-17T00:00:00"), uri);
        assertTrue(uri.contains("end=2026-08-23T23:59:59"), uri);
    }

    @Test
    void buildReportUri_omitsLimitWhenNotCapped() {
        assertFalse(client.buildReportUri(urls.getDaily(), START, END, null).contains("limit="));
    }

    @Test
    void buildReportUri_addsLimitWhenCapped() {
        assertTrue(client.buildReportUri(urls.getTopProducts(), START, END, 25).contains("limit=25"));
    }

    @Test
    void buildReportUri_neverSendsAZoneOffset() {
        String uri = client.buildReportUri(urls.getSales(), START, END, null);

        assertFalse(uri.contains("%3A"), uri);
        assertFalse(uri.contains("Z&"), uri);
        assertFalse(uri.endsWith("Z"), uri);
    }

    @Test
    void settlement_deserializesReportEnvelopeData() throws Exception {
        String json = """
                {
                  "httpStatus": 200,
                  "data": {
                    "reportDate": "2026-08-23",
                    "totalOrders": 12,
                    "totalSales": 1500000.00,
                    "totalDiscounts": 50000.00,
                    "totalTax": 165000.00,
                    "totalNetSales": 1615000.00,
                    "paymentBreakdown": { "CASH": 1000000.00, "CARD": 615000.00 },
                    "totalRefunds": 0.00
                  }
                }
                """;

        SettlementReportDto report = objectMapper().convertValue(envelopeData(json), SettlementReportDto.class);

        assertEquals(12, report.getTotalOrders());
        assertEquals("2026-08-23", report.getReportDate());
        assertAmount("1500000.00", report.getTotalSales());
        assertAmount("1615000.00", report.getTotalNetSales());
        assertAmount("0.00", report.getTotalRefunds());
        assertNotNull(report.getPaymentBreakdown());
        assertAmount("1000000.00", report.getPaymentBreakdown().get("CASH"));
    }

    @Test
    void settlement_toleratesMissingPaymentBreakdownAndUnknownFields() throws Exception {
        String json = "{\"httpStatus\":200,\"data\":{\"totalOrders\":0,\"extraField\":123}}";

        SettlementReportDto report = objectMapper().convertValue(envelopeData(json), SettlementReportDto.class);

        assertEquals(0, report.getTotalOrders());
        assertEquals(null, report.getPaymentBreakdown());
        assertEquals(null, report.getTotalSales());
    }

    @Test
    void topProducts_deserializesReportList() throws Exception {
        String json = """
                {
                  "httpStatus": 200,
                  "data": [
                    { "productId": 3, "productName": "Nasi Goreng", "totalSales": 450000.00, "quantitySold": 30 }
                  ]
                }
                """;

        List<TopProductReportDto> rows = rowsOf(json, TopProductReportDto.class);

        assertEquals(1, rows.size());
        assertEquals(3, rows.getFirst().getProductId());
        assertEquals("Nasi Goreng", rows.getFirst().getProductName());
        assertEquals(30, rows.getFirst().getQuantitySold());
        assertAmount("450000.00", rows.getFirst().getTotalSales());
    }

    @Test
    void daily_deserializesNestedProductsPerDay() throws Exception {
        String json = """
                {
                  "httpStatus": 200,
                  "data": [
                    {
                      "date": "2026-08-23",
                      "products": [
                        { "productId": 1, "productName": "Es Teh", "quantity": 5,
                          "netSales": 25000.00, "discount": 0.00 }
                      ]
                    }
                  ]
                }
                """;

        List<DailyReportDto> days = rowsOf(json, DailyReportDto.class);

        assertEquals(1, days.size());
        assertEquals("2026-08-23", days.getFirst().getDate());
        assertEquals(1, days.getFirst().getProducts().size());
        assertEquals("Es Teh", days.getFirst().getProducts().getFirst().getProductName());
        assertAmount("25000.00", days.getFirst().getProducts().getFirst().getNetSales());
    }

    @Test
    void sales_deserializesCategoryBreakdown() throws Exception {
        String json = """
                {
                  "httpStatus": 200,
                  "data": [
                    { "categoryId": 2, "categoryName": "Makanan", "productId": 3, "productName": "Nasi Goreng",
                      "quantity": 10, "grossSales": 150000.00, "discount": 15000.00, "netSales": 135000.00 }
                  ]
                }
                """;

        List<ProductSalesReportDto> rows =
                rowsOf(json, ProductSalesReportDto.class);

        assertEquals("Makanan", rows.getFirst().getCategoryName());
        assertAmount("150000.00", rows.getFirst().getGrossSales());
        assertAmount("135000.00", rows.getFirst().getNetSales());
    }

    @Test
    void orderVolume_deserializesPeakSplit() throws Exception {
        String json = """
                {
                  "httpStatus": 200,
                  "data": { "totalOrders": 40, "peakTimeOrders": 25, "nonPeakTimeOrders": 15 }
                }
                """;

        OrderVolumeReportDto report = objectMapper().convertValue(envelopeData(json), OrderVolumeReportDto.class);

        assertEquals(40, report.getTotalOrders());
        assertEquals(25, report.getPeakTimeOrders());
        assertEquals(15, report.getNonPeakTimeOrders());
    }

    @Test
    void reportUrls_defaultToTheGatewayNotTheOrderServicePort() {
        ReportUrlProperties defaults = new ReportUrlProperties();

        Map<String, String> paths = Map.of(
                "settlement", defaults.getSettlement(),
                "topProducts", defaults.getTopProducts(),
                "daily", defaults.getDaily(),
                "sales", defaults.getSales(),
                "orderVolume", defaults.getOrderVolume());

        paths.forEach((name, url) -> {
            assertTrue(url.startsWith("http://localhost:8080/api/v1/reports/"), name + " -> " + url);
        });
    }

    /**
     * Mirrors the lenient mapper the reactive client relies on, so the test
     * fails if the wire format ever drifts from the DTOs.
     */
    private static ObjectMapper objectMapper() {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    /**
     * Extracts the {@code data} node of a {@code RestAPIResponse} envelope.
     *
     * <p>The reactive client never deserializes the envelope itself with the
     * plain mapper: WebClient decodes it, and the client then converts only the
     * already generic {@code data} value. Reading the payload the same way here
     * keeps the test on the same path as production.</p>
     *
     * @param envelope the raw envelope JSON
     * @return the generic {@code data} node
     */
    private static Object envelopeData(String envelope) throws Exception {
        return objectMapper().readValue(envelope, Map.class).get("data");
    }

    /**
     * Asserts a monetary amount, ignoring the scale.
     *
     * <p>{@link BigDecimal#equals(Object)} is scale sensitive, so
     * {@code 1500000.0} would not equal {@code 1500000.00} even though both
     * describe the same amount. The scale a service chooses for a decimal is not
     * part of its contract, so the comparison is by value.</p>
     *
     * @param expected the expected amount
     * @param actual   the parsed amount
     */
    private static void assertAmount(String expected, BigDecimal actual) {
        assertNotNull(actual, "amount should be parsed");
        assertEquals(0, new BigDecimal(expected).compareTo(actual), "expected " + expected + " but was " + actual);
    }

    /**
     * Reads a {@code RestAPIResponse} envelope whose {@code data} node is a list
     * of report rows.
     *
     * @param envelope the raw envelope JSON
     * @param element  the report row type
     * @return the deserialized rows
     */
    private static <T> List<T> rowsOf(String envelope, Class<T> element) throws Exception {
        return objectMapper().convertValue(envelopeData(envelope),
                objectMapper().getTypeFactory().constructCollectionType(List.class, element));
    }
}
