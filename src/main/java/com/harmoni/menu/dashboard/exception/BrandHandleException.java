package com.harmoni.menu.dashboard.exception;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.notification.Notification;

public class BrandHandleException {
    public BrandHandleException(UI ui, String message) {

        ui.access(()->{
            Notification notification = new Notification(message, 3000, Notification.Position.MIDDLE);
            notification.open();
        });

    }
}
