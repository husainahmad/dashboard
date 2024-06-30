package com.harmoni.menu.dashboard.event.tier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.exception.BrandHandler;
import com.harmoni.menu.dashboard.layout.organization.tier.TierForm;
import com.harmoni.menu.dashboard.rest.data.RestAPIResponse;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;

public class TierSaveEventListener implements ComponentEventListener<ClickEvent<Button>> {

    private final TierForm tierForm;

    private final RestClientOrganizationService restClientOrganizationService;

    public TierSaveEventListener(TierForm tierForm, RestClientOrganizationService restClientOrganizationService) {
        this.tierForm = tierForm;
        this.restClientOrganizationService = restClientOrganizationService;
    }
    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        if (this.tierForm.getBinder().validate().hasErrors()) {
            return;
        }
        TierDto tierDto = new TierDto();
        tierDto.setName(this.tierForm.getTierNameField().getValue());
        tierDto .setBrandId(this.tierForm.getBrandBox().getValue().getId());
        restClientOrganizationService.createTier(tierDto)
                .doOnError(error -> new BrandHandler(this.tierForm.getUi(), "Error while inserting Brand ".concat(error.getMessage())))
                .subscribe(this::accept);
    }

    private void accept(RestAPIResponse restAPIResponse) {
        try {
            Broadcaster.broadcast(ObjectUtil.objectToJsonString(BroadcastMessage.builder()
                    .type(BroadcastMessage.TIER_INSERT_SUCCESS)
                    .data(restAPIResponse).build()));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
