package com.harmoni.menu.dashboard.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Date;

/**
 * Represents a brand, the top-level organizational unit of the menu domain.
 *
 * <p>Used both as the create/update payload and as the response body for the
 * {@code /api/v1/brand} endpoints; a {@code null} id means the row has not
 * been saved yet, while the timestamps are managed by the server.
 */

@Data
public class BrandDto {

    /** The database id, or {@code null} when the row has not been saved yet. */

    private Integer id;

    /** The display name of the brand; never blank (validated with {@code @NotEmpty}). */

    @NotEmpty
    private String name;

    /** The timestamp the brand was created, set by the server. */

    private Date createdAt;

    /** The timestamp of the last update, set by the server. */

    private Date updatedAt;

}