package com.harmoni.menu.dashboard.event.tier;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.exception.BrandHandler;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import lombok.RequiredArgsConstructor;

import static com.harmoni.menu.dashboard.component.BroadcastMessage.TIER_DELETED_SUCCESS;

@RequiredArgsConstructor
public class TierServiceDeleteEventListener implements ComponentEventListener<ClickEvent<Button>>,
        BroadcastMessageService {

    private final Integer id;
    private final RestClientOrganizationService restClientOrganizationService;
    private final UI ui;

    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        ui.access(() -> {
            ConfirmDialog confirmDialog = new ConfirmDialog();
            confirmDialog.setHeader("Confirmation");
            confirmDialog.setText("Do you want to remove this tier service?");
            confirmDialog.setCancelable(true);
            confirmDialog.addConfirmListener(event -> deleteTierService());
            confirmDialog.open();
        });
    }

    private void deleteTierService() {
        TierDto tierDto = new TierDto();
        tierDto.setId(this.id);
        restClientOrganizationService.deleteTier(tierDto)
                .doOnError(error -> new BrandHandler(this.ui,
                        "Error while deleting Tier ".concat(error.getMessage())))
                .subscribe(this::accept);
    }

    private void accept(RestAPIResponse restAPIResponse) {
        broadcastMessage(TIER_DELETED_SUCCESS, restAPIResponse);
    }
}
