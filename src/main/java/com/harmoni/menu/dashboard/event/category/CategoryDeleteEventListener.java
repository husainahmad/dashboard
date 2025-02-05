package com.harmoni.menu.dashboard.event.category;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.rest.data.RestAPIResponse;
import com.harmoni.menu.dashboard.rest.data.RestClientMenuService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class CategoryDeleteEventListener implements ComponentEventListener<ClickEvent<Button>>,
        BroadcastMessageService {

    private final transient CategoryDto categoryDto;
    private final RestClientMenuService restClientMenuService;

    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        setConfirmDialogDelete();
    }

    private void callRemoveAPI() {
        restClientMenuService.deleteCategory(categoryDto)
                .subscribe(this::accept);
    }

    private void setConfirmDialogDelete() {
        ConfirmDialog confirmDialog = new ConfirmDialog();
        confirmDialog.setHeader("Confirmation");
        confirmDialog.setText("Do you want to remove this category ".concat(categoryDto.getName()).concat("?"));
        confirmDialog.setCancelable(true);
        confirmDialog.addConfirmListener(_ -> callRemoveAPI());
        confirmDialog.open();
    }

    private void accept(RestAPIResponse restAPIResponse) {
        broadcastMessage(BroadcastMessage.CATEGORY_UPDATED_SUCCESS, restAPIResponse);
    }

}
