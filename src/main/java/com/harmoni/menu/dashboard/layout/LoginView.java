package com.harmoni.menu.dashboard.layout;

import com.harmoni.menu.dashboard.dto.LoginDto;
import com.harmoni.menu.dashboard.event.user.LoginEventListener;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.layout.util.LanguageSelector;
import com.harmoni.menu.dashboard.layout.util.ThemeUtil;
import com.harmoni.menu.dashboard.service.AuthorizationService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientLoginService;
import com.harmoni.menu.dashboard.service.data.rest.RestClientRbacService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import lombok.RequiredArgsConstructor;

/**
 * The sign-in screen at {@code /login}. Shows the POSHarmoni branding next to
 * a username/password form and delegates authentication to
 * {@link LoginEventListener}. The layout is re-drawn on every attach to apply
 * the persisted theme, while the click listener is wired once on first attach
 * so re-attaching cannot register it twice.
 */
@Route(value = "login")
@RequiredArgsConstructor
public class LoginView extends VerticalLayout {

    private final RestClientLoginService restClientLoginService;
    private final RestClientRbacService restClientRbacService;
    private final AuthorizationService authorizationService;

    private final TextField usernameField = new TextField(Messages.get("label.username"));
    private final PasswordField passwordField = new PasswordField(Messages.get(Messages.Keys.LABEL_PASSWORD));
    private final Button loginButton = new Button(Messages.get("action.signIn"));
    private final Span messageSpan = new Span();

    private boolean loginWired;

    /**
     * Wires the click listener and Enter shortcut exactly once. It runs on the
     * first attach because the view can be re-attached without being rebuilt.
     */
    private void wireLogin() {
        if (loginWired) {
            return;
        }
        loginWired = true;
        messageSpan.addClassName("login-message");
        loginButton.addClickListener(new LoginEventListener(
                restClientLoginService, restClientRbacService, authorizationService, this));
        loginButton.addClickShortcut(Key.ENTER);
    }

    /**
     * Draws the login view with a two-column layout: the left side shows the
     * branding and tagline, the right side shows the form. The layout is
     * re-drawn on every attach to apply the persisted theme.
     */
    private void drawLayout() {
        removeAll();
        setSizeFull();
        setPadding(false);
        setSpacing(false);
        setAlignItems(FlexComponent.Alignment.STRETCH);

        HorizontalLayout root = new HorizontalLayout();
        root.setSizeFull();
        root.setSpacing(false);
        root.setPadding(false);
        root.addClassName("login-view");
        root.setAlignItems(FlexComponent.Alignment.STRETCH);

        VerticalLayout brandPanel = createBrandPanel();
        VerticalLayout formSidePanel = createFormPanel();
        root.add(brandPanel, formSidePanel);
        root.setFlexGrow(2, brandPanel);
        root.setFlexGrow(3, formSidePanel);
        add(root);
    }

    /**
     * Creates the left side of the login view with the POSHarmoni logo, title,
     * and tagline.
     *
     * @return the branding panel
     */
    private VerticalLayout createBrandPanel() {
        Div badge = new Div("");
        badge.addClassName("app-logo-icon");

        H1 title = new H1("POSHarmoni");
        Paragraph tagline = new Paragraph(Messages.get("login.tagline"));

        VerticalLayout content = new VerticalLayout(badge, title, tagline);
        content.addClassName("login-brand-content");
        content.setSpacing(true);
        content.setAlignItems(FlexComponent.Alignment.START);

        VerticalLayout brand = new VerticalLayout(content);
        brand.addClassName("login-brand");
        brand.setHeightFull();
        brand.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        brand.setAlignItems(FlexComponent.Alignment.START);
        return brand;
    }

    /**
     * Creates the right side of the login view with the username/password form
     * and sign-in button.
     *
     * @return the form panel
     */
    private VerticalLayout createFormPanel() {
        H2 welcome = new H2(Messages.get("login.welcome"));
        welcome.getStyle().set(Css.MARGIN, "0");
        Paragraph subtitle = new Paragraph(Messages.get("login.subtitle"));
        subtitle.addClassName("login-subtitle");

        usernameField.setWidthFull();
        passwordField.setWidthFull();

        loginButton.setWidthFull();
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);

        VerticalLayout panel = new VerticalLayout(
                welcome, subtitle, usernameField, passwordField, loginButton, messageSpan);
        panel.addClassName("login-panel");
        panel.setSpacing(true);
        panel.setPadding(true);

        VerticalLayout formSide = new VerticalLayout(panel, LanguageSelector.create());
        formSide.addClassName("login-form-side");
        formSide.setHeightFull();
        formSide.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        formSide.setAlignItems(FlexComponent.Alignment.CENTER);
        return formSide;
    }

    /**
     * Reads the credentials currently entered in the form fields.
     *
     * @return the username and password as entered
     */
    public LoginDto getLoginDto() {
        LoginDto loginDto = new LoginDto();
        loginDto.setUsername(usernameField.getValue());
        loginDto.setPassword(passwordField.getValue());
        return loginDto;
    }

    /**
     * Shows an inline error under the form.
     *
     * @param message the message to display, or {@code null} to clear it
     */
    public void showError(String message) {
        messageSpan.setText(message == null ? "" : message);
    }

    /**
     * Clears the inline error message.
     */
    public void clearError() {
        messageSpan.setText("");
    }

    /**
     * Locks the form while a login request is in flight.
     *
     * @param busy whether the request is running
     */
    public void setBusy(boolean busy) {
        loginButton.setEnabled(!busy);
        usernameField.setReadOnly(busy);
        passwordField.setReadOnly(busy);
        loginButton.setText(Messages.get(busy
                ? Messages.Keys.ACTION_SIGNING_IN
                : "action.signIn"));
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        ThemeUtil.applySavedTheme(attachEvent.getUI());
        wireLogin();
        drawLayout();
        usernameField.focus();
    }
}
