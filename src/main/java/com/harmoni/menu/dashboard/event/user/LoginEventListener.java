package com.harmoni.menu.dashboard.event.user;

import com.harmoni.menu.dashboard.dto.AuthoritiesDto;
import com.harmoni.menu.dashboard.dto.JwtDto;
import com.harmoni.menu.dashboard.dto.LoginDto;
import com.harmoni.menu.dashboard.dto.UserDto;
import com.harmoni.menu.dashboard.layout.DashboardView;
import com.harmoni.menu.dashboard.layout.LoginView;
import com.harmoni.menu.dashboard.service.AuthorizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestAPIResponse;
import com.harmoni.menu.dashboard.service.data.rest.RestClientLoginService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientRbacService;
import com.harmoni.menu.dashboard.util.Messages;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.harmoni.menu.dashboard.util.VaadinSessionUtil;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentEventListener;
import com.vaadin.flow.component.button.Button;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ObjectUtils;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.Optional;

/**
 * Handles the sign-in click: authenticates against the auth service, then
 * loads the user's authorities and profile as best-effort extras. The session
 * is only written once the whole chain succeeds, on the UI thread, so a failed
 * auxiliary lookup cannot leave a half-initialised session behind.
 */
@Slf4j
@RequiredArgsConstructor
public class LoginEventListener implements ComponentEventListener<ClickEvent<Button>> {

    /** Cap on the best-effort profile lookups so a slow service cannot block sign-in. */
    private static final Duration PROFILE_TIMEOUT = Duration.ofSeconds(3);

    private final RestClientLoginService restClientLoginService;
    private final RestClientRbacService restClientRbacService;
    private final AuthorizationService authorizationService;
    private final LoginView loginView;

    @Override
    public void onComponentEvent(ClickEvent<Button> event) {
        LoginDto credentials = loginView.getLoginDto();
        if (ObjectUtils.isEmpty(credentials.getUsername()) || ObjectUtils.isEmpty(credentials.getPassword())) {
            loginView.showError(Messages.get("validation.login.required"));
            return;
        }

        loginView.clearError();
        loginView.setBusy(true);
        log.debug("Login requested username={}", credentials.getUsername());

        restClientLoginService.login(credentials)
                .flatMap(jwt -> loadProfile(jwt, credentials.getUsername()))
                .subscribe(
                        result -> runOnUi(() -> onSuccess(result)),
                        error -> runOnUi(() -> onFailure(error)));
    }

    /**
     * Loads the authorities and user detail in parallel. Both are optional: a
     * failure in either one must not block the sign-in, because the auth
     * service already accepted the credentials.
     *
     * @param jwt      the token pair returned by the auth service
     * @param username the username being signed in
     * @return a {@link Mono} that always emits a {@link LoginResult}
     */
    private Mono<LoginResult> loadProfile(JwtDto jwt, String username) {
        if (jwt == null || ObjectUtils.isEmpty(jwt.getAccessToken())) {
            return Mono.error(new IllegalStateException("Login response did not contain an access token"));
        }

        Mono<Optional<AuthoritiesDto>> authorities = restClientRbacService
                .getMyAuthorities(jwt.getAccessToken())
                .map(this::toAuthorities)
                .defaultIfEmpty(Optional.empty())
                .timeout(PROFILE_TIMEOUT)
                .onErrorResume(error -> {
                    log.warn("Could not load authorities for {}", username, error);
                    return Mono.just(Optional.empty());
                });

        Mono<Optional<UserDto>> user = restClientLoginService
                .getUser(username, jwt.getAccessToken())
                .map(this::toUser)
                .defaultIfEmpty(Optional.empty())
                .timeout(PROFILE_TIMEOUT)
                .onErrorResume(error -> {
                    log.warn("Could not load user detail for {}", username, error);
                    return Mono.just(Optional.empty());
                });

        return Mono.zip(authorities, user)
                .map(tuple -> new LoginResult(jwt, tuple.getT1().orElse(null), tuple.getT2().orElse(null)));
    }

    private Optional<AuthoritiesDto> toAuthorities(RestAPIResponse response) {
        if (response == null || ObjectUtils.isEmpty(response.getData())) {
            return Optional.empty();
        }
        return Optional.ofNullable(ObjectUtil.convertValueToObject(response.getData(), AuthoritiesDto.class));
    }

    private Optional<UserDto> toUser(RestAPIResponse response) {
        if (response == null || ObjectUtils.isEmpty(response.getData())) {
            return Optional.empty();
        }
        return Optional.ofNullable(ObjectUtil.convertValueToObject(response.getData(), UserDto.class));
    }

    private void onSuccess(LoginResult result) {
        VaadinSessionUtil.setAttribute(VaadinSessionUtil.JWT_TOKEN, result.jwt().getAccessToken());
        VaadinSessionUtil.setAttribute(VaadinSessionUtil.REFRESH_TOKEN, result.jwt().getRefreshToken());
        if (result.authorities() != null) {
            authorizationService.store(result.authorities());
        }
        if (result.user() != null) {
            VaadinSessionUtil.setAttribute(VaadinSessionUtil.USER_DETAIL, result.user());
        }
        loginView.setBusy(false);
        loginView.getUI().ifPresent(ui -> ui.navigate(DashboardView.class));
    }

    private void onFailure(Throwable error) {
        log.warn("Login failed", error);
        loginView.setBusy(false);
        loginView.showError(Messages.get(Messages.Keys.NOTIFICATION_LOGIN_FAILED));
    }

    private void runOnUi(Runnable action) {
        loginView.getUI().ifPresent(ui -> ui.access(action::run));
    }

    private record LoginResult(JwtDto jwt, AuthoritiesDto authorities, UserDto user) {
    }
}
