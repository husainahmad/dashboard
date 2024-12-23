package com.harmoni.menu.dashboard.layout;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.layout.component.DialogClosing;
import com.harmoni.menu.dashboard.layout.navigation.SideNavMenu;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.shared.Registration;
import org.apache.commons.lang3.ObjectUtils;


@CssImport("./styles/shared-styles.css")
public class MainLayout extends AppLayout implements BroadcastMessageService {

    Registration broadcasterRegistration;

    public static final String TITLE = "POSHarmoni";

    public MainLayout() {
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        DrawerToggle toggle = new DrawerToggle();

        H2 logo = new H2(TITLE);
        HorizontalLayout header = new HorizontalLayout(toggle, logo);
        header.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.CENTER);
        header.expand(logo);
        header.setWidth("100%");
        header.addClassNames("py-0", "px-m");

        addToNavbar(header);
    }

    private void createDrawer() {
        addToDrawer(new SideNavMenu());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        broadcasterRegistration = Broadcaster.register(this::acceptNotification);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        broadcasterRegistration.remove();
        broadcasterRegistration = null;
    }

    private void acceptNotification(String message) {

        try {
            BroadcastMessage broadcastMessage = (BroadcastMessage) ObjectUtil.jsonStringToBroadcastMessageClass(message);
            if (ObjectUtils.isNotEmpty(broadcastMessage) && ObjectUtils.isNotEmpty(broadcastMessage.getType())) {
                showErrorDialogOnlyProcessError(broadcastMessage);
            }
        } catch (JsonProcessingException e) {
            showErrorDialog(e.getMessage());
        }
    }

    private void showErrorDialogOnlyProcessError(BroadcastMessage broadcastMessage) {
        if (broadcastMessage.getType().equals(BroadcastMessage.BAD_REQUEST_FAILED) ||
                broadcastMessage.getType().equals(BroadcastMessage.PROCESS_FAILED)) {
            showErrorDialog(broadcastMessage.getData().toString());
        }
    }

    private void showErrorDialog(String message) {
        if (getUI().isPresent()) {
            getUI().orElseThrow().access(() -> {
                DialogClosing dialog = new DialogClosing(message);
                this.getUI().get().add(dialog);
                dialog.open();
            });
        }
    }
}
