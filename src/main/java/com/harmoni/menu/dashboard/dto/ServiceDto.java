package com.harmoni.menu.dashboard.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Date;
import java.util.List;

/**
 * Represents a service that a store can offer, grouping its sub-services.
 *
 * <p>Used for the {@code /api/v1/service} endpoints and as the parent element
 * of the tier-service assignment tree.
 */

@Data
public class ServiceDto {

    /** The database id, or {@code null} when the row has not been saved yet. */

    private Integer id;

    /** The display name of the service; never blank (validated with {@code @NotEmpty}). */

    @NotEmpty
    private String name;

    /** The sub-services that belong to this service. */

    private List<SubServiceDto> subServices;

    /** The timestamp the service was created, set by the server. */

    private Date createdAt;

    /** The timestamp of the last update, set by the server. */

    private Date updatedAt;

}