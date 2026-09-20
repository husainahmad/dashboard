package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

/**
 * The price of a {@link SkuDto} under a billing tier.
 *
 * <p>Nested inside {@link SkuDto#skuTierPriceDtos}; a SKU carries one entry
 * per tier it is offered under.
 */

@Data
public class SkuTierPriceDto {

    /** The database id, or {@code null} when the row has not been saved yet. */

    private Integer id;

    /** The id of the SKU this price belongs to. */

    private Integer skuId;

    /** The id of the tier this price applies to. */

    private Integer tierId;

    /** Nested tier details, bound to the {@code tier} JSON property. */

    @JsonProperty("tier")
    private TierDto tierDto;

    /** The selling price of the SKU under {@link #tierId}. */

    private Double price;

    /** The timestamp the price was created, set by the server. */

    private Date createdAt;

    /** The timestamp of the last update, set by the server. */

    private Date updatedAt;

}