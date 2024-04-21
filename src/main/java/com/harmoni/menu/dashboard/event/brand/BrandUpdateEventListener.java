package com.harmoni.menu.dashboard.event.brand;

import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.exception.BrandHandleException;
import com.harmoni.menu.dashboard.layout.organization.brand.BrandForm;
import com.harmoni.menu.dashboard.rest.data.RestAPIResponse;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;


public class BrandUpdateEventListener implements ComponentEventListener<ClickEvent<Button>> {

    private final BrandForm brandForm;
    private final RestClientOrganizationService restClientOrganizationService;

    public BrandUpdateEventListener(BrandForm brandForm, RestClientOrganizationService restClientOrganizationService) {
        this.brandForm = brandForm;
        this.restClientOrganizationService = restClientOrganizationService;
    }
    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        if (this.brandForm.getBinder().validate().hasErrors()) {
            return;
        };
        BrandDto brandDto = this.brandForm.getBrandDto();
        brandDto.setName(this.brandForm.getBrandNameField().getValue());
        restClientOrganizationService.updateBrand(brandDto)
                .doOnError(error -> new BrandHandleException(this.brandForm.getUi(), "Error while inserting Brand"))
                .subscribe(this::accept);
    }

    private void accept(RestAPIResponse restAPIResponse) {
        this.brandForm.getUi().access(()->{
            Notification notification = new Notification("Brand created..", 3000, Notification.Position.MIDDLE);
            notification.open();

            this.brandForm.setVisible(false);
        });
    }
}
