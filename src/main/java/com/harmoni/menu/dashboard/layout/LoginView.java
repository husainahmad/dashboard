package com.harmoni.menu.dashboard.layout;

import com.harmoni.menu.dashboard.dto.LoginDto;
import com.harmoni.menu.dashboard.event.user.LoginEventListener;
import com.harmoni.menu.dashboard.service.data.rest.RestClientLoginService;
import com.harmoni.menu.dashboard.layout.util.ThemeUtil;
import com.vaadin.flow.component.AttachEvent;
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
import com.vaadin.flow.theme.lumo.Lumo;
import lombok.RequiredArgsConstructor;

/**
 * The sign-in screen at {@code /login}. Shows the POSHarmoni branding next to
 * a username/password form and delegates authentication to
 * {@link RestClientLoginService}. The layout is re-drawn entirely on every
 * attach to apply the persisted theme, so a browser reload rebuilds the page.
 */
@RequiredArgsConstructor
@Route(value = "login")
public class LoginView extends VerticalLayout {

    private final RestClientLoginService restClientLoginService;

    TextField usernameField = new TextField("Username");
    PasswordField passwordField = new PasswordField("Password");
    Button loginButton = new Button("Sign in");
    Span messageSpan = new Span("");

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

        loginButton.addClickListener(new LoginEventListener(restClientLoginService, this));
    }

    private VerticalLayout createBrandPanel() {
        Div badge = new Div("");
        badge.addClassName("app-logo-icon");

        H1 title = new H1("POSHarmoni");
        Paragraph tagline = new Paragraph("The all-in-one menu management platform for your stores.");

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

    private VerticalLayout createFormPanel() {
        H2 welcome = new H2("Sign in");
        welcome.getStyle().set("margin", "0");
        Paragraph subtitle = new Paragraph("Welcome back — enter your credentials to continue.");
        subtitle.addClassName("login-subtitle");

        usernameField.setWidthFull();
        passwordField.setWidthFull();
        messageSpan.addClassName("login-message");

        loginButton.setWidthFull();
        loginButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_LARGE);
        loginButton.setAutofocus(true);

        VerticalLayout panel = new VerticalLayout(
                welcome, subtitle, usernameField, passwordField, loginButton, messageSpan);
        panel.addClassName("login-panel");
        panel.setSpacing(true);
        panel.setPadding(true);

        VerticalLayout formSide = new VerticalLayout(panel);
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

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        ThemeUtil.applySavedTheme(attachEvent.getUI());
        drawLayout();
    }
}