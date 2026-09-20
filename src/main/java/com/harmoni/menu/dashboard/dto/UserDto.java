package com.harmoni.menu.dashboard.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Date;

/**
 * Represents an application user who signs in to the dashboard.
 *
 * <p>Used both as the create/update payload and as the response body for the
 * {@code /api/v1/user} endpoints, and stored in the Vaadin session after a
 * successful login; a {@code null} id means the row has not been saved yet.
 */

@Data
public class UserDto {

    /** The database id, or {@code null} when the row has not been saved yet. */

    private Integer id;

    /** The id of the linked authentication record. */

    private Integer authId;

    /** The login username of the user. */

    private String username;

    /** The email address of the user. */

    private String email;

    /** The plain-text password; mostly set when creating a new user. */

    private String password;

    /** The id of the store the user belongs to, matching the entity carried by {@link #storeDto}. */

    private Integer storeId;

    /** Nested store details, bound to the {@code store} JSON property. */

    @JsonProperty("store")
    private StoreDto storeDto;

    /** The timestamp the user was created, set by the server. */

    private Date createdAt;

    /** The timestamp of the last update, set by the server. */

    private Date updatedAt;

    /** Soft-delete timestamp; {@code null} while the user is active. */

    private Date deletedAt;
}
