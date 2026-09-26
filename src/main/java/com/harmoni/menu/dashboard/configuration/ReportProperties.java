package com.harmoni.menu.dashboard.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.Serializable;

/**
 * Root configuration for the reporting endpoints of the order service.
 *
 * <p>
 * Binds the {@code report.*} keys, with the per-report URLs exposed through the
 * nested {@link ReportUrlProperties} under {@code report.url.*}.
 * </p>
 *
 * <p>
 * The reports are served by the order microservice on its own {@code /api/v1/reports}
 * base path, not under {@code /api/v1/order}, so they get their own base URL
 * instead of reusing {@link MenuProperties}.
 * </p>
 */

@Configuration
@Data
@ConfigurationProperties("report")
public class ReportProperties implements Serializable {
    private transient ReportUrlProperties url = new ReportUrlProperties();
}
