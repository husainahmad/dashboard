package com.harmoni.menu.dashboard.dto.report;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * A single day's product breakdown, holding one row per product sold that day.
 *
 * <p>Mirrors the order service's {@code DailyReport} model.</p>
 */
@Data
public class DailyReportDto implements Serializable {

    private String date;

    private List<DailyProductReportDto> products;
}
