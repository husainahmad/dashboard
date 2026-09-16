package com.harmoni.menu.dashboard.exception;

import com.harmoni.menu.dashboard.layout.util.UiUtil;
import com.vaadin.flow.component.UI;

public class BrandHandler {
    public BrandHandler(UI ui, String message) {
        ui.access(() -> UiUtil.error(message));
    }
}
