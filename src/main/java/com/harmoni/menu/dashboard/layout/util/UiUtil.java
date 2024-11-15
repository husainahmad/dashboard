package com.harmoni.menu.dashboard.layout.util;

import com.harmoni.menu.dashboard.layout.component.DialogClosing;
import com.vaadin.flow.component.HasComponents;
import com.vaadin.flow.component.UI;

public class UiUtil {

    public static void showErrorDialog(UI ui, HasComponents hasComponents, String message) {
        DialogClosing dialog = new DialogClosing(message);
        ui.access(()-> {
            hasComponents.add(dialog);
            dialog.open();
        });
    }

}
