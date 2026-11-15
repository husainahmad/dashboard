package com.harmoni.menu.dashboard.dto;

import lombok.Data;

import java.util.List;

/**
 * Both routing tables for a store.
 */
@Data
public class KitchenRoutingDto {
    private List<KitchenCategoryRoutingDto> categories;
    private List<KitchenProductRoutingDto> products;
}
