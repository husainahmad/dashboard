package com.harmoni.menu.dashboard.event.tier;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.exception.BrandHandler;
import com.harmoni.menu.dashboard.layout.organization.tier.TierForm;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import lombok.RequiredArgsConstructor;

/**
 * Called when the user clicks Save on the tier form: validates the binder,
 * posts the new tier and broadcasts the result.
 */
@RequiredArgsConstructor
public class TierSaveEventListener implements ComponentEventListener<ClickEvent<Button>>,
        BroadcastMessageService {

    private final TierForm tierForm;
    private final RestClientOrganizationService restClientOrganizationService;

    /**
     * Validates the tier form and, if valid, posts the new tier.
     *
     * @param buttonClickEvent the click event that triggered the listener
     */
    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        if (this.tierForm.getBinder().validate().hasErrors()) {
            return;
        }
        TierDto tierDto = new TierDto();
        tierDto.setType(this.tierForm.getTierDto().getType());
        tierDto.setName(this.tierForm.getTierNameField().getValue());
        tierDto.setBrandId(this.tierForm.getBrandBox().getValue().getId());
        restClientOrganizationService.createTier(tierDto)
                .doOnError(error -> new BrandHandler(this.tierForm.getUi(),
                        Messages.get(Messages.Keys.NOTIFICATION_BRAND_INSERT_ERROR, error.getMessage())))
                .subscribe(this::accept);
    }

    private void accept(RestAPIResponse restAPIResponse) {
        broadcastMessage(BroadcastMessage.TIER_INSERT_SUCCESS, restAPIResponse);
    }
}
