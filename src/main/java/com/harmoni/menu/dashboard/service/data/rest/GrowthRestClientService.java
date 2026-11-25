package com.harmoni.menu.dashboard.service.data.rest;

import com.harmoni.menu.dashboard.configuration.GrowthProperties;
import com.harmoni.menu.dashboard.dto.growth.DashboardAiAnswerDto;
import com.harmoni.menu.dashboard.dto.growth.DashboardAiRequest;
import com.harmoni.menu.dashboard.util.VaadinSessionUtil;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * Blocking REST client for the dashboard AI endpoint. Calls go through the API
 * gateway with the signed-in user's JWT.
 */
@Service
public class GrowthRestClientService {

    private final WebClient webClient;
    private final String baseUrl;

    public GrowthRestClientService(GrowthProperties properties) {
        this.baseUrl = properties.getUrl();
        this.webClient = WebClient.builder()
                .filter((request, next) -> {
                    ClientRequest.Builder builder = ClientRequest.from(request);
                    String token = VaadinSessionUtil.getAttribute(VaadinSessionUtil.JWT_TOKEN, String.class);
                    if (token != null && !token.isBlank()) {
                        builder.headers(headers -> headers.setBearerAuth(token));
                    }
                    return next.exchange(builder.build());
                })
                .build();
    }

    public DashboardAiAnswerDto askDashboardAi(String message, String page, String tool, String summary, String token) {
        WebClient.RequestBodySpec spec = webClient.post()
                .uri(baseUrl + "/api/v1/ai/dashboard/summary-chat")
                .contentType(MediaType.APPLICATION_JSON);
        if (token != null && !token.isBlank()) {
            spec.header(HttpHeaders.AUTHORIZATION, "Bearer " + token);
        }
        return spec.bodyValue(new DashboardAiRequest(message, page, tool, summary))
                .retrieve().bodyToMono(DashboardAiAnswerDto.class).block();
    }
}
