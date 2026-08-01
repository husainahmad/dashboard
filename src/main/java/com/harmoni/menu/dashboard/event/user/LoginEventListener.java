package com.harmoni.menu.dashboard.event.user;

import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.dto.JwtDto;
import com.harmoni.menu.dashboard.dto.UserDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.layout.DashboardView;
import com.harmoni.menu.dashboard.layout.LoginView;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientLoginService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.harmoni.menu.dashboard.util.VaadinSessionUtil;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import reactor.core.publisher.Mono;

@Slf4j
@RequiredArgsConstructor
public class LoginEventListener implements ComponentEventListener<ClickEvent<Button>>,
        BroadcastMessageService {

    private final RestClientLoginService restClientLoginService;
    private final LoginView loginView;

    @Override
    public void onComponentEvent(ClickEvent<Button> buttonClickEvent) {
        log.debug("Login button clicked username={}", loginView.getLoginDto().getUsername());
        restClientLoginService.login(loginView.getLoginDto())
            .onErrorResume(throwable -> {
                handleLoginError(throwable);
                return Mono.empty();
            })
            .subscribe(this::accept);
    }

    private void accept(JwtDto jwtDto) {
        log.debug("Login response received hasAccessToken={}",
                ObjectUtils.isNotEmpty(jwtDto) && ObjectUtils.isNotEmpty(jwtDto.getAccessToken()));

        if (ObjectUtils.isEmpty(jwtDto) || ObjectUtils.isEmpty(jwtDto.getAccessToken())) {
            loginView.getUI().ifPresent(ui -> ui.access(() ->
                    showErrorMessage("Login failed, please make sure username is correct!! ")));
            return;
        }

        setTokenInSession(jwtDto);

        restClientLoginService.getUser(loginView.getLoginDto().getUsername(), jwtDto.getAccessToken())
                .doOnError(error -> log.error("Get User Detail Error", error))
                .subscribe(this::acceptUserDetail, error -> navigateToDashboard());

        broadcastMessage(BroadcastMessage.LOGIN_SUCCESS, jwtDto);
    }

    private void setTokenInSession(JwtDto jwtDto) {
        loginView.getUI().ifPresent(ui -> ui.access(() -> {
            VaadinSessionUtil.setAttribute(VaadinSessionUtil.JWT_TOKEN, jwtDto.getAccessToken());
            VaadinSessionUtil.setAttribute(VaadinSessionUtil.REFRESH_TOKEN, jwtDto.getRefreshToken());
        }));
    }

    private void navigateToDashboard() {
        log.debug("Navigating to DashboardView");
        loginView.getUI().ifPresent(ui -> ui.access(() -> ui.navigate(DashboardView.class)));
    }

    private void acceptUserDetail(RestAPIResponse restAPIResponse) {
        log.debug("User detail response received httpStatus={} hasData={}",
                restAPIResponse.getHttpStatus(), ObjectUtils.isNotEmpty(restAPIResponse.getData()));
        loginView.getUI().ifPresent(ui -> ui.access(() -> {
            if (ObjectUtils.isNotEmpty(restAPIResponse.getData())) {
                UserDto userDto = ObjectUtil.convertValueToObject(restAPIResponse.getData(), UserDto.class);
                log.debug("Storing user detail username={} userId={}", userDto.getUsername(), userDto.getId());
                VaadinSessionUtil.setAttribute(VaadinSessionUtil.USER_DETAIL, userDto);
            }
            ui.navigate(DashboardView.class);
        }));
    }

    private void handleLoginError(Throwable error) {
        log.error("Login Error", error);
        loginView.getUI().ifPresent(ui -> ui.access(() -> showErrorMessage("Login failed, please make sure username is correct!! ")));
    }

    private void showErrorMessage(String message) {
        Notification.show(message, 3000, Notification.Position.MIDDLE);
    }
}
