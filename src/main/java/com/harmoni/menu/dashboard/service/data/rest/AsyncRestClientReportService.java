package com.harmoni.menu.dashboard.service.data.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.harmoni.menu.dashboard.configuration.ReportProperties;
import com.harmoni.menu.dashboard.dto.report.DailyReportDto;
import com.harmoni.menu.dashboard.dto.report.OrderVolumeReportDto;
import com.harmoni.menu.dashboard.dto.report.ProductSalesReportDto;
import com.harmoni.menu.dashboard.dto.report.SettlementReportDto;
import com.harmoni.menu.dashboard.dto.report.TopProductReportDto;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Reactive client for the order service's reporting endpoints.
 *
 * <p>The reports live on their own {@code /api/v1/reports} base path in the
 * order microservice, and answer with the standard {@link RestAPIResponse}
 * envelope, so the calls reuse {@link AsyncRestClientBase#makeAsyncRequest}.</p>
 *
 * <p>Every report is date-ranged, so a {@code start}/{@code end} pair is
 * mandatory. The store a report covers is <em>not</em> passed from here: the API
 * gateway derives {@code X-Username} from the bearer token and forwards it, and
 * the order service resolves the store from that. Sending our own
 * {@code X-Username} as well makes the gateway emit the header twice, which the
 * order service reads as a single comma-joined value such as
 * {@code kasir1,kasir1} and then fails to resolve, so the header is deliberately
 * left to the gateway.</p>
 *
 * <p>Range values are sent as ISO local date-times ({@code yyyy-MM-dd'T'HH:mm:ss})
 * to match the service's {@code @DateTimeFormat(iso = ISO.DATE_TIME)} binding.
 * A zone offset or {@code Z} suffix would not parse, so the range is always sent
 * without one.</p>
 */
@Service
public class AsyncRestClientReportService extends AsyncRestClientBase {

    private static final DateTimeFormatter RANGE_FORMAT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private static final int DEFAULT_TOP_PRODUCT_LIMIT = 10;

    private final transient ReportProperties reportProperties;

    /**
     * Constructs the service with the required configuration and token refresh support.
     *
     * @param reportProperties     configuration for the report endpoints
     * @param tokenRefreshService  service to refresh expired tokens
     */
    public AsyncRestClientReportService(ReportProperties reportProperties,
                                        TokenRefreshService tokenRefreshService) {
        super(tokenRefreshService);
        this.reportProperties = reportProperties;
    }

    /**
     * Asynchronously fetches the daily settlement totals for a range.
     *
     * @param callback      success callback with the settlement totals
     * @param errorCallback error callback for handling failures
     * @param start         range start, inclusive
     * @param end           range end, inclusive
     */
    public void getSettlementAsync(AsyncRestCallback<SettlementReportDto> callback,
                                   AsyncRestCallback<Throwable> errorCallback,
                                   LocalDateTime start, LocalDateTime end) {
        makeAsyncRequest(buildReportUri(reportProperties.getUrl().getSettlement(), start, end, null),
                new TypeReference<SettlementReportDto>() {
                }, callback, errorCallback);
    }

    /**
     * Asynchronously fetches the best selling products for a range.
     *
     * @param callback      success callback with the ranking, highest first
     * @param errorCallback error callback for handling failures
     * @param start         range start, inclusive
     * @param end           range end, inclusive
     * @param limit         maximum number of products to return, non-positive for the service default
     */
    public void getTopProductsAsync(AsyncRestCallback<List<TopProductReportDto>> callback,
                                    AsyncRestCallback<Throwable> errorCallback,
                                    LocalDateTime start, LocalDateTime end, int limit) {
        makeAsyncRequest(buildReportUri(reportProperties.getUrl().getTopProducts(), start, end,
                        limit > 0 ? limit : DEFAULT_TOP_PRODUCT_LIMIT),
                new TypeReference<List<TopProductReportDto>>() {
                }, callback, errorCallback);
    }

    /**
     * Asynchronously fetches the per-day product breakdown for a range.
     *
     * @param callback      success callback with one entry per day that had sales
     * @param errorCallback error callback for handling failures
     * @param start         range start, inclusive
     * @param end           range end, inclusive
     */
    public void getDailyAsync(AsyncRestCallback<List<DailyReportDto>> callback,
                              AsyncRestCallback<Throwable> errorCallback,
                              LocalDateTime start, LocalDateTime end) {
        makeAsyncRequest(buildReportUri(reportProperties.getUrl().getDaily(), start, end, null),
                new TypeReference<List<DailyReportDto>>() {
                }, callback, errorCallback);
    }

    /**
     * Asynchronously fetches per-product sales totals for a range.
     *
     * @param callback      success callback with one row per product sold
     * @param errorCallback error callback for handling failures
     * @param start         range start, inclusive
     * @param end           range end, inclusive
     */
    public void getSalesAsync(AsyncRestCallback<List<ProductSalesReportDto>> callback,
                              AsyncRestCallback<Throwable> errorCallback,
                              LocalDateTime start, LocalDateTime end) {
        makeAsyncRequest(buildReportUri(reportProperties.getUrl().getSales(), start, end, null),
                new TypeReference<List<ProductSalesReportDto>>() {
                }, callback, errorCallback);
    }

    /**
     * Asynchronously fetches the order volume split for a range.
     *
     * @param callback      success callback with the order volume totals
     * @param errorCallback error callback for handling failures
     * @param start         range start, inclusive
     * @param end           range end, inclusive
     */
    public void getOrderVolumeAsync(AsyncRestCallback<OrderVolumeReportDto> callback,
                                    AsyncRestCallback<Throwable> errorCallback,
                                    LocalDateTime start, LocalDateTime end) {
        makeAsyncRequest(buildReportUri(reportProperties.getUrl().getOrderVolume(), start, end, null),
                new TypeReference<OrderVolumeReportDto>() {
                }, callback, errorCallback);
    }

    /**
     * Builds a report URI for a date range, optionally capping the row count.
     *
     * @param baseUrl configured report resource path
     * @param start   range start, inclusive
     * @param end     range end, inclusive
     * @param limit   maximum rows, or {@code null} to leave the service default
     * @return the fully built report URI
     */
    String buildReportUri(String baseUrl, LocalDateTime start, LocalDateTime end, Integer limit) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(baseUrl)
                .queryParam("start", RANGE_FORMAT.format(start))
                .queryParam("end", RANGE_FORMAT.format(end));
        if (limit != null) {
            builder.queryParam("limit", limit);
        }
        return builder.toUriString();
    }
}
