package com.harmoni.menu.dashboard.service.data.rest;

import com.fasterxml.jackson.core.type.TypeReference;
import com.harmoni.menu.dashboard.configuration.OrderProperties;
import com.harmoni.menu.dashboard.dto.order.OrderPageDto;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class AsyncRestClientOrderService extends AsyncRestClientBase {

    private static final int DEFAULT_RECENT_ORDER_LIMIT = 10;

    private final transient OrderProperties orderProperties;

    public AsyncRestClientOrderService(OrderProperties orderProperties,
                                       TokenRefreshService tokenRefreshService) {
        super(tokenRefreshService);
        this.orderProperties = orderProperties;
    }

    public void getRecentOrdersAsync(AsyncRestCallback<OrderPageDto> callback,
                                     AsyncRestCallback<Throwable> errorCallback,
                                     Integer limit, Integer beforeId) {
        makeAsyncRequest(buildOrderPageUri(limit, beforeId),
                new TypeReference<OrderPageDto>() {
                }, callback, errorCallback);
    }

    String buildOrderPageUri(Integer limit, Integer beforeId) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(orderProperties.getUrl().getPage());
        builder.queryParam("limit", limit == null || limit <= 0 ? DEFAULT_RECENT_ORDER_LIMIT : limit);
        if (beforeId != null) {
            builder.queryParam("beforeId", beforeId);
        }
        return builder.toUriString();
    }
}
