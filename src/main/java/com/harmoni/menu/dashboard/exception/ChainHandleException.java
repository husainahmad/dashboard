package com.harmoni.menu.dashboard.exception;

import com.harmoni.menu.dashboard.layout.organization.chain.ChainForm;
import com.vaadin.flow.component.notification.Notification;

public class ChainHandleException {
    public ChainHandleException(ChainForm chainForm, String message) {

        chainForm.getUi().access(()->{
            Notification notification = new Notification(message, 3000, Notification.Position.MIDDLE);
            notification.open();
        });

    }
}
