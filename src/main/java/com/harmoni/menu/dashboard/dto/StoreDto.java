package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.Date;

/**
 * Represents a store, a physical outlet that belongs to a chain.
 *
 * <p>Used both as the create/update payload and as the response body for the
 * {@code /api/v1/store} endpoints; the tier ids reference the price, menu and
 * service tiers active at the store.
 */

@Data
public class StoreDto {

    /** The database id, or {@code null} when the row has not been saved yet. */

    private Integer id;

    /** The display name of the store; never blank (validated with {@code @NotEmpty}). */

    @NotEmpty
    private String name;

    /** The id of the owning chain; never blank (validated with {@code @NotEmpty}). */

    @NotEmpty
    private Integer chainId;

    /** Nested chain details, bound to the {@code chain} JSON property. */

    @JsonProperty("chain")
    private ChainDto chainDto;

    /** The id of the menu tier assigned to the store; {@code null} when unassigned. */

    private Integer tierMenuId;

    /** The id of the price tier assigned to the store; {@code null} when unassigned. */

    private Integer tierPriceId;

    /** The id of the service tier assigned to the store; {@code null} when unassigned. */

    private Integer tierServiceId;

    /** The street address of the store. */

    private String address;

    /** The timestamp the store was created, set by the server. */

    private Date createdAt;

    /** The timestamp of the last update, set by the server. */

    private Date updatedAt;

    /** Soft-delete timestamp; {@code null} while the store is active. */

    private Date deletedAt;

}