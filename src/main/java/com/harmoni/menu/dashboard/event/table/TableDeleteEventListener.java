package com.harmoni.menu.dashboard.event.table;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.TableDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientSettingService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import lombok.RequiredArgsConstructor;

/**
 * Called when the user clicks Delete on the table form: shows a confirmation
 * dialog and, once confirmed, removes the table via the REST API, shows a
 * success toast and broadcasts the result.
 */
@RequiredArgsConstructor
public class TableDeleteEventListener implements ComponentEventListener<ClickEvent<Button>>,
        BroadcastMessageService {

    private final transient TableDto tableDto;
    private final RestClientSettingService restClientSettingService;
    private final UI ui;

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
        confirmDialog.setText(Messages.get("dialog.confirmDeleteTable", tableDto.getName()));
        confirmDialog.setCancelable(true);
        confirmDialog.addConfirmListener(event -> callRemoveAPI());
        confirmDialog.open();
    }

    private void callRemoveAPI() {
        restClientSettingService.deleteTable(tableDto)
                .subscribe(this::accept);
    }

    private void accept(RestAPIResponse restAPIResponse) {
        ui.access(() -> {
            UiUtil.success(Messages.get("notification.table.deleted"));
            broadcastMessage(BroadcastMessage.TABLE_DELETED_SUCCESS, restAPIResponse);
        });
    }
}
