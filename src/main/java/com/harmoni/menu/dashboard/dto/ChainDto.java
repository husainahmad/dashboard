package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Date;

/**
 * Represents a chain of stores that belongs to one brand.
 *
 * <p>Used both as the create/update payload and as the response body for the
 * {@code /api/v1/chain} endpoints; a {@code null} id means the row has not
 * been saved yet.
 */

@Data
public class ChainDto {

    /** The database id, or {@code null} when the row has not been saved yet. */

    private Integer id;

    /** The display name of the chain; never blank (validated with {@code @NotEmpty}). */

    @NotEmpty
    private String name;

    /** The id of the owning brand; never blank (validated with {@code @NotEmpty}). */

    @NotEmpty
    private Integer brandId;

    /** Nested brand details, bound to the {@code brand} JSON property. */

    @JsonProperty("brand")
    private BrandDto brandDto;

    /** The timestamp the chain was created, set by the server. */

    private Date createdAt;

    /** The timestamp of the last update, set by the server. */

    private Date updatedAt;

    /** Soft-delete timestamp; {@code null} while the chain is active. */

    private Date deletedAt;

}