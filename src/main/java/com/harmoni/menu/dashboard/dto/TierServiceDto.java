package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

/**
 * Assigns a sub-service to a service tier.
 *
 * <p>Returned by the tier-service endpoints; the {@code active} flag toggles
 * whether the sub-service participates in this tier.
 */

@Data
public class TierServiceDto {

    /** The database id, or {@code null} when the row has not been saved yet. */

    private Integer id;

    /** The id of the owning tier, matching the entity carried by {@link #tierDto}. */

    private Integer tierId;

    /** Nested tier details, bound to the {@code tier} JSON property. */

    @JsonProperty("tier")
    private TierDto tierDto;

    /** Nested sub-service details, bound to the {@code subService} JSON property. */

    @JsonProperty("subService")
    private SubServiceDto subServiceDto;

    /** Whether the sub-service is active under this service tier. */

    private boolean active;

    /** The timestamp the assignment was created, set by the server. */

    private Date createdAt;

    /** The timestamp of the last update, set by the server. */

    private Date updatedAt;

}