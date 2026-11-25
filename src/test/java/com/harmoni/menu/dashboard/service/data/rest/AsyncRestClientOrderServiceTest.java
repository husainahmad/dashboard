package com.harmoni.menu.dashboard.service.data.rest;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.harmoni.menu.dashboard.configuration.OrderProperties;
import com.harmoni.menu.dashboard.configuration.OrderUrlProperties;
import com.harmoni.menu.dashboard.dto.order.OrderPageDto;
import com.harmoni.menu.dashboard.dto.order.RecentOrderDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AsyncRestClientOrderServiceTest {

    private static final String BASE_URL = "http://localhost:8083/api/v1/order/page";

    private AsyncRestClientOrderService client;

    @BeforeEach
    void setUp() {
        OrderUrlProperties urls = new OrderUrlProperties();
        urls.setPage(BASE_URL);
        OrderProperties properties = new OrderProperties();
        properties.setUrl(urls);
        client = new AsyncRestClientOrderService(properties, null);
    }

    @Test
    void buildOrderPageUri_usesDefaultLimit() {
        String uri = client.buildOrderPageUri(null, null);

        assertTrue(uri.startsWith(BASE_URL), uri);
        assertTrue(uri.contains("limit=10"), uri);
        assertFalse(uri.contains("beforeId="), uri);
    }

    @Test
    void buildOrderPageUri_appendsCursorWhenPresent() {
        String uri = client.buildOrderPageUri(25, 99);

        assertTrue(uri.contains("limit=25"), uri);
        assertTrue(uri.contains("beforeId=99"), uri);
    }

    @Test
    void orderPage_deserializesSafeOrderFields() throws Exception {
        String json = """
                {
                  "httpStatus": 200,
                  "data": {
                    "orders": [
                      {
                        "id": 7,
                        "orderNo": "ORD-7",
                        "grandTotal": 150000.00,
                        "status": "PAID",
                        "tableNumber": "A2",
                        "createdAt": "2026-11-23T10:15:00Z",
                        "customerName": "Sensitive",
                        "customerPhone": "0812",
                        "remark": "private",
                        "idempotencyKey": "secret",
                        "orderDetails": [
                          { "productName": "Nasi Goreng", "amount": 150000.00 }
                        ]
                      }
                    ],
                    "hasMore": false
                  }
                }
                """;

        OrderPageDto page = objectMapper().convertValue(envelopeData(json), OrderPageDto.class);

        assertNotNull(page.getOrders());
        assertEquals(1, page.getOrders().size());
        RecentOrderDto order = page.getOrders().getFirst();
        assertEquals(7, order.getId());
        assertEquals("ORD-7", order.getOrderNo());
        assertEquals("PAID", order.getStatus());
        assertEquals("A2", order.getTableNumber());
        assertEquals(1, order.getOrderDetails().size());
        assertEquals(0, new BigDecimal("150000.00").compareTo(order.getGrandTotal()));
    }

    @Test
    void recentOrderDto_hasNoPiiAccessors() {
        assertThrows(NoSuchMethodException.class, () -> RecentOrderDto.class.getMethod("getCustomerName"));
        assertThrows(NoSuchMethodException.class, () -> RecentOrderDto.class.getMethod("getCustomerPhone"));
        assertThrows(NoSuchMethodException.class, () -> RecentOrderDto.class.getMethod("getRemark"));
    }

    private static ObjectMapper objectMapper() {
        return new ObjectMapper()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    }

    private static Object envelopeData(String envelope) throws Exception {
        return objectMapper().readValue(envelope, Map.class).get("data");
    }
}
