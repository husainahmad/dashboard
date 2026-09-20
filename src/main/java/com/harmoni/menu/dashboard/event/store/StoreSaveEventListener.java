package com.harmoni.menu.dashboard.event.store;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.layout.organization.store.StoreForm;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import lombok.AllArgsConstructor;

/**
 * Called when the user clicks Save on the store form: validates the binder,
 * posts the new store and broadcasts the result.
 */
@AllArgsConstructor
public class StoreSaveEventListener implements ComponentEventListener<ClickEvent<Button>>,
        BroadcastMessageService {

    private final transient StoreForm storeForm;
    private final RestClientOrganizationService restClientOrganizationService;

    /**
     * Validates the store form and, if valid, posts the new store.
     *
     * @param buttonClickEvent the click event that triggered the listener
     */
    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        if (storeForm.getBinder().validate().hasErrors()) {
            return;
        }
        restClientOrganizationService.createStore(storeForm.getStoreDto())
                .subscribe(this::accept);
    }

    private void accept(RestAPIResponse restAPIResponse) {
        broadcastMessage(BroadcastMessage.STORE_INSERT_SUCCESS, restAPIResponse);
    }
}
