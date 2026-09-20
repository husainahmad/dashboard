package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;


/**
 * Assigns a category to a menu tier.
 *
 * <p>Returned by the tier-menu endpoints and used as one element of the
 * {@code PUT /api/v1/tier/{id}/menu} payload; the {@code active} flag decides
 * whether the category is visible under this tier.
 */

@Data
public class TierMenuDto {

    /** The database id, or {@code null} when the row has not been saved yet. */

    private Integer id;

    /** The id of the owning tier, matching the entity carried by {@link #tierDto}. */

    private Integer tierId;

    /** Nested tier details, bound to the {@code tier} JSON property. */

    @JsonProperty("tier")
    private TierDto tierDto;

    /** The id of the assigned category, matching the entity carried by {@link #categoryDto}. */

    private Integer categoryId;

    /** Nested category details, bound to the {@code category} JSON property. */

    @JsonProperty("category")
    private CategoryDto categoryDto;

    /** Whether the category is currently active under this menu tier. */

    private Boolean active;

    /** The timestamp the assignment was created, set by the server. */

    private Date createdAt;

    /** The timestamp of the last update, set by the server. */

    private Date updatedAt;

    /** Soft-delete timestamp; {@code null} while the assignment is active. */

    private Date deletedAt;

}