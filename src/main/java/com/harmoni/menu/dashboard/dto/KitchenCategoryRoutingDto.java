package com.harmoni.menu.dashboard.dto;

import lombok.Data;

/**
 * Category to station routing row.
 */
@Data
public class KitchenCategoryRoutingDto {
    private Integer id;
    private Integer storeId;
    private Integer categoryId;
    private Integer kitchenStationId;
    private String categoryName;
    private String kitchenStationName;
}
