package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Date;

/**
 * Represents a sub-service that belongs to a {@link ServiceDto}.
 *
 * <p>Used for the service endpoints and as the leaf element of the
 * tier-service assignment tree.
 */

@Data
public class SubServiceDto {

    /** The database id, or {@code null} when the row has not been saved yet. */

    private Integer id;

    /** The display name of the sub-service; never blank (validated with {@code @NotEmpty}). */

    @NotEmpty
    private String name;

    /** The id of the owning service; never blank (validated with {@code @NotEmpty}). */

    @NotEmpty
    private Integer serviceId;

    /** Nested service details, bound to the {@code service} JSON property. */

    @JsonProperty("service")
    private ServiceDto serviceDto;

    /** The timestamp the sub-service was created, set by the server. */

    private Date createdAt;

    /** The timestamp of the last update, set by the server. */

    private Date updatedAt;

}