package com.harmoni.menu.dashboard.event.chain;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.layout.organization.chain.ChainForm;
import com.harmoni.menu.dashboard.rest.data.RestAPIResponse;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;

public class ChainSaveEventListener implements ComponentEventListener<ClickEvent<Button>> {
    private final ChainForm chainForm;
    private final RestClientOrganizationService restClientOrganizationService;
    public ChainSaveEventListener(ChainForm chainForm, RestClientOrganizationService restClientOrganizationService) {
        this.chainForm = chainForm;
        this.restClientOrganizationService = restClientOrganizationService;
    }
    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        if (this.chainForm.getBinder().validate().hasErrors()) {
            return;
        }
        ChainDto chainDto = this.chainForm.getChainDto();
        chainDto.setName(this.chainForm.getChainNameField().getValue());
        restClientOrganizationService.createChain(chainDto)
                .subscribe(this::accept);
    }
    private void accept(RestAPIResponse restAPIResponse) {
        try {
            Broadcaster.broadcast(ObjectUtil.objectToJsonString(BroadcastMessage.builder()
                    .type(BroadcastMessage.CATEGORY_INSERT_SUCCESS)
                    .data(restAPIResponse).build()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
}
