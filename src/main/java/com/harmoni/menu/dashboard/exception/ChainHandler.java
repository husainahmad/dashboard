package com.harmoni.menu.dashboard.exception;

import com.harmoni.menu.dashboard.layout.organization.chain.ChainForm;
import com.harmoni.menu.dashboard.layout.util.UiUtil;

public class ChainHandler {
    public ChainHandler(ChainForm chainForm, String message) {
        chainForm.getUi().access(() -> UiUtil.error(message));
    }
}
