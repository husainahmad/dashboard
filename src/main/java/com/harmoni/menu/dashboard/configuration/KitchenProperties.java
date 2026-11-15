package com.harmoni.menu.dashboard.configuration;

import lombok.Data;

import java.io.Serializable;

/**
 * Kitchen station, printer and routing endpoints.
 */
@Data
public class KitchenProperties implements Serializable {
    private String stations;
    private String stationById;
    private String printersByStation;
    private String printerById;
    private String routings;
    private String routingCategories;
    private String routingProducts;
}
