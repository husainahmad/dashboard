package com.harmoni.menu.dashboard.dto;

import lombok.Data;

/**
 * A printer attached to a kitchen station.
 */
@Data
public class KitchenPrinterDto {

    private Integer id;
    private Integer kitchenStationId;
    private String name;
    private String printerType;
    private String connectionConfig;
    private String paperWidth;
    private Boolean isPrimary;
    private String status;
}
