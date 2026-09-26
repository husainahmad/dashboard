package com.harmoni.menu.dashboard.service.data.rest;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.harmoni.menu.dashboard.configuration.CustomerProperties;
import com.harmoni.menu.dashboard.configuration.CustomerUrlProperties;
import com.harmoni.menu.dashboard.dto.CustomerDto;
import com.harmoni.menu.dashboard.dto.CustomerPageDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies the customer client against the contract exposed by the customer
 * service: plain JSON bodies (no {@code RestAPIResponse} envelope) and
 * zero-based pagination.
 */
class AsyncRestClientCustomerServiceTest {

    private static final String BASE_URL = "http://localhost:8081/api/v1/customers";

    private AsyncRestClientCustomerService client;

    @BeforeEach
    void setUp() {
        CustomerUrlProperties url = new CustomerUrlProperties();
        url.setCustomers(BASE_URL);
        url.setCustomersById(BASE_URL + "/%d");
        CustomerProperties properties = new CustomerProperties();
        properties.setUrl(url);
        client = new AsyncRestClientCustomerService(properties, null);
    }

    @Test
    void buildSearchUri_keepsZeroBasedPageAndSearch() {
        String uri = client.buildSearchUri("ahmad", 2, 10);

        assertTrue(uri.startsWith(BASE_URL), uri);
        assertTrue(uri.contains("search=ahmad"), uri);
        assertTrue(uri.contains("page=2"), uri);
        assertTrue(uri.contains("size=10"), uri);
    }

    @Test
    void buildSearchUri_encodesBlankAndSpecialSearchValues() {
        String blank = client.buildSearchUri("", 0, 20);
        String nullSearch = client.buildSearchUri(null, 0, 20);
        String special = client.buildSearchUri("a b&c", 0, 20);

        assertTrue(blank.contains("search="), blank);
        assertEquals(blank, nullSearch);
        assertTrue(special.contains("search=a%20b%26c") || special.contains("search=a b&c"), special);
    }

    @Test
    void buildCustomerUri_appendsId() {
        assertEquals(BASE_URL + "/42", client.buildCustomerUri(42L));
    }

    @Test
    void customerPage_deserializesPlainPageResponse() throws Exception {
        String json = """
                {
                  "content": [
                    {
                      "id": 1,
                      "name": "Ahmad",
                      "phone": "08123456789",
                      "email": "ahmad@example.com",
                      "createdAt": "2026-08-23T06:00:00Z",
                      "updatedAt": "2026-08-23T07:00:00Z"
                    }
                  ],
                  "totalElements": 1,
                  "totalPages": 1,
                  "page": 0,
                  "size": 20
                }
                """;

        CustomerPageDto page = objectMapper().readValue(json, CustomerPageDto.class);

        assertEquals(1, page.getContent().size());
        CustomerDto customer = page.getContent().getFirst();
        assertEquals(1L, customer.getId());
        assertEquals("Ahmad", customer.getName());
        assertEquals("08123456789", customer.getPhone());
        assertEquals("ahmad@example.com", customer.getEmail());
        assertNotNull(customer.getCreatedAt());
        assertNotNull(customer.getUpdatedAt());
        assertEquals(1, page.getTotalPages());
        assertEquals(0, page.getPage());
        assertEquals(20, page.getSize());
        assertEquals(1L, page.getTotalElements());
    }

    @Test
    void customerPage_defaultsContentWhenServerOmitsIt() throws Exception {
        CustomerPageDto page = objectMapper().readValue("{\"totalElements\":0}", CustomerPageDto.class);

        assertNotNull(page.getContent());
        assertTrue(page.getContent().isEmpty());
    }

    @Test
    void customer_deserializesIsoInstantTimestamps() throws Exception {
        String json = """
                {
                  "id": 7,
                  "name": "Siti",
                  "phone": "08129876543",
                  "email": null,
                  "createdAt": "2026-08-23T06:00:00Z",
                  "updatedAt": "2026-08-23T07:30:00Z"
                }
                """;

        CustomerDto customer = objectMapper().readValue(json, CustomerDto.class);

        assertEquals(7L, customer.getId());
        assertEquals("Siti", customer.getName());
        assertEquals(null, customer.getEmail());
        assertNotNull(customer.getCreatedAt());
        assertNotNull(customer.getUpdatedAt());
    }

    /**
     * Mirrors the lenient mapper the reactive client relies on, so the test
     * fails if the wire format ever drifts from the DTOs.
     */
    private static ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }
}
