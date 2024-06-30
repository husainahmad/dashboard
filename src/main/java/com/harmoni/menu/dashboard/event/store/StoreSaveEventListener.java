package com.harmoni.menu.dashboard.event.store;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.layout.organization.store.StoreForm;
import com.harmoni.menu.dashboard.rest.data.RestAPIResponse;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;

public class StoreSaveEventListener implements ComponentEventListener<ClickEvent<Button>> {

    private final StoreForm storeForm;

    private final RestClientOrganizationService restClientOrganizationService;

    public StoreSaveEventListener(StoreForm storeForm, RestClientOrganizationService restClientOrganizationService) {
        this.storeForm = storeForm;
        this.restClientOrganizationService = restClientOrganizationService;
    }
    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        if (this.storeForm.getBinder().validate().hasErrors()) {
            return;
        }
        StoreDto storeDto = new StoreDto();
        storeDto.setName(this.storeForm.getStoreNameField().getValue());
        storeDto.setBrandId(this.storeForm.getBrandBox().getValue().getId());
        storeDto.setTierId(this.storeForm.getTierBox().getValue().getId());
        storeDto.setAddress(this.storeForm.getAddressArea().getValue());
        restClientOrganizationService.createStore(storeDto)
                .subscribe(this::accept);
    }

    private void accept(RestAPIResponse restAPIResponse) {
        try {
            Broadcaster.broadcast(ObjectUtil.objectToJsonString(BroadcastMessage.builder()
                    .type(BroadcastMessage.STORE_INSERT_SUCCESS)
                    .data(restAPIResponse).build()));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
