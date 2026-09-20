package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import lombok.Data;


/**
 * One sub-service entry of the tier-service assignment payload.
 *
 * <p>Element of the list body for {@code PUT /api/v1/tier/{id}/service},
 * carrying the sub-service reference and whether it is active for the tier.
 */

@Data
public class TierSubServiceDto {

    /** The database id, or {@code null} when the row has not been saved yet. */

    private Integer id;

    /** Nested service details, bound to the {@code service} JSON property. */

    @JsonProperty("service")
    private ServiceDto serviceDto;

    /** The owning tier, validated as a whole, bound to the {@code tier} JSON property. */

    @JsonProperty("tier")
    private @Valid TierDto tierDto;

    /** Nested sub-service details, bound to the {@code subService} JSON property. */

    @JsonProperty("subService")
    private SubServiceDto subServiceDto;

    /** The id of the sub-service, bound to the {@code subServiceId} JSON property. */

    @JsonProperty("subServiceId")
    private Integer subServiceId;

    /** Whether the sub-service is active for the owning tier. */

    @JsonProperty("active")
    private boolean active;

}