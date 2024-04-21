package com.harmoni.menu.dashboard.event.chain;

import com.harmoni.menu.dashboard.dto.ChainDto;
import com.harmoni.menu.dashboard.layout.organization.chain.ChainForm;
import com.harmoni.menu.dashboard.rest.data.RestAPIResponse;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;

public class ChainUpdateEventListener implements ComponentEventListener<ClickEvent<Button>> {

    private final ChainForm chainForm;
    private final RestClientOrganizationService restClientOrganizationService;

    public ChainUpdateEventListener(ChainForm chainForm, RestClientOrganizationService restClientOrganizationService) {
        this.chainForm = chainForm;
        this.restClientOrganizationService = restClientOrganizationService;
    }
    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        if (this.chainForm.getBinder().validate().hasErrors()) {
            return;
        }

        ChainDto chainDto = this.chainForm.getChainDto();
        chainDto.setName(this.chainForm.getChainNameField().getValue());

        restClientOrganizationService.updateChain(chainDto)
                .subscribe(this::accept);
    }

    private void accept(RestAPIResponse restAPIResponse) {
        this.chainForm.getUi().access(()->{
            Notification notification = new Notification("Chain updated..", 3000, Notification.Position.MIDDLE);
            notification.open();

            this.chainForm.setVisible(false);
        });
    }
}
