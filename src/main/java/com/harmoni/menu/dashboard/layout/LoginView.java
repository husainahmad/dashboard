package com.harmoni.menu.dashboard.layout;

import com.harmoni.menu.dashboard.dto.LoginDto;
import com.harmoni.menu.dashboard.event.user.LoginEventListener;
import com.harmoni.menu.dashboard.service.data.rest.RestClientLoginService;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Route(value = "login")
@CssImport("./styles/shared-styles.css")
public class LoginView extends VerticalLayout {

    private final RestClientLoginService restClientLoginService;

    TextField usernameField = new TextField("Username");
    PasswordField passwordField = new PasswordField("Password");
    Button loginButton = new Button("Login");
    Span messageSpan = new Span("");

    private void drawLayout() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        H2 welcomeMessage = new H2("Welcome to POSHarmoni");
        welcomeMessage.getStyle()
                .set("margin-bottom", "10px")
                .set("font-size", "20px")
                .set("color", "#fffff");

        VerticalLayout formLayout = new VerticalLayout(welcomeMessage, usernameField, passwordField, loginButton, messageSpan);
        formLayout.setWidth("300px");
        formLayout.setAlignItems(Alignment.CENTER);

        Div panel = new Div();
        panel.getStyle()
                .set("padding", "20px")
                .set("border", "1px solid #ccc")
                .set("border-radius", "10px")
                .set("box-shadow", "2px 2px 10px rgba(0,0,0,0.1)")
                .set("width", "320px")
                .set("text-align", "center");
        panel.add(formLayout);
        add(panel);

        loginButton.addClickListener(new LoginEventListener(restClientLoginService, this));
    }

    public LoginDto getLoginDto() {
        LoginDto loginDto = new LoginDto();
        loginDto.setUsername(usernameField.getValue());
        loginDto.setPassword(passwordField.getValue());
        return loginDto;
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        drawLayout();
    }
}
