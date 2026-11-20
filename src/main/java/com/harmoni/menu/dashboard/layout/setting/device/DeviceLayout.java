package com.harmoni.menu.dashboard.layout.setting.device;

import com.harmoni.menu.dashboard.layout.MainLayout;
import com.harmoni.menu.dashboard.layout.component.PageHeader;
import com.harmoni.menu.dashboard.layout.security.RequiresPermission;
import com.harmoni.menu.dashboard.layout.util.Css;
import com.harmoni.menu.dashboard.layout.util.Permissions;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.service.AuthorizationService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientOrganizationService;
import com.harmoni.menu.dashboard.service.data.rest.AsyncRestClientSettingService;
import com.harmoni.menu.dashboard.service.data.rest.DeviceRestClientService;
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import lombok.RequiredArgsConstructor;

/**
 * Route ({@code /device}) for the store device registry: POS terminals,
 * table-order tablets, self-order kiosks and quick-order devices.
 */
@RequiredArgsConstructor
@Route(value = "device", layout = MainLayout.class)
@RequiresPermission(Permissions.TERMINAL_VIEW)
public class DeviceLayout extends VerticalLayout {

    private final DeviceRestClientService deviceRestClientService;
    private final AsyncRestClientOrganizationService asyncRestClientOrganizationService;
    private final AsyncRestClientSettingService asyncRestClientSettingService;
    private final AccessService accessService;
    private final AuthorizationService authorizationService;

    private boolean built;

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        if (built) {
            return;
        }
        built = true;
        addClassName(Css.LIST_VIEW);
        add(PageHeader.of(Messages.Keys.PAGE_DEVICE_TITLE, Messages.Keys.PAGE_DEVICE_DESCRIPTION));
        add(new DeviceView(deviceRestClientService, asyncRestClientOrganizationService,
                asyncRestClientSettingService, accessService, authorizationService));
        setSizeFull();
    }
}
