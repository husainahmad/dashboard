package com.harmoni.menu.dashboard.event.store;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.StoreDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.layout.organization.store.StoreForm;
import com.harmoni.menu.dashboard.rest.data.RestAPIResponse;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;

public class StoreUpdateEventListener implements ComponentEventListener<ClickEvent<Button>>,
        BroadcastMessageService {

    private final StoreForm storeForm;

    private final RestClientOrganizationService restClientOrganizationService;

    public StoreUpdateEventListener(StoreForm storeForm,
                                    RestClientOrganizationService restClientOrganizationService) {
        this.storeForm = storeForm;
        this.restClientOrganizationService = restClientOrganizationService;
    }

    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        if (this.storeForm.getBinder().validate().hasErrors()) {
            return;
        }
        StoreDto storeDto = new StoreDto();
        storeDto.setId(this.storeForm.getStoreDto().getId());
        storeDto.setName(this.storeForm.getStoreNameField().getValue());
        storeDto.setChainId(this.storeForm.getChainDtoComboBox().getValue().getId());
        storeDto.setTierId(this.storeForm.getTierBox().getValue().getId());
        storeDto.setAddress(this.storeForm.getAddressArea().getValue());
        restClientOrganizationService.updateStore(storeDto)
                .subscribe(this::accept);
    }

    private void accept(RestAPIResponse restAPIResponse) {
        broadcastMessage(BroadcastMessage.STORE_UPDATED_SUCCESS, restAPIResponse);
    }

}
