package com.harmoni.menu.dashboard.dto;

import lombok.Data;

/**
 * Product to station routing row. Takes priority over category routing.
 */
@Data
public class KitchenProductRoutingDto {
    private Integer id;
    private Integer storeId;
    private Integer productId;
    private Integer kitchenStationId;
    private String productName;
    private String kitchenStationName;
}
