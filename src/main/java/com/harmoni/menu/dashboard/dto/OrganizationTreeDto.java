package com.harmoni.menu.dashboard.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Nested brand → chain → store view returned by
 * {@code GET /api/v1/organization/tree}.
 */
@Data
public class OrganizationTreeDto {

    private List<BrandNode> brands = new ArrayList<>();

    /** A brand and its children. */
    @Data
    public static class BrandNode {
        private Integer id;
        private String name;
        private String logoUrl;
        private String timeZone;
        private String currency;
        private String adminEmail;
        private String initialMenuMode;
        private Integer sourceBrandId;
        private int chainCount;
        private int storeCount;
        private List<ChainNode> chains = new ArrayList<>();
    }

    /** A chain and its stores. */
    @Data
    public static class ChainNode {
        private Integer id;
        private String name;
        private Integer brandId;
        private String type;
        private String managerEmail;
        private String taxServiceMode;
        private BigDecimal taxPercent;
        private BigDecimal serviceChargePercent;
        private int storeCount;
        private List<StoreNode> stores = new ArrayList<>();
    }

    /** A store with its settings and counts. */
    @Data
    public static class StoreNode {
        private Integer id;
        private String name;
        private Integer chainId;
        private String address;
        private String orderTypes;
        private String taxServiceMode;
        private BigDecimal taxPercent;
        private BigDecimal serviceChargePercent;
        private String receiptLogoMode;
        private String logoUrl;
        private String menuPriceMode;
        private String orderTypeMode;
        private String timeZone;
        private Integer tierMenuId;
        private Integer tierPriceId;
        private Integer tierServiceId;
        private int tableCount;
        private int terminalCount;
    }
}
