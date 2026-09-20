package com.harmoni.menu.dashboard.dto;

import com.harmoni.menu.dashboard.layout.enums.SelectionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


/**
 * Master definition of a product customization and the options it offers.
 *
 * <p>Used both as the create/update payload and as the response body for the
 * {@code /api/v1/customization} endpoints; each option carries its per-tier
 * prices in {@link CustomizationOptionDto#tierPrices}.
 */

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomizationDto {

    /** The database id, or {@code null} when the row has not been saved yet. */

    private Integer id;

    /** The display name of the customization. */

    private String name;

    /** Optional free-text description; {@code null} when none was provided. */

    private String description;

    /** Whether one or several options can be selected for this customization. */

    private SelectionType selectionType;

    /** Whether every product using this customization must make a selection. */

    private Boolean required;

    /** Minimum number of options that must be selected; {@code null} when unrestricted. */

    private Integer minimumSelection;

    /** Maximum number of options that may be selected; {@code null} when unrestricted. */

    private Integer maximumSelection;

    /** The available options, each with optional per-tier prices. */

    private List<CustomizationOptionDto> customizationOptions;

}