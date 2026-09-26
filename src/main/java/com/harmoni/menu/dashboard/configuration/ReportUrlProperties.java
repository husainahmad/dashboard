package com.harmoni.menu.dashboard.configuration;

import lombok.Data;

import java.io.Serializable;

/**
 * Per-report endpoint URLs, bound from {@code report.url.*}.
 *
 * <p>
 * Every report takes the same {@code start}/{@code end} ISO date-time range as a
 * query parameter, so those are appended by the report client rather than baked
 * into the configured URLs here. The configured values are therefore plain
 * resource paths that a date range can be added to.
 * </p>
 *
 * <p>
 * Defaults target the API gateway, which is how every other service is reached
 * and what keeps authentication, rate limiting and request logging in the path.
 * The gateway forwards {@code /api/v1/reports/**} to the order service, so the
 * public path is the same either way; override {@code report.url} to point at
 * the order service directly when working on it in isolation.
 * </p>
 */

@Data
public class ReportUrlProperties implements Serializable {

    private String settlement = "http://localhost:8080/api/v1/reports/settlement";

    private String topProducts = "http://localhost:8080/api/v1/reports/top-products";

    private String daily = "http://localhost:8080/api/v1/reports/daily";

    private String sales = "http://localhost:8080/api/v1/reports/sales";

    private String orderVolume = "http://localhost:8080/api/v1/reports/order-volume";
}
