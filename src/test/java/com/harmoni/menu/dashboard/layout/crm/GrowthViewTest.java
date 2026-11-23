package com.harmoni.menu.dashboard.layout.crm;

import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.service.data.rest.GrowthRestClientService;
import com.harmoni.menu.dashboard.support.UiTestSupport;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

class GrowthViewTest {

    private final GrowthRestClientService client = mock(GrowthRestClientService.class);

    @Test
    void allGrowthViewsFollowTheAdminPageShell() {
        assertShell(new LoyaltyView(client));
        assertShell(new CampaignsView(client));
        assertShell(new CustomerAnalyticsView(client));
        assertShell(new AiSalesAssistantView(client));
        assertShell(new RecommendationsView(client));
    }

    private static void assertShell(AbstractGrowthView view) {
        assertTrue(view.hasClassName(Css.LIST_VIEW));
        assertFalse(view.getChildren().findAny().isEmpty());
        assertTrue(UiTestSupport.descendants(view).stream()
                .anyMatch(component -> component.hasClassName("page-header")));
        assertTrue(UiTestSupport.descendants(view).stream()
                .anyMatch(component -> component.hasClassName(Css.TOOLBAR)));
    }
}
