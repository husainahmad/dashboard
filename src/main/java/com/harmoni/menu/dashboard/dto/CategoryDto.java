package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.Date;

/**
 * Represents a menu category that belongs to a brand.
 *
 * <p>Used both as the create/update payload and as the response body for the
 * {@code /api/v1/category} endpoints. Categories support soft deletion, so a
 * non-null {@code deletedAt} means the row is no longer active.
 */

@Data
public class CategoryDto {

    /** The database id, or {@code null} when the row has not been saved yet. */

    private Integer id;

    /** The display name of the category. */

    private String name;

    /** Optional free-text description; {@code null} when none was provided. */

    private String description;

    /** The id of the owning brand, matching the entity carried by {@link #brandDto}. */

    private Integer brandId;

    /** Nested brand details, bound to the {@code brand} JSON property. */

    @JsonProperty("brand")
    private BrandDto brandDto;

    /** The timestamp the category was created, set by the server. */

    private Date createdAt;

    /** The timestamp of the last update, set by the server. */

    private Date updatedAt;

    /** Soft-delete timestamp; {@code null} while the category is active. */

    private Date deletedAt;

}