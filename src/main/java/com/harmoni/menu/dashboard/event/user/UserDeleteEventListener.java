package com.harmoni.menu.dashboard.event.user;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.UserDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import lombok.AllArgsConstructor;

/**
 * Called when the user clicks Delete on the user form: shows a confirmation
 * dialog and, once confirmed, removes the user via the REST API and broadcasts
 * the result so every open grid refreshes.
 */
@AllArgsConstructor
public class UserDeleteEventListener implements ComponentEventListener<ClickEvent<Button>>,
        BroadcastMessageService {

    private final transient UserDto userDto;
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

    private void callRemoveAPI() {
        restClientOrganizationService.deleteUser(userDto)
                .subscribe(this::accept);
    }

    private void setConfirmDialogDelete() {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader(Messages.get(Messages.Keys.DIALOG_CONFIRM_TITLE));
        confirmDialog.setText(Messages.get("dialog.confirmDeleteUser", userDto.getUsername()));
        confirmDialog.setCancelable(true);
        confirmDialog.addConfirmListener(event -> callRemoveAPI());
        confirmDialog.open();
    }

    private void accept(RestAPIResponse restAPIResponse) {
        broadcastMessage(BroadcastMessage.STORE_UPDATED_SUCCESS, restAPIResponse);
    }
}
