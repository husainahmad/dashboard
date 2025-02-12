package com.harmoni.menu.dashboard.event.brand;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class BrandDeleteEventListener implements ComponentEventListener<ClickEvent<Button>>, BroadcastMessageService {

    private final transient BrandDto brandDto;
    private final RestClientOrganizationService restClientOrganizationService;

    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        setConfirmDialogDelete();
    }

    private void setConfirmDialogDelete() {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader("Confirmation");
        confirmDialog.setText("Do you want to remove this brand ".concat(brandDto.getName()).concat("?"));
        confirmDialog.setCancelable(true);
        confirmDialog.addConfirmListener(_ -> callRemoveAPI());
        confirmDialog.open();
    }

    private void callRemoveAPI() {
        restClientOrganizationService.deleteBrand(brandDto)
                .subscribe(this::accept);
    }


    private void accept(RestAPIResponse restAPIResponse) {
        broadcastMessage(BroadcastMessage.BRAND_SUCCESS_UPDATED, restAPIResponse);
    }
}
