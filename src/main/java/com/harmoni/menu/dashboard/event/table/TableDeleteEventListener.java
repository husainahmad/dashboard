package com.harmoni.menu.dashboard.event.table;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.TableDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientSettingService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class TableDeleteEventListener implements ComponentEventListener<ClickEvent<Button>>,
        BroadcastMessageService {

    private final transient TableDto tableDto;
    private final RestClientSettingService restClientSettingService;
    private final UI ui;

    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        setConfirmDialogDelete();
    }

    private void setConfirmDialogDelete() {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader("Confirmation");
        confirmDialog.setText("Do you want to remove this table ".concat(tableDto.getName()).concat("?"));
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
            UiUtil.success("Table deleted..");
            broadcastMessage(BroadcastMessage.TABLE_DELETED_SUCCESS, restAPIResponse);
        });
    }
}
