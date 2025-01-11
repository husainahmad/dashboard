package com.harmoni.menu.dashboard.event.tier;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.exception.BrandHandler;
import com.harmoni.menu.dashboard.rest.data.RestAPIResponse;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TierDeleteEventListener implements ComponentEventListener<ClickEvent<Button>>,
        BroadcastMessageService {

    private final UI ui;
    private final Integer id;
    private final RestClientOrganizationService restClientOrganizationService;

    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        setConfirmDialogDelete();
    }

    private void executeDelete() {
        TierDto tierDto = new TierDto();
        tierDto.setId(this.id);
        restClientOrganizationService.deleteTier(tierDto)
                .doOnError(error -> new BrandHandler(this.ui,
                        "Error while deleting Tier ".concat(error.getMessage())))
                .subscribe(this::accept);
    }

    private void setConfirmDialogDelete() {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader("Confirmation");
        confirmDialog.setText("Do you want to remove this Tier ".concat("?"));
        confirmDialog.setCancelable(true);
        confirmDialog.addConfirmListener(_ -> executeDelete());
        confirmDialog.open();
    }

    private void accept(RestAPIResponse restAPIResponse) {
        broadcastMessage(BroadcastMessage.TIER_DELETED_SUCCESS, restAPIResponse);
    }
}
