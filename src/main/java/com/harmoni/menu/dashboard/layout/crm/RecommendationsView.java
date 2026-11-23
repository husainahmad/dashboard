package com.harmoni.menu.dashboard.layout.crm;

import com.harmoni.menu.dashboard.dto.growth.RecommendationDto;
import com.harmoni.menu.dashboard.layout.component.PageHeader;
import com.harmoni.menu.dashboard.service.data.rest.GrowthRestClientService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.IntegerField;

public class RecommendationsView extends AbstractGrowthView {

    private final GrowthRestClientService client;
    private final IntegerField customerId = new IntegerField(Messages.get("growth.customerId"));
    private final Div result = new Div();

    public RecommendationsView(GrowthRestClientService client) {
        super("recommendations-view");
        this.client = client;
        customerId.setValue(1);
        result.addClassName("growth-result");

        render(PageHeader.of("page.recommendations.title", "page.recommendations.description"),
                toolbar(customerId,
                        button("growth.generate", VaadinIcon.LIGHTBULB, event -> generate())),
                result);
    }

    private void generate() {
        try {
            RecommendationDto recommendation = client.recommendation(customerId.getValue());
            result.setText(recommendation.recommendation());
        } catch (Exception e) {
            actionError(this::generate);
        }
    }
}
