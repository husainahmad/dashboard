package com.harmoni.menu.dashboard.layout.organization.tier.price;

import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.layout.organization.tier.TierForm;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.vaadin.flow.router.Route;
import lombok.extern.slf4j.Slf4j;

/**
 * Tier form specialized for price tiers (type {@code PRICE}). Adds the brand
 * and name fields, binds them, and preloads the available brands
 * asynchronously.
 */
@Route("tier-price-form")
@Slf4j
public class TierPriceForm extends TierForm {

    /**
     * Creates the price tier form with the given REST clients.
     *
     * @param restClientOrganizationService     the synchronous REST client
     * @param asyncRestClientOrganizationService the async REST client
     */
    public TierPriceForm(RestClientOrganizationService restClientOrganizationService,
                         AsyncRestClientOrganizationService asyncRestClientOrganizationService) {
        super(restClientOrganizationService, asyncRestClientOrganizationService);
        addValidation();
        brandBox.setItemLabelGenerator(BrandDto::getName);
        add(brandBox);
        add(tierNameField);
        add(createButtonsLayout(true, true));
        getBinder().bindInstanceFields(this);
        fetchBrands();
    }

}
