package com.harmoni.menu.dashboard.event.store;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.rest.data.RestAPIResponse;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class StoreDeleteEventListener implements ComponentEventListener<ClickEvent<Button>>,
        BroadcastMessageService {

    private final transient StoreDto storeDto;
    private final RestClientOrganizationService restClientOrganizationService;

    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        setConfirmDialogDelete();
    }

    private void callRemoveAPI() {
        restClientOrganizationService.deleteStore(storeDto)
                .subscribe(this::accept);
    }

    private void setConfirmDialogDelete() {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader("Confirmation");
        confirmDialog.setText("Do you want to remove this product ".concat(storeDto.getName()).concat("?"));
        confirmDialog.setCancelable(true);
        confirmDialog.addConfirmListener(_ -> callRemoveAPI());
        confirmDialog.open();
    }

    private void accept(RestAPIResponse restAPIResponse) {
        broadcastMessage(BroadcastMessage.STORE_UPDATED_SUCCESS, restAPIResponse);
    }
}
