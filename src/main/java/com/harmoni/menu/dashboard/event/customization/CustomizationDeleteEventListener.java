package com.harmoni.menu.dashboard.event.customization;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.CustomizationDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.exception.BusinessBadRequestException;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Called when the user clicks Delete on the customization form: shows a
 * confirmation dialog and, once confirmed, removes the customization via the
 * REST API and broadcasts the result so every open grid refreshes.
 */
@RequiredArgsConstructor
@Slf4j
public class CustomizationDeleteEventListener
        implements ComponentEventListener<ClickEvent<Button>>, BroadcastMessageService {

    private final CustomizationDto customizationDto;
    private final RestClientMenuService restClientMenuService;

    /**
     * Opens the delete confirmation dialog when the delete button is clicked.
     *
     * @param buttonClickEvent the click event that triggered the listener
     */
    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader("Confirmation");
        confirmDialog.setText("Do you want to remove this customization " + customizationDto.getName() + "?");
        confirmDialog.setCancelable(true);
        confirmDialog.addConfirmListener(event -> executeDelete());
        confirmDialog.open();
    }

    private void executeDelete() {
        restClientMenuService.deleteCustomization(customizationDto)
                .subscribe(this::accept, this::onError);
    }

    private void accept(RestAPIResponse response) {
        broadcastMessage(BroadcastMessage.CUSTOMIZATION_DELETE_SUCCESS, response);
    }

    private void onError(Throwable error) {
        log.error("Delete customization failed", error);
        if (!(error instanceof BusinessBadRequestException)) {
            UiUtil.error("Unable to delete customization");
        }
    }
}