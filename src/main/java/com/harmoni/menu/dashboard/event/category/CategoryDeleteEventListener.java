package com.harmoni.menu.dashboard.event.category;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.layout.menu.category.CategoryForm;
import com.harmoni.menu.dashboard.rest.data.RestAPIResponse;
import com.harmoni.menu.dashboard.rest.data.RestClientMenuService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class CategoryDeleteEventListener implements ComponentEventListener<ClickEvent<Button>>,
        BroadcastMessageService {

    private final CategoryForm categoryForm;
    private final RestClientMenuService restClientMenuService;

    public CategoryDeleteEventListener(CategoryForm categoryForm, RestClientMenuService restClientMenuService) {
        this.categoryForm = categoryForm;
        this.restClientMenuService = restClientMenuService;
    }

    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        if (this.categoryForm.getBinder().validate().hasErrors()) {
            return;
        }

        CategoryDto categoryDto = this.categoryForm.getCategoryDto();
        categoryDto.setId(this.categoryForm.getCategoryDto().getId());
        categoryDto.setName(this.categoryForm.getCategoryNameField().getValue());
        categoryDto.setBrandId(this.categoryForm.getBrandBox().getValue().getId());
        categoryDto.setDescription(this.categoryForm.getCategoryDescArea().getValue());

        restClientMenuService.deleteCategory(categoryDto)
                .subscribe(this::accept);
    }

    private void accept(RestAPIResponse restAPIResponse) {
        this.categoryForm.getUi().access(()->{
            Notification notification = new Notification("Category deleted..", 3000, Notification.Position.MIDDLE);
            notification.open();

            this.categoryForm.setVisible(false);
            broadcastMessage(BroadcastMessage.CATEGORY_UPDATED_SUCCESS, restAPIResponse);
        });
    }

}
