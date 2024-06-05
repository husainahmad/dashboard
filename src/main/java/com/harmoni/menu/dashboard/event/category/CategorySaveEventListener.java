package com.harmoni.menu.dashboard.event.category;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.layout.menu.category.CategoryForm;
import com.harmoni.menu.dashboard.layout.organization.brand.BrandForm;
import com.harmoni.menu.dashboard.rest.data.RestAPIResponse;
import com.harmoni.menu.dashboard.rest.data.RestClientMenuService;
import com.harmoni.menu.dashboard.rest.data.RestClientOrganizationService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public class CategorySaveEventListener implements ComponentEventListener<ClickEvent<Button>> {

    private final CategoryForm categoryForm;
    private final RestClientMenuService restClientMenuService;

    public CategorySaveEventListener(CategoryForm categoryForm, RestClientMenuService restClientMenuService) {
        this.categoryForm = categoryForm;
        this.restClientMenuService = restClientMenuService;
    }
    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        if (this.categoryForm.getBinder().validate().hasErrors()) {
            return;
        };

        CategoryDto categoryDto = this.categoryForm.getCategoryDto();
        categoryDto.setName(this.categoryForm.getCategoryNameField().getValue());
        categoryDto.setBrandId(this.categoryForm.getBrandBox().getValue().getId());
        categoryDto.setDescription(this.categoryForm.getCategoryDescArea().getValue());

        restClientMenuService.createCategory(categoryDto)
                .subscribe(this::accept);
    }

    private void accept(RestAPIResponse restAPIResponse) {
        try {
            Broadcaster.broadcast(ObjectUtil.objectToJsonString(BroadcastMessage.builder()
                    .type(BroadcastMessage.CATEGORY_INSERT_SUCCESS)
                    .data(restAPIResponse).build()));
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }

}
