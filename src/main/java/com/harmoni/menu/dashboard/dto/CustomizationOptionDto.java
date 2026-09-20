package com.harmoni.menu.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * A single selectable option of a {@link CustomizationDto}.
 *
 * <p>Nested inside the customization create/update payloads and responses,
 * with its price per {@code PRICE} tier in {@link #tierPrices}.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomizationOptionDto {

    /** The database id, or {@code null} when the row has not been saved yet. */

    private Long id;

    /** The display name of the option. */

    private String name;

    /** Optional free-text description; {@code null} when none was provided. */

    private String description;

    /** Lifecycle state of the option, e.g. {@code Active} or {@code Inactive}. */

    private String status;

    /** The price of this option under each price tier; empty when not priced per tier. */

    private List<CustomizationOptionTierPriceDto> tierPrices;

}
