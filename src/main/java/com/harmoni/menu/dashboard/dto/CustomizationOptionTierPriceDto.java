package com.harmoni.menu.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * The price of a {@link CustomizationOptionDto} under a billing tier.
 *
 * <p>One entry exists per tier the option is offered in, keyed by
 * {@link #tierId}.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomizationOptionTierPriceDto {

    /** The id of the price tier this price applies to. */

    private Integer tierId;

    /** The price of the option under {@link #tierId}. */

    private Double price;
}