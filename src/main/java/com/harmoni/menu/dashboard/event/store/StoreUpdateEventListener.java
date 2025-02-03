package com.harmoni.menu.dashboard.event.store;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.layout.organization.store.StoreForm;
import com.harmoni.menu.dashboard.rest.data.RestAPIResponse;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import lombok.AllArgsConstructor;

@AllArgsConstructor
public class StoreUpdateEventListener implements ComponentEventListener<ClickEvent<Button>>,
        BroadcastMessageService {

    private final transient StoreForm storeForm;
    private final RestClientOrganizationService restClientOrganizationService;

    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        restClientOrganizationService.updateStore(storeForm.getStoreDto())
                .subscribe(this::accept);
    }

    private void accept(RestAPIResponse restAPIResponse) {
        broadcastMessage(BroadcastMessage.STORE_UPDATED_SUCCESS, restAPIResponse);
    }

}
