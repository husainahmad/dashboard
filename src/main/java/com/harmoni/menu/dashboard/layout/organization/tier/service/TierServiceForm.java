package com.harmoni.menu.dashboard.layout.organization.tier.service;

import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.TierDto;
import com.harmoni.menu.dashboard.layout.organization.tier.TierForm;
import com.harmoni.menu.dashboard.rest.data.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
import com.vaadin.flow.component.*;
import com.vaadin.flow.component.checkbox.CheckboxGroup;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;

import java.util.ArrayList;
import java.util.List;

@Route("tier-service-form")
@Slf4j
public class TierServiceForm extends TierForm {

    private final CheckboxGroup<String> checkboxGroup = new CheckboxGroup<>();
    @Getter
    private List<String> subNames;

    public TierServiceForm(RestClientOrganizationService restClientOrganizationService,
                           AsyncRestClientOrganizationService asyncRestClientOrganizationService) {
        super(restClientOrganizationService, asyncRestClientOrganizationService);
        brandBox.setItemLabelGenerator(BrandDto::getName);
        add(brandBox);
        add(tierNameField);
        add(createButtonsLayout(true, false, true));
        fetchServices();
        setServiceBoxChangeListener();
        setCheckBoxSubServiceChangeListener();
        addValidation();
    }

    private void setServiceBoxChangeListener() {
        getServiceBox().addValueChangeListener(event -> {
            event.getValue().stream().toList().forEach(serviceDto -> {
                checkboxGroup.setLabel("Sub Service");
                List<String> subServices = new ArrayList<>();
                serviceDto.getSubServices().forEach(subServiceDto -> subServices.add(subServiceDto.getName()));
                checkboxGroup.setItems(subServices.toArray(new String[0]));
            });
        });
    }

    private void setCheckBoxSubServiceChangeListener() {
        checkboxGroup.addValueChangeListener(changeEvent -> {
            log.debug("{}", changeEvent);
            subNames = changeEvent.getValue().stream().toList();
        });
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

        getBinder().bindInstanceFields(this);
    }

    public void changeTierDto(TierDto tierDto) {
        this.setTierDtoAndBind(tierDto);

        if (!ObjectUtils.isEmpty(tierDto) && !ObjectUtils.isEmpty(tierDto.getBrandId())) {
            fetchDetailBrands(tierDto.getBrandId().longValue());
        }
    }

    private void fetchServices() {
        this.getAsyncRestClientOrganizationService().getAllServicesAsync(result ->
                getUi().access(()-> getServiceBox().setItems(result)));
    }

    private void fetchDetailBrands(Long id) {
        this.getAsyncRestClientOrganizationService().getDetailBrandAsync(result -> {
            getUi().access(()-> brandBox.setValue(result));
        }, id);
    }

}
