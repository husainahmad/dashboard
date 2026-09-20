package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.Date;

/**
 * Represents a restaurant table inside a store.
 *
 * <p>Used both as the create/update payload and as the response body for the
 * {@code /api/v1/table} endpoints; a {@code null} id means the row has not
 * been saved yet.
 */

@Data
public class TableDto {

    /** The database id, or {@code null} when the row has not been saved yet. */

    private Integer id;

    /** The display name of the table; never blank (validated with {@code @NotEmpty}). */

    @NotEmpty
    private String name;

    /** How many guests the table seats, between 1 and 999; never {@code null}. */

    @NotNull
    @Min(1)
    @Max(999)
    private Integer capacity;

    /** The id of the store the table belongs to. */

    private Integer storeId;

    /** Nested store details, bound to the {@code store} JSON property. */

    @JsonProperty("store")
    private StoreDto storeDto;

    /** The timestamp the table was created, set by the server. */

    private Date createdAt;

    /** The timestamp of the last update, set by the server. */

    private Date updatedAt;

    /** Soft-delete timestamp; {@code null} while the table is active. */

    private Date deletedAt;

}
