package com.harmoni.menu.dashboard.layout.organization.tier.price;

import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.layout.organization.tier.TierForm;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

@Route("tier-price-form")
@Slf4j
public class TierPriceForm extends TierForm {

    public TierPriceForm(RestClientOrganizationService restClientOrganizationService,
                         AsyncRestClientOrganizationService asyncRestClientOrganizationService) {
        super(restClientOrganizationService, asyncRestClientOrganizationService);
        addValidation();
        brandBox.setItemLabelGenerator(BrandDto::getName);
        add(brandBox);
        add(tierNameField);
        add(createButtonsLayout(true, true, true));
        getBinder().bindInstanceFields(this);
        fetchBrands();
    }

    void setChangeTierDto(TierDto tierDto) {
        this.setTierDtoAndBind(tierDto);

        if (!ObjectUtils.isEmpty(tierDto) && !ObjectUtils.isEmpty(tierDto.getBrandId())) {
            fetchDetailBrands(tierDto.getBrandId().longValue());
        }
    }

}
