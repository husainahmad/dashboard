package com.harmoni.menu.dashboard.event.tier;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.exception.BrandHandler;
import com.harmoni.menu.dashboard.layout.organization.tier.service.TierServiceListView;
import com.harmoni.menu.dashboard.rest.data.RestAPIResponse;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;

public class TierServiceDeleteEventListener implements ComponentEventListener<ClickEvent<Button>>,
        BroadcastMessageService {

    private final Integer id;
    private final RestClientOrganizationService restClientOrganizationService;
    private final TierServiceListView layout;

    public TierServiceDeleteEventListener(TierServiceListView layout, Integer id,
                                          RestClientOrganizationService restClientOrganizationService) {
        this.layout = layout;
        this.id = id;
        this.restClientOrganizationService = restClientOrganizationService;
    }

    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        TierDto tierDto = new TierDto();
        tierDto.setId(this.id);
        restClientOrganizationService.deleteTier(tierDto)
                .doOnError(error -> new BrandHandler(this.layout.getUi(),
                        "Error while deleting Tier ".concat(error.getMessage())))
                .subscribe(this::accept);
    }

    private void accept(RestAPIResponse restAPIResponse) {
        broadcastMessage(BroadcastMessage.TIER_UPDATED_SUCCESS, restAPIResponse);
    }
}
