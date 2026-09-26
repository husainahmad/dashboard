package com.harmoni.menu.dashboard.configuration;

import lombok.Data;

import java.io.Serializable;

/**
 * Holds the REST URLs of the customer service endpoints.
 *
 * <p>
 * Binds the {@code customer.url.*} keys: the search/collection path and the
 * single-resource path formatted with the customer id. The default points at the
 * API gateway, like the other services, which forwards
 * {@code /api/v1/customers/**} to the customer service; it is overridden from
 * {@code application.properties} per environment.
 * </p>
 */

@Data
public class CustomerUrlProperties implements Serializable {

    /** Default base URL, reached through the API gateway. */
    private static final String DEFAULT_BASE_URL = "http://localhost:8080/api/v1/customers";

    private String customers = DEFAULT_BASE_URL;

    private String customersById = DEFAULT_BASE_URL + "/%d";
}
