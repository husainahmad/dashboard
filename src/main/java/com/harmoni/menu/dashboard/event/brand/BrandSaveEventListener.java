package com.harmoni.menu.dashboard.event.brand;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.layout.organization.brand.BrandForm;
import com.harmoni.menu.dashboard.rest.data.RestAPIResponse;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public class BrandSaveEventListener implements ComponentEventListener<ClickEvent<Button>> {

    private final BrandForm brandForm;
    private final RestClientOrganizationService restClientOrganizationService;

    public BrandSaveEventListener(BrandForm brandForm, RestClientOrganizationService restClientOrganizationService) {
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
        brandDto.setChainId(this.brandForm.getChainBox().getValue().getId());
        restClientOrganizationService.createBrand(brandDto)
                .subscribe(this::accept);
    }


    private void accept(RestAPIResponse restAPIResponse) {
        try {
            Broadcaster.broadcast(ObjectUtil.objectToJsonString(BroadcastMessage.builder()
                    .type(BroadcastMessage.BRAND_INSERT_SUCCESS)
                    .data(restAPIResponse).build()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
