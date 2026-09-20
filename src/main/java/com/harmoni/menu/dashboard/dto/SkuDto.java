package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * Represents a stock-keeping unit of a product, priced per tier.
 *
 * <p>Nested inside {@link ProductDto#skuDtos} on product create/update payloads
 * and responses; each SKU carries its prices per tier in
 * {@link #skuTierPriceDtos}.
 */

@Data
public class SkuDto {

    /** The database id, or {@code null} when the SKU has not been saved yet. */

    private Integer id;

    /** The display name of the SKU. */

    private String name;

    /** Optional free-text description; {@code null} when none was provided. */

    private String description;

    /** The id of the owning product, matching the entity carried by {@link #productDto}. */

    private Integer productId;

    /** Nested product details, bound to the {@code product} JSON property. */

    @JsonProperty("product")
    private ProductDto productDto;

    /** The prices of this SKU under each tier, bound to the {@code tierPrices} JSON property. */

    @JsonProperty("tierPrices")
    private List<SkuTierPriceDto> skuTierPriceDtos;

    /** The timestamp the SKU was created, set by the server. */

    private Date createdAt;

    /** The timestamp of the last update, set by the server. */

    private Date updatedAt;

    /** Whether the SKU is currently offered for sale. */

    private Boolean active;

}