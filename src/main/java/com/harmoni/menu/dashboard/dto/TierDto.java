package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Date;

/**
 * Represents a price, menu or service tier that a store can adopt.
 *
 * <p>Used both as the create/update payload and as the response body for the
 * {@code /api/v1/tier} endpoints; the kind of tier is given by {@link #type}.
 */

@Data
public class TierDto {

    /** The database id, or {@code null} when the row has not been saved yet. */

    private Integer id;

    /** The display name of the tier; never blank (validated with {@code @NotEmpty}). */

    @NotEmpty
    private String name;

    /** The id of the owning brand; never blank (validated with {@code @NotEmpty}). */

    @NotEmpty
    private Integer brandId;

    /** Classifies the tier as price, menu or service; never blank. */

    @NotEmpty
    private TierTypeDto type;

    /** Nested brand details, bound to the {@code brand} JSON property. */

    @NotEmpty
    @JsonProperty("brand")
    private BrandDto brandDto;

    /** The timestamp the tier was created, set by the server. */

    private Date createdAt;

    /** The timestamp of the last update, set by the server. */

    private Date updatedAt;

}