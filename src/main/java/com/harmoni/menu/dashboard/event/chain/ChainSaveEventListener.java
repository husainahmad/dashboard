package com.harmoni.menu.dashboard.event.chain;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.layout.organization.chain.ChainForm;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import lombok.RequiredArgsConstructor;

/**
 * Called when the user clicks Save on the chain form: validates the binder,
 * posts the new chain and broadcasts the result.
 */
@RequiredArgsConstructor
public class ChainSaveEventListener implements ComponentEventListener<ClickEvent<Button>>,
        BroadcastMessageService {

    private final ChainForm chainForm;
    private final RestClientOrganizationService restClientOrganizationService;

    /**
     * Validates the chain form and, if valid, posts the new chain.
     *
     * @param buttonClickEvent the click event that triggered the listener
     */
    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        if (this.chainForm.getBinder().validate().hasErrors()) {
            return;
        }
        ChainDto chainDto = this.chainForm.getChainDto();
        chainDto.setName(this.chainForm.getChainNameField().getValue());
        chainDto.setBrandId(this.chainForm.getBrandComboBox().getValue().getId());
        restClientOrganizationService.createChain(chainDto)
                .subscribe(this::accept);
    }

    private void accept(RestAPIResponse restAPIResponse) {
        broadcastMessage(BroadcastMessage.CHAIN_INSERT_SUCCESS, restAPIResponse);
    }
}
