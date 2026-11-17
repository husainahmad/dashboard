package com.harmoni.menu.dashboard.layout.security;

import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * Shown when the signed-in user lacks the permission a route requires.
 */
@Route(value = "access-denied", layout = MainLayout.class)
@PageTitle("Access denied | POSHarmoni")
public class AccessDeniedView extends VerticalLayout {

    public AccessDeniedView() {
        addClassName(Css.PAGE);
        setSizeFull();
        setAlignItems(FlexComponent.Alignment.CENTER);
        setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);

        H2 title = new H2(Messages.get("accessDenied.title"));
        Paragraph message = new Paragraph(Messages.get("accessDenied.message"));

        Button back = new Button(Messages.get("accessDenied.back"), VaadinIcon.ARROW_LEFT.create(),
                event -> getUI().ifPresent(ui -> ui.navigate("")));
        back.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        add(title, message, back);
    }
}
