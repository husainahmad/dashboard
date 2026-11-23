package com.harmoni.menu.dashboard.layout.crm;

import com.harmoni.menu.dashboard.dto.growth.ChatMessageDto;
import com.harmoni.menu.dashboard.layout.component.PageHeader;
import com.harmoni.menu.dashboard.service.data.rest.GrowthRestClientService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;

public class AiSalesAssistantView extends AbstractGrowthView {

    private final GrowthRestClientService client;
    private final IntegerField sessionId = new IntegerField(Messages.get("growth.sessionId"));
    private final TextArea message = new TextArea(Messages.get("growth.message"));
    private final Div reply = new Div();

    public AiSalesAssistantView(GrowthRestClientService client) {
        super("ai-sales-assistant-view");
        this.client = client;
        sessionId.setValue(1);
        message.setValue("Halo, saya ingin kopi susu");
        message.setWidthFull();
        reply.addClassName("growth-result");

        render(PageHeader.of("page.aiSalesAssistant.title", "page.aiSalesAssistant.description"),
                toolbar(sessionId, button("growth.send", VaadinIcon.PAPERPLANE, event -> send())),
                message,
                reply);
    }

    private void send() {
        try {
            ChatMessageDto answer = client.chat(sessionId.getValue(), message.getValue());
            reply.setText(answer.message());
        } catch (Exception e) {
            actionError(this::send);
        }
    }
}
