package com.harmoni.menu.dashboard.layout.organization.tier.price;

import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.layout.organization.tier.TierForm;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.textfield.TextField;
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

    private void addValidation() {

        brandBox.addValueChangeListener(changeEvent -> getBinder().validate());
        getBinder().forField(brandBox)
                        .withValidator(value -> value.getId() > 0, "Brand not allow to be empty"
                        ).bind(TierDto::getBrandDto, TierDto::setBrandDto);
        tierNameField.addValueChangeListener(
                (HasValue.ValueChangeListener<AbstractField
                        .ComponentValueChangeEvent<TextField, String>>) changeEvent -> getBinder().validate());

        getBinder().forField(tierNameField)
                .withValidator(value -> value.length() > 2,
                        "Name must contain at least three characters")
                .bind(TierDto::getName, TierDto::setName);

    }

    void setChangeTierDto(TierDto tierDto) {
        this.setTierDtoAndBind(tierDto);

        if (!ObjectUtils.isEmpty(tierDto) && !ObjectUtils.isEmpty(tierDto.getBrandId())) {
            fetchDetailBrands(tierDto.getBrandId().longValue());
        }
    }

    private void fetchBrands() {
        this.getAsyncRestClientOrganizationService().getAllBrandAsync(result ->
                getUi().access(()-> brandBox.setItems(result)));
    }

    private void fetchDetailBrands(Long id) {
        this.getAsyncRestClientOrganizationService().getDetailBrandAsync(result ->
                getUi().access(()-> brandBox.setValue(result)), id);
    }
}
