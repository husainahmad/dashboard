package com.harmoni.menu.dashboard.dto;

import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * One page of customers returned by the customer service search endpoint.
 *
 * <p>Mirrors the server-side {@code PageResponse} record
 * ({@code content}, {@code totalElements}, {@code totalPages}, {@code page},
 * {@code size}). Note the server pages are <em>zero-based</em>, unlike the
 * one-based paging state shared by the dashboard list views.</p>
 */

@Data
public class CustomerPageDto {

    /** The customers of the requested page, never {@code null}. */

    private List<CustomerDto> content = Collections.emptyList();

    /** The total number of customers matching the search, across all pages. */

    private long totalElements;

    /** The total number of pages available for the current page size. */

    private int totalPages;

    /** The zero-based index of the returned page, as sent by the server. */

    private int page;

    /** The page size applied by the server. */

    private int size;
}
