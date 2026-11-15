package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Date;

/**
 * A floor of a store. A store has many floors and each floor groups a set of
 * tables.
 */
@Data
public class FloorDto {

    /** The database id, or {@code null} when the floor has not been saved yet. */
    private Integer id;

    /** The display name of the floor. */
    @NotEmpty
    private String name;

    /** Order in which floors are listed, ascending. */
    private Integer sortOrder;

    /** Whether the floor is active. */
    private Boolean active;

    /** The id of the store the floor belongs to. */
    private Integer storeId;

    /** Nested store details, bound to the {@code store} JSON property. */
    @JsonProperty("store")
    private StoreDto storeDto;

    private Date createdAt;

    private Date updatedAt;

    private Date deletedAt;
}
