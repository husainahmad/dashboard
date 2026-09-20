package com.harmoni.menu.dashboard.event.table;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.TableDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.layout.setting.table.TableForm;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientSettingService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import lombok.RequiredArgsConstructor;

/**
 * Called when the user clicks Save on the table form: validates the binder,
 * posts the new table and broadcasts the result.
 */
@RequiredArgsConstructor
public class TableSaveEventListener implements ComponentEventListener<ClickEvent<Button>>,
        BroadcastMessageService {

    private final TableForm tableForm;
    private final RestClientSettingService restClientSettingService;

    /**
     * Validates the table form and, if valid, posts the new table.
     *
     * @param buttonClickEvent the click event that triggered the listener
     */
    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        if (this.tableForm.getBinder().validate().hasErrors()) {
            return;
        }
        TableDto tableDto = this.tableForm.getTableDto();
        tableDto.setName(this.tableForm.getNameField().getValue());
        tableDto.setCapacity(this.tableForm.getCapacityField().getValue());
        restClientSettingService.createTable(tableDto)
                .subscribe(this::accept);
    }

    private void accept(RestAPIResponse restAPIResponse) {
        broadcastMessage(BroadcastMessage.TABLE_INSERT_SUCCESS, restAPIResponse);
    }
}
