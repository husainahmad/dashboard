package com.harmoni.menu.dashboard.event.brand;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.exception.BrandHandler;
import com.harmoni.menu.dashboard.layout.organization.brand.BrandForm;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@RequiredArgsConstructor
@Slf4j
public class BrandUpdateEventListener implements ComponentEventListener<ClickEvent<Button>>,
        BroadcastMessageService {

    private final BrandForm brandForm;
    private final RestClientOrganizationService restClientOrganizationService;

    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        if (this.brandForm.getBinder().validate().hasErrors()) {
            return;
        }
        BrandDto brandDto = this.brandForm.getBrandDto();
        brandDto.setName(this.brandForm.getBrandNameField().getValue());
        restClientOrganizationService.updateBrand(brandDto)
                .doOnError(error -> new BrandHandler(this.brandForm.getUi(), "Error while inserting Brand ".concat(error.getMessage())))
                .subscribe(this::accept);
    }

    private void accept(RestAPIResponse restAPIResponse) {
        this.brandForm.getUi().access(()->{
            Notification notification = new Notification("Brand created..", 3000, Notification.Position.MIDDLE);
            notification.open();

            this.brandForm.setVisible(false);
            broadcastMessage(BroadcastMessage.BRAND_SUCCESS_UPDATED, restAPIResponse);
        });
    }
}
