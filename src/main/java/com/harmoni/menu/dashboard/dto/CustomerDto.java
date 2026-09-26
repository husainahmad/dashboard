package com.harmoni.menu.dashboard.dto;

import lombok.Data;

import java.util.Date;

/**
 * A customer record as returned by the customer service
 * ({@code /api/v1/customers}).
 *
 * <p>Mirrors the server-side {@code CustomerResponse}. The service serialises
 * {@code createdAt} / {@code updatedAt} as ISO-8601 instants, which Jackson
 * deserialises into {@link Date} without extra configuration. Customers are
 * soft-deleted server-side, so deleted records never appear here.
 * </p>
 */

@Data
public class CustomerDto {

    /** The database id of the customer. */

    private Long id;

    /** The customer display name. */

    private String name;

    /** The phone number; the server requires it to be unique. */

    private String phone;

    /** The email address, may be {@code null}. */

    private String email;

    /** The timestamp the customer was created, set by the server. */

    private Date createdAt;

    /** The timestamp of the last update, set by the server. */

    private Date updatedAt;
}
