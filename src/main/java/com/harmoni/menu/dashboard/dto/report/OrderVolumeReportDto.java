package com.harmoni.menu.dashboard.dto.report;

import lombok.Data;

import java.io.Serializable;

/**
 * Order counts split by peak and off-peak hours.
 *
 * <p>Mirrors the order service's {@code OrderVolumeReport} model.</p>
 */
@Data
public class OrderVolumeReportDto implements Serializable {

    private Integer totalOrders;

    private Integer peakTimeOrders;

    private Integer nonPeakTimeOrders;
}
