package com.harmoni.menu.dashboard.layout.component;

import com.harmoni.menu.dashboard.layout.ai.AiPageContext;
import com.harmoni.menu.dashboard.service.data.rest.GrowthRestClientService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Span;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DashboardAiPanelContextTest {

    @Test
    void navigationChangesTheContextLabel() {
        DashboardAiPanel panel = new DashboardAiPanel(mock(GrowthRestClientService.class),
                () -> new AiPageContext("Dashboard", "Dashboard summary", "pos_get_sales_report"));

        panel.onPageContextChanged(new AiPageContext("Category", "Category summary",
                "pos_search_categories"));

        assertThat(contextLabel(panel).getText())
                .isEqualTo(Messages.get("dashboard.ai.context", "Category"));
    }

    @Test
    void missingContextFallsBackToUnknownPage() {
        DashboardAiPanel panel = new DashboardAiPanel(mock(GrowthRestClientService.class), () -> null);

        panel.onPageContextChanged(null);

        assertThat(contextLabel(panel).getText())
                .isEqualTo(Messages.get("dashboard.ai.context", Messages.get("dashboard.ai.unknownPage")));
    }

    private static Span contextLabel(Component component) {
        for (Component child : component.getChildren().toList()) {
            if (child instanceof Span span && span.getClassNames().contains("dashboard-ai-context")) {
                return span;
            }
            Span nested = contextLabel(child);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }
}
