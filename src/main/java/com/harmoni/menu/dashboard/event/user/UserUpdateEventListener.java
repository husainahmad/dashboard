package com.harmoni.menu.dashboard.event.user;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.layout.organization.user.UserForm;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientOrganizationService;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import lombok.AllArgsConstructor;

/**
 * Called when the user clicks Update on the user form: validates the binder,
 * posts the updated user and broadcasts the result.
 */
@AllArgsConstructor
public class UserUpdateEventListener implements ComponentEventListener<ClickEvent<Button>>,
        BroadcastMessageService {

    private final transient UserForm userForm;
    private final RestClientOrganizationService restClientOrganizationService;

    /**
     * Validates the user form and, if valid, posts the updated user.
     *
     * @param buttonClickEvent the click event that triggered the listener
     */
    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        if (userForm.getBinder().validate().hasErrors()) {
            return;
        }
        restClientOrganizationService.updateUser(userForm.getUserDto())
                .subscribe(this::accept);
    }

    private void accept(RestAPIResponse restAPIResponse) {
        broadcastMessage(BroadcastMessage.STORE_INSERT_SUCCESS, restAPIResponse);
    }
}
