package com.harmoni.menu.dashboard.event.category;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.BrandDto;
import com.harmoni.menu.dashboard.dto.CategoryDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.layout.menu.category.CategoryForm;
import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientMenuService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Called when the user clicks Save on the category form: validates the binder
 * and the selected brand, posts the new category and broadcasts the result.
 */
@RequiredArgsConstructor
@Slf4j
public class CategorySaveEventListener implements ComponentEventListener<ClickEvent<Button>>,
        BroadcastMessageService {

    private final CategoryForm categoryForm;
    private final RestClientMenuService restClientMenuService;

    /**
     * Validates the category form and its brand selection and, if valid, posts
     * the new category.
     *
     * @param buttonClickEvent the click event that triggered the listener
     */
    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        if (this.categoryForm.getBinder().validate().hasErrors()) {
            return;
        }

        BrandDto brand = this.categoryForm.getBrandBox().getValue();
        if (brand == null || brand.getId() == null || brand.getId() <= 0) {
            this.categoryForm.showNotification("Brand not allow to be empty");
            return;
        }

        CategoryDto categoryDto = this.categoryForm.getCategoryDto();
        categoryDto.setName(this.categoryForm.getCategoryNameField().getValue());
        categoryDto.setBrandId(brand.getId());
        categoryDto.setDescription(this.categoryForm.getCategoryDescArea().getValue());

        restClientMenuService.createCategory(categoryDto)
                .subscribe(this::accept);
    }

    private void accept(RestAPIResponse restAPIResponse) {
        this.categoryForm.getUi().access(()->{
            UiUtil.success("Category created..");
            this.categoryForm.close();
            broadcastMessage(BroadcastMessage.CATEGORY_INSERT_SUCCESS, restAPIResponse);
        });
    }

}
