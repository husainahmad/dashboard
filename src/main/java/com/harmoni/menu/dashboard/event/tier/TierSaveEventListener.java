package com.harmoni.menu.dashboard.event.tier;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.exception.BrandHandler;
import com.harmoni.menu.dashboard.layout.organization.tier.TierPriceForm;
import com.harmoni.menu.dashboard.rest.data.RestAPIResponse;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;

public class TierSaveEventListener implements ComponentEventListener<ClickEvent<Button>>,
        BroadcastMessageService {

    private final TierPriceForm tierForm;

    private final RestClientOrganizationService restClientOrganizationService;

    public TierSaveEventListener(TierPriceForm tierForm, RestClientOrganizationService restClientOrganizationService) {
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
                .doOnError(error -> new BrandHandler(this.tierForm.getUi(),
                        "Error while inserting Brand ".concat(error.getMessage())))
                .subscribe(this::accept);
    }

    private void accept(RestAPIResponse restAPIResponse) {
        broadcastMessage(BroadcastMessage.TIER_INSERT_SUCCESS, restAPIResponse);
    }
}
