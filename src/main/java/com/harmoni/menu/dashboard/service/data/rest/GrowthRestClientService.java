package com.harmoni.menu.dashboard.service.data.rest;

import com.harmoni.menu.dashboard.configuration.GrowthProperties;
import com.harmoni.menu.dashboard.dto.growth.CampaignDto;
import com.harmoni.menu.dashboard.dto.growth.ChatMessageDto;
import com.harmoni.menu.dashboard.dto.growth.ChatRequest;
import com.harmoni.menu.dashboard.dto.growth.CreateCampaignRequest;
import com.harmoni.menu.dashboard.dto.growth.CustomerMetricDto;
import com.harmoni.menu.dashboard.dto.growth.InsightDto;
import com.harmoni.menu.dashboard.dto.growth.IssueVoucherRequest;
import com.harmoni.menu.dashboard.dto.growth.LoyaltyAccountDto;
import com.harmoni.menu.dashboard.dto.growth.LoyaltyTransactionDto;
import com.harmoni.menu.dashboard.dto.growth.RedeemPointsRequest;
import com.harmoni.menu.dashboard.dto.growth.RedeemVoucherRequest;
import com.harmoni.menu.dashboard.dto.growth.RecommendationDto;
import com.harmoni.menu.dashboard.dto.growth.SalesSummaryDto;
import com.harmoni.menu.dashboard.dto.growth.VoucherDto;
import com.harmoni.menu.dashboard.util.VaadinSessionUtil;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

/**
 * Blocking REST client for the customer-growth and AI endpoints. All calls go
 * through the API gateway with the signed-in user's JWT.
 */
@Service
public class GrowthRestClientService {

    private static final ParameterizedTypeReference<List<LoyaltyTransactionDto>> LOYALTY_HISTORY =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<VoucherDto>> VOUCHERS =
            new ParameterizedTypeReference<>() {
            };
    private static final ParameterizedTypeReference<List<InsightDto>> INSIGHTS =
            new ParameterizedTypeReference<>() {
            };

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

    public LoyaltyAccountDto loyaltyAccount(long customerId) {
        return get("/api/v1/growth/loyalty/" + customerId, LoyaltyAccountDto.class);
    }

    public List<LoyaltyTransactionDto> loyaltyHistory(long customerId) {
        return get("/api/v1/growth/loyalty/" + customerId + "/history", LOYALTY_HISTORY);
    }

    public LoyaltyAccountDto redeemPoints(long customerId, int points) {
        return post("/api/v1/growth/loyalty/" + customerId + "/redeem",
                new RedeemPointsRequest(points), LoyaltyAccountDto.class);
    }

    public CampaignDto createCampaign(CreateCampaignRequest request) {
        return post("/api/v1/campaigns", request, CampaignDto.class);
    }

    public CampaignDto activateCampaign(long campaignId) {
        return post("/api/v1/campaigns/" + campaignId + "/activate", null, CampaignDto.class);
    }

    public VoucherDto issueVoucher(long campaignId, long customerId) {
        return post("/api/v1/campaigns/" + campaignId + "/vouchers",
                new IssueVoucherRequest(customerId), VoucherDto.class);
    }

    public List<VoucherDto> vouchers(long customerId) {
        return get("/api/v1/campaigns/vouchers?customerId=" + customerId, VOUCHERS);
    }

    public VoucherDto redeemVoucher(String code, int orderId) {
        return post("/api/v1/campaigns/vouchers/" + code + "/redeem",
                new RedeemVoucherRequest(orderId), VoucherDto.class);
    }

    public SalesSummaryDto sales() {
        return get("/api/v1/analytics/sales", SalesSummaryDto.class);
    }

    public CustomerMetricDto metrics(long customerId) {
        return get("/api/v1/growth/customers/" + customerId + "/metrics", CustomerMetricDto.class);
    }

    public List<InsightDto> insights(long customerId) {
        return get("/api/v1/analytics/customers/" + customerId + "/insights", INSIGHTS);
    }

    public List<InsightDto> refreshInsights(long customerId) {
        return post("/api/v1/analytics/customers/" + customerId + "/insights/refresh", null, INSIGHTS);
    }

    public RecommendationDto recommendation(long customerId) {
        return get("/api/v1/ai/recommendations/" + customerId, RecommendationDto.class);
    }

    public ChatMessageDto chat(long sessionId, String message) {
        return post("/api/v1/customer-sessions/" + sessionId + "/chat",
                new ChatRequest(message), ChatMessageDto.class);
    }

    private <T> T get(String path, Class<T> type) {
        return webClient.get().uri(baseUrl + path).retrieve().bodyToMono(type).block();
    }

    private <T> List<T> get(String path, ParameterizedTypeReference<List<T>> type) {
        return webClient.get().uri(baseUrl + path).retrieve().bodyToMono(type).block();
    }

    private <T> T post(String path, Object body, Class<T> type) {
        WebClient.RequestBodySpec spec = webClient.post().uri(baseUrl + path)
                .contentType(MediaType.APPLICATION_JSON);
        return (body == null ? spec : spec.bodyValue(body))
                .retrieve().bodyToMono(type).block();
    }

    private <T> List<T> post(String path, Object body, ParameterizedTypeReference<List<T>> type) {
        WebClient.RequestBodySpec spec = webClient.post().uri(baseUrl + path)
                .contentType(MediaType.APPLICATION_JSON);
        return (body == null ? spec : spec.bodyValue(body))
                .retrieve().bodyToMono(type).block();
    }
}
