package com.harmoni.menu.dashboard.configuration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class OrderPropertiesBindingTest {

    @Autowired
    private OrderProperties orderProperties;

    @Test
    void orderPageUrl_isBoundToTheGateway() {
        assertNotNull(orderProperties.getUrl(), "order.url should be bound");
        assertEquals("http://localhost:8080/api/v1/order/page", orderProperties.getUrl().getPage());
    }
}
