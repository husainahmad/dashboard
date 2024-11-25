package com.harmoni.menu.dashboard.event.tier;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.exception.BrandHandler;
import com.harmoni.menu.dashboard.layout.organization.tier.TierPriceForm;
import com.harmoni.menu.dashboard.rest.data.RestAPIResponse;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;

public class TierDeleteEventListener implements ComponentEventListener<ClickEvent<Button>>,
        BroadcastMessageService {

    private final TierPriceForm tierForm;

    private final RestClientOrganizationService restClientOrganizationService;

    public TierDeleteEventListener(TierPriceForm tierForm, RestClientOrganizationService restClientOrganizationService) {
        this.tierForm = tierForm;
        this.restClientOrganizationService = restClientOrganizationService;
    }

    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        if (this.tierForm.getBinder().validate().hasErrors()) {
            return;
        }
        TierDto tierDto = new TierDto();
        tierDto.setId(this.tierForm.getTierDto().getId());
        tierDto.setType(this.tierForm.getTierDto().getType());
        tierDto.setName(this.tierForm.getTierNameField().getValue());
        tierDto.setBrandId(this.tierForm.getBrandBox().getValue().getId());
        restClientOrganizationService.deleteTier(tierDto)
                .doOnError(error -> new BrandHandler(this.tierForm.getUi(),
                        "Error while deleting Tier ".concat(error.getMessage())))
                .subscribe(this::accept);
    }

    private void accept(RestAPIResponse restAPIResponse) {
        broadcastMessage(BroadcastMessage.TIER_UPDATED_SUCCESS, restAPIResponse);
    }
}
