package com.harmoni.menu.dashboard.event.user;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.layout.DashboardView;
import com.harmoni.menu.dashboard.layout.LoginView;
import com.harmoni.menu.dashboard.rest.data.RestAPIResponse;
import com.harmoni.menu.dashboard.rest.data.RestClientLoginService;
import com.harmoni.menu.dashboard.util.VaadinSessionUtil;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.ObjectUtils;

@RequiredArgsConstructor
public class LoginEventListener implements ComponentEventListener<ClickEvent<Button>>,
        BroadcastMessageService {

    private final RestClientLoginService restClientLoginService;
    private final LoginView loginView;

    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        restClientLoginService.login(loginView.getLoginDto())
                .subscribe(this::accept);
    }

    private void accept(RestAPIResponse restAPIResponse) {

        broadcastMessage(BroadcastMessage.LOGIN_SUCCESS, restAPIResponse);

        loginView.getUI().ifPresent(ui -> ui.access(() -> {
            if (ObjectUtils.isNotEmpty(restAPIResponse.getData())) {
                VaadinSessionUtil.setAttribute(VaadinSessionUtil.JWT_TOKEN, restAPIResponse.getData().toString());
            }
            ui.navigate(DashboardView.class);
        }));
    }
}
