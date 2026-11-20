package com.harmoni.menu.dashboard.component;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Builder
@Data
@AllArgsConstructor
@RequiredArgsConstructor
public class BroadcastMessage {

    public static final String UN_AUTHORIZED = "UN_AUTHORIZED";
    public static final String BAD_REQUEST_FAILED = "BAD_REQUEST_FAILED";
    public static final String FORBIDDEN = "FORBIDDEN";

    public static final String CHAIN_INSERT_SUCCESS = "CHAIN_INSERT_SUCCESS";
    public static final String CHAIN_SUCCESS_UPDATED = "CHAIN_SUCCESS_UPDATED";
    public static final String BRAND_INSERT_SUCCESS = "BRAND_INSERT_SUCCESS";
    public static final String BRAND_SUCCESS_UPDATED = "BRAND_SUCCESS_UPDATED";

    public static final String TIER_INSERT_SUCCESS = "TIER_INSERT_SUCCESS";
    public static final String TIER_UPDATED_SUCCESS = "TIER_UPDATED_SUCCESS";
    public static final String TIER_DELETED_SUCCESS = "TIER_DELETED_SUCCESS";

    public static final String PRODUCT_INSERT_SUCCESS = "PRODUCT_INSERT_SUCCESS";
    public static final String PRODUCT_UPDATE_SUCCESS = "PRODUCT_UPDATE_SUCCESS";
    public static final String PRODUCT_DELETE_SUCCESS = "PRODUCT_DELETE_SUCCESS";

    public static final String CUSTOMIZATION_INSERT_SUCCESS = "CUSTOMIZATION_INSERT_SUCCESS";
    public static final String CUSTOMIZATION_UPDATED_SUCCESS = "CUSTOMIZATION_UPDATED_SUCCESS";
    public static final String CUSTOMIZATION_DELETE_SUCCESS = "CUSTOMIZATION_DELETE_SUCCESS";

    public static final String PROMOTION_INSERT_SUCCESS = "PROMOTION_INSERT_SUCCESS";
    public static final String PROMOTION_UPDATED_SUCCESS = "PROMOTION_UPDATED_SUCCESS";
    public static final String PROMOTION_STATUS_UPDATED_SUCCESS = "PROMOTION_STATUS_UPDATED_SUCCESS";
    public static final String PROMOTION_DELETE_SUCCESS = "PROMOTION_DELETE_SUCCESS";

    public static final String TABLE_INSERT_SUCCESS = "TABLE_INSERT_SUCCESS";
    public static final String TABLE_UPDATED_SUCCESS = "TABLE_UPDATED_SUCCESS";
    public static final String TABLE_DELETED_SUCCESS = "TABLE_DELETED_SUCCESS";

    public static final String FLOOR_INSERT_SUCCESS = "FLOOR_INSERT_SUCCESS";
    public static final String FLOOR_UPDATED_SUCCESS = "FLOOR_UPDATED_SUCCESS";
    public static final String FLOOR_DELETED_SUCCESS = "FLOOR_DELETED_SUCCESS";

    public static final String DEVICE_INSERT_SUCCESS = "DEVICE_INSERT_SUCCESS";
    public static final String DEVICE_UPDATED_SUCCESS = "DEVICE_UPDATED_SUCCESS";
    public static final String DEVICE_DELETED_SUCCESS = "DEVICE_DELETED_SUCCESS";

    private String type;
    private Object data;
}
