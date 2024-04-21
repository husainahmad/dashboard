package com.harmoni.menu.dashboard.component;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.router.Route;

@Route("notification-success")
public class NotificationSuccess extends Div {
    public NotificationSuccess() {
        Notification notification = new Notification("Submitted!");
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        notification.setPosition(Notification.Position.MIDDLE);
        notification.setDuration(0);
    }
}
