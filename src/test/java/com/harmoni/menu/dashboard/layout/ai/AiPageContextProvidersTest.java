package com.harmoni.menu.dashboard.layout.ai;

import com.harmoni.menu.dashboard.layout.customer.CustomerView;
import com.harmoni.menu.dashboard.layout.menu.category.CategoryView;
import com.harmoni.menu.dashboard.layout.menu.product.ProductListView;
import com.harmoni.menu.dashboard.layout.menu.promotion.PromotionListView;
import com.harmoni.menu.dashboard.layout.report.OrderVolumeReportView;
import com.harmoni.menu.dashboard.layout.report.SettlementReportView;
import com.harmoni.menu.dashboard.service.AuthorizationService;
import com.harmoni.menu.dashboard.util.Messages;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class AiPageContextProvidersTest {

    private final AuthorizationService authorizationService = mock(AuthorizationService.class);

    @Test
    void categoryWithoutSelectionDescribesEmptyState() {
        CategoryView view = new CategoryView(null, null, null, null, authorizationService);

        AiPageContext context = view.aiPageContext();

        assertThat(context.title()).isEqualTo(Messages.get("page.category.title"));
        assertThat(context.toolName()).isEqualTo(AiTools.SEARCH_CATEGORIES);
        assertThat(context.summary())
                .contains(Messages.get("ai.context.categories") + ": 0")
                .contains(Messages.get("ai.context.selected") + ": " + Messages.get("ai.context.none"));
    }

    @Test
    void customerSummaryUsesCountsAndCurrentPage() {
        CustomerView view = new CustomerView(null, null, null);

        AiPageContext context = view.aiPageContext();

        assertThat(context.title()).isEqualTo(Messages.get("page.customer.title"));
        assertThat(context.toolName()).isEqualTo(AiTools.SEARCH_CUSTOMERS);
        assertThat(context.summary())
                .contains(Messages.get("ai.context.totalCustomers") + ": 0")
                .contains(Messages.get("ai.context.currentPage") + ": 1")
                .doesNotContain("phone", "email");
    }

    @Test
    void productSummaryIncludesPagingAndEmptyFilters() {
        ProductListView view = new ProductListView(null, null, authorizationService);

        AiPageContext context = view.aiPageContext();

        assertThat(context.title()).isEqualTo(Messages.get("page.product.title"));
        assertThat(context.toolName()).isEqualTo(AiTools.SEARCH_PRODUCTS);
        assertThat(context.summary())
                .contains(Messages.get("ai.context.productsOnPage") + ": 0")
                .contains(Messages.get("ai.context.currentPage") + ": 1")
                .contains(Messages.get("ai.context.totalPages") + ": 0");
    }

    @Test
    void promotionSummaryIncludesFiltersAndPaging() {
        PromotionListView view = new PromotionListView(
                null, null, null, null, null, authorizationService);

        AiPageContext context = view.aiPageContext();

        assertThat(context.title()).isEqualTo(Messages.get("page.promotion.title"));
        assertThat(context.toolName()).isEqualTo(AiTools.LIST_PROMOTIONS);
        assertThat(context.summary())
                .contains(Messages.get("ai.context.status") + ": " + Messages.get("ai.context.all"))
                .contains(Messages.get("ai.context.promotionsOnPage") + ": 0")
                .contains(Messages.get("ai.context.currentPage") + ": 1");
    }

    @Test
    void reportSummaryExplainsScopeBeforeDataLoads() {
        SettlementReportView settlement = new SettlementReportView(null);
        OrderVolumeReportView orderVolume = new OrderVolumeReportView(null);

        assertThat(settlement.aiPageContext().summary())
                .contains(Messages.get("ai.context.storeScope") + ": " + Messages.get("ai.context.currentStore"))
                .contains(Messages.get("ai.context.noReportData"));
        assertThat(settlement.aiPageContext().toolName()).isEqualTo(AiTools.SALES_REPORT);
        assertThat(orderVolume.aiPageContext().title()).isEqualTo(Messages.get("nav.report.orderVolume"));
    }
}
