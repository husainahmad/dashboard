package com.harmoni.menu.dashboard.layout;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.dto.UserDto;
import com.harmoni.menu.dashboard.event.BroadcastMessageService;
import com.harmoni.menu.dashboard.layout.component.DialogClosing;
import com.harmoni.menu.dashboard.layout.navigation.SideNavMenu;
import com.harmoni.menu.dashboard.layout.util.ThemeUtil;
import com.harmoni.menu.dashboard.service.AccessService;
import com.harmoni.menu.dashboard.util.ObjectUtil;
import com.harmoni.menu.dashboard.util.VaadinSessionUtil;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.shared.Registration;
import com.vaadin.flow.theme.lumo.Lumo;
import org.apache.commons.lang3.ObjectUtils;

/**
 * Root application shell hosting the header (logo, palette switcher, theme
 * toggle and user menu) plus the side-navigation drawer. Guards every route by
 * forwarding unauthenticated users to the login screen and surfaces broadcast
 * error notifications as dialogs.
 */
public class MainLayout extends AppLayout implements BroadcastMessageService, BeforeEnterObserver {

    Registration broadcasterRegistration;

    /** Application title displayed in the header logo. */
    public static final String TITLE = "POSHarmoni";

    private final AccessService accessService;

    private final Icon moonIcon = new Icon(VaadinIcon.MOON);
    private final Icon sunIcon = new Icon(VaadinIcon.SUN_O);
    private Button themeToggle;

    /**
     * Creates the app shell, building the header and drawer navigation.
     *
     * @param accessService resolves the logged-in user for the header menu
     */
    public MainLayout(AccessService accessService) {
        this.accessService = accessService;
        createHeader();
        createDrawer();
    }

    private void createHeader() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setTooltipText("Toggle navigation");

        H2 logo = createLogo();

        HorizontalLayout actions = new HorizontalLayout(createPaletteMenu(), createThemeToggle(), createUserMenu());
        actions.setSpacing(true);
        actions.setPadding(false);
        actions.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout navbar = new HorizontalLayout(toggle, logo, actions);
        navbar.setWidthFull();
        navbar.setPadding(false);
        navbar.setSpacing(false);
        navbar.setAlignItems(FlexComponent.Alignment.CENTER);
        navbar.setFlexGrow(1, logo);
        navbar.addClassName("app-navbar");

        addToNavbar(navbar);
    }

    private H2 createLogo() {
        Div badge = new Div("P");
        badge.addClassName("app-logo-icon");
        H2 logo = new H2(badge, new Span("POSHarmoni"));
        logo.addClassName("app-logo");
        return logo;
    }

    private MenuBar createPaletteMenu() {
        MenuBar palette = new MenuBar();
        palette.setThemeName("tertiary-inline");
        MenuItem paletteItem = palette.addItem(new Icon(VaadinIcon.PALETTE));
        paletteItem.setAriaLabel("Color palette");

        paletteItem.getSubMenu().addItem("Emerald (default)", event -> applyPalette(null));
        paletteItem.getSubMenu().addItem("Warm Food", event -> applyPalette("preset-warm"));
        return palette;
    }

    private void applyPalette(String token) {
        UI.getCurrent().getElement().getThemeList().removeIf(theme -> theme.startsWith("preset-"));
        if (token != null) {
            UI.getCurrent().getElement().getThemeList().add(token);
        }
    }

    private Button createThemeToggle() {
        // App boots in night mode, so the toggle offers light first.
        Button toggle = new Button(sunIcon);
        toggle.addClassName("app-theme-toggle");
        toggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        toggle.setTooltipText("Toggle light/dark mode");
        toggle.addClickListener(event -> {
            boolean dark = UI.getCurrent().getElement().getThemeList().contains(Lumo.DARK);
            if (dark) {
                UI.getCurrent().getElement().getThemeList().remove(Lumo.DARK);
                toggle.setIcon(moonIcon);
                ThemeUtil.storeTheme(UI.getCurrent(), false);
            } else {
                UI.getCurrent().getElement().getThemeList().add(Lumo.DARK);
                toggle.setIcon(sunIcon);
                ThemeUtil.storeTheme(UI.getCurrent(), true);
            }
        });
        themeToggle = toggle;
        return toggle;
    }

    private Component createUserMenu() {
        UserDto user = accessService.getUserDetail();
        String name = user != null && ObjectUtils.isNotEmpty(user.getUsername())
                ? user.getUsername() : "Guest";
        String initials = name.length() >= 2 ? name.substring(0, 2).toUpperCase() : name.toUpperCase();

        Div avatar = new Div(initials);
        avatar.addClassName("app-avatar");
        Span label = new Span(name);
        label.addClassName("app-user-name");
        HorizontalLayout chip = new HorizontalLayout(avatar, label);
        chip.addClassName("app-user-chip");

        MenuBar menuBar = new MenuBar();
        menuBar.setThemeName("tertiary-inline");
        MenuItem userItem = menuBar.addItem(chip);
        userItem.setAriaLabel("User menu");
        userItem.getSubMenu().addItem("Sign out", event -> logout());
        return menuBar;
    }

    private void logout() {
        getUI().ifPresent(ui -> ui.getSession().close());
        getUI().ifPresent(ui -> ui.navigate(LoginView.class));
    }

    private void createDrawer() {
        addToDrawer(new SideNavMenu());
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        ThemeUtil.applySavedTheme(attachEvent.getUI(),
                dark -> themeToggle.setIcon(dark ? sunIcon : moonIcon));
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
        if (broadcastMessage.getType().equals(BroadcastMessage.UN_AUTHORIZED)) {
            getUI().ifPresent(ui -> ui.access(() -> {
                VaadinSessionUtil.close();
                ui.navigate(LoginView.class);
            }));
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

    /**
     * Forwards to the login screen when no JWT token is present in the session.
     *
     * @param beforeEnterEvent the navigation event carrying the target route
     */
    @Override
    public void beforeEnter(BeforeEnterEvent beforeEnterEvent) {
        String token = VaadinSessionUtil.getAttribute(VaadinSessionUtil.JWT_TOKEN, String.class);
        if (ObjectUtils.isEmpty(token)) {
            beforeEnterEvent.forwardTo(LoginView.class);
        }
    }
}