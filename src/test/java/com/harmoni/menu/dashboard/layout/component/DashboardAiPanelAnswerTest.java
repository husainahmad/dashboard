package com.harmoni.menu.dashboard.layout.component;

import com.harmoni.menu.dashboard.layout.ai.AiPageContext;
import com.harmoni.menu.dashboard.service.data.rest.GrowthRestClientService;
import com.vaadin.flow.component.Component;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DashboardAiPanelAnswerTest {

    @Test
    void answerIsRenderedAsMarkdown() {
        DashboardAiPanel panel = new DashboardAiPanel(mock(GrowthRestClientService.class),
                () -> new AiPageContext("Dashboard", "summary", "tool"));

        ReflectionTestUtils.invokeMethod(panel, "showAnswer", "**Total**\n- one\n- two");

        Component body = findBody(panel);
        assertThat(body).isInstanceOf(MarkdownText.class);
        assertThat(body.getElement().getProperty("innerHTML", ""))
                .contains("<strong>Total</strong>", "<li>one</li>", "<li>two</li>");
    }

    @Test
    void hintsRemainPlainText() {
        DashboardAiPanel panel = new DashboardAiPanel(mock(GrowthRestClientService.class), () -> null);

        Component body = findBody(panel);

        assertThat(body).isNotInstanceOf(MarkdownText.class);
        assertThat(body.getElement().getProperty("innerHTML", "")).isEmpty();
    }

    private static Component findBody(Component component) {
        for (Component child : component.getChildren().toList()) {
            if (child.getClassNames().contains("dashboard-ai-message-text")) {
                return child;
            }
            Component nested = findBody(child);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }
}
