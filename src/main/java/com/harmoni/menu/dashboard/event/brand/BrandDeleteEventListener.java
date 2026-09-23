package com.harmoni.menu.dashboard.event.brand;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Called when the user clicks Delete on the brand form: shows a confirmation
 * dialog and, once confirmed, removes the brand via the REST API and broadcasts
 * the result so every open grid refreshes.
 */
@RequiredArgsConstructor
@Slf4j
public class BrandDeleteEventListener implements ComponentEventListener<ClickEvent<Button>>, BroadcastMessageService {

    private final transient BrandDto brandDto;
    private final RestClientOrganizationService restClientOrganizationService;

    /**
     * Opens the delete confirmation dialog when the delete button is clicked.
     *
     * @param buttonClickEvent the click event that triggered the listener
     */
    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        setConfirmDialogDelete();
    }

    private void setConfirmDialogDelete() {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader(Messages.get(Messages.Keys.DIALOG_CONFIRM_TITLE));
        confirmDialog.setText(Messages.get("dialog.confirmDeleteBrand", brandDto.getName()));
        confirmDialog.setCancelable(true);
        confirmDialog.addConfirmListener(event -> callRemoveAPI());
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
