package com.harmoni.menu.dashboard.layout.component;

import com.harmoni.menu.dashboard.layout.ai.AiPageContext;
import com.harmoni.menu.dashboard.service.data.rest.GrowthRestClientService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.internal.nodefeature.ElementListenerMap;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class DashboardAiPanelShortcutTest {

    @Test
    void questionKeepsKeystrokesAwayFromGlobalListShortcuts() {
        DashboardAiPanel panel = new DashboardAiPanel(mock(GrowthRestClientService.class),
                () -> new AiPageContext("Dashboard", "summary", "tool"));

        TextArea question = (TextArea) findQuestion(panel);
        assertThat(question).isNotNull();
        assertThat(question.getElement().getNode().getFeature(ElementListenerMap.class)
                .getExpressions("keydown"))
                .contains("event.stopPropagation()");
    }

    private static Component findQuestion(Component component) {
        for (Component child : component.getChildren().toList()) {
            if (child.getClassNames().contains("dashboard-ai-question")) {
                return child;
            }
            Component nested = findQuestion(child);
            if (nested != null) {
                return nested;
            }
        }
        return null;
    }
}
