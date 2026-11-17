package com.harmoni.menu.dashboard.service.data.rest;

import com.harmoni.menu.dashboard.configuration.CustomerProperties;
import com.harmoni.menu.dashboard.dto.CustomerDto;
import com.harmoni.menu.dashboard.dto.CustomerPageDto;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Reactive client for the standalone customer backend.
 *
 * <p>Unlike the organization and menu clients, the customer service answers
 * with plain JSON rather than the {@link RestAPIResponse} envelope, so the
 * requests go through {@link AsyncRestClientBase#makeAsyncRawRequestWithoutLogout}.
 * The bearer token is still attached when present, but a rejected call is shown
 * as a page-level load failure instead of triggering the global token refresh
 * and logout: a 401 while browsing customers must not sign the operator out.</p>
 *
 * <p>Server-side paging is zero-based, so callers pass {@code page} exactly as
 * the service expects it.</p>
 */
@Service
public class AsyncRestClientCustomerService extends AsyncRestClientBase {

    private final transient CustomerProperties customerProperties;

    /**
     * Constructs the service with the required configuration and token refresh support.
     *
     * @param customerProperties   configuration for the customer endpoints
     * @param tokenRefreshService  service to refresh expired tokens
     */
    public AsyncRestClientCustomerService(CustomerProperties customerProperties,
                                          TokenRefreshService tokenRefreshService) {
        super(tokenRefreshService);
        this.customerProperties = customerProperties;
    }

    /**
     * Asynchronously searches customers by name, phone or email.
     *
     * @param callback      success callback with one page of customers
     * @param errorCallback error callback for handling failures
     * @param search        free-text filter, blank to list every customer
     * @param brandId       optional brand filter
     * @param page          zero-based page index
     * @param size          page size
     */
    public void searchCustomersAsync(AsyncRestCallback<CustomerPageDto> callback,
                                     AsyncRestCallback<Throwable> errorCallback,
                                     String search, Integer brandId, int page, int size) {
        makeAsyncRawRequestWithoutLogout(buildSearchUri(search, brandId, page, size),
                CustomerPageDto.class, callback, errorCallback);
    }

    /**
     * Builds the search URI for the given filter and zero-based page.
     *
     * @param search  free-text filter, blank to list every customer
     * @param brandId optional brand filter
     * @param page    zero-based page index
     * @param size    page size
     * @return the fully built search URI
     */
    String buildSearchUri(String search, Integer brandId, int page, int size) {
        UriComponentsBuilder builder = UriComponentsBuilder
                .fromUriString(customerProperties.getUrl().getCustomers())
                .queryParam("search", search == null ? "" : search);
        if (brandId != null) {
            builder.queryParam("brandId", brandId);
        }
        return builder.queryParam("page", page)
                .queryParam("size", size)
                .toUriString();
    }

    /**
     * Asynchronously fetches a single customer by its id.
     *
     * @param callback      success callback with the customer
     * @param errorCallback error callback for handling failures
     * @param customerId    the id of the customer to load
     */
    public void getCustomerAsync(AsyncRestCallback<CustomerDto> callback,
                                 AsyncRestCallback<Throwable> errorCallback,
                                 Long customerId) {
        makeAsyncRawRequestWithoutLogout(buildCustomerUri(customerId), CustomerDto.class, callback, errorCallback);
    }

    /**
     * Builds the single-customer URI for the given id.
     *
     * @param customerId the id of the customer to load
     * @return the fully built detail URI
     */
    String buildCustomerUri(Long customerId) {
        return String.format(customerProperties.getUrl().getCustomersById(), customerId);
    }
}
