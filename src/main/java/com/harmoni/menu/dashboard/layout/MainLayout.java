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
import com.harmoni.menu.dashboard.util.Messages;
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import com.harmoni.menu.dashboard.layout.util.Css;

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

    /**
     * Creates the header with a drawer toggle, logo, palette switcher, theme
     * toggle and user menu.
     */
    private void createHeader() {
        DrawerToggle toggle = new DrawerToggle();
        toggle.setTooltipText(Messages.get("nav.toggle"));

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

    /**
     * Creates the logo with a badge and title.
     *
     * @return the logo component
     */
    private H2 createLogo() {
        Div badge = new Div("P");
        badge.addClassName("app-logo-icon");
        H2 logo = new H2(badge, new Span(TITLE));
        logo.addClassName("app-logo");
        return logo;
    }

    /**
     * Creates the palette menu with options to switch between the default and
     * warm color palettes.
     *
     * @return the palette menu component
     */
    private MenuBar createPaletteMenu() {
        MenuBar palette = new MenuBar();
        palette.setThemeName(Css.TERTIARY_INLINE);
        MenuItem paletteItem = palette.addItem(new Icon(VaadinIcon.PALETTE));
        paletteItem.setAriaLabel(Messages.get("nav.palette"));

        paletteItem.getSubMenu().addItem(Messages.get("nav.palette.emerald"), event -> applyPalette(null));
        paletteItem.getSubMenu().addItem(Messages.get("nav.palette.warm"), event -> applyPalette("preset-warm"));
        return palette;
    }

    /**
     * Applies the selected palette by removing any existing preset and adding
     * the new one. If the token is null, it reverts to the default palette.
     *
     * @param token the theme token to apply, or null for default
     */
    private void applyPalette(String token) {
        UI.getCurrent().getElement().getThemeList().removeIf(theme -> theme.startsWith("preset-"));
        if (token != null) {
            UI.getCurrent().getElement().getThemeList().add(token);
        }
    }

    /**
     * Creates the theme toggle button that switches between light and dark
     * modes. The initial state is determined by the current theme.
     *
     * @return the theme toggle button
     */
    private Button createThemeToggle() {
        // App boots in night mode, so the toggle offers light first.
        Button toggle = new Button(sunIcon);
        toggle.addClassName("app-theme-toggle");
        toggle.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ICON);
        toggle.setTooltipText(Messages.get("nav.themeToggle"));
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

    /**
     * Creates the user menu with the user's initials and name. If no user is
     * logged in, it shows "Guest". The menu provides a sign-out option.
     *
     * @return the user menu component
     */
    private Component createUserMenu() {
        UserDto user = accessService.getUserDetail();
        String name = user != null && ObjectUtils.isNotEmpty(user.getUsername())
                ? user.getUsername() : Messages.get("label.guest");
        String initials = name.length() >= 2 ? name.substring(0, 2).toUpperCase() : name.toUpperCase();

        Div avatar = new Div(initials);
        avatar.addClassName("app-avatar");
        Span label = new Span(name);
        label.addClassName("app-user-name");
        HorizontalLayout chip = new HorizontalLayout(avatar, label);
        chip.addClassName("app-user-chip");

        MenuBar menuBar = new MenuBar();
        menuBar.setThemeName(Css.TERTIARY_INLINE);
        MenuItem userItem = menuBar.addItem(chip);
        userItem.setAriaLabel(Messages.get("nav.userMenu"));
        userItem.getSubMenu().addItem(Messages.get("action.signOut"), event -> logout());
        return menuBar;
    }

    /**
     * Logs out the user by closing the current session and navigating to the
     * login view.
     */
    private void logout() {
        getUI().ifPresent(ui -> ui.getSession().close());
        getUI().ifPresent(ui -> ui.navigate(LoginView.class));
    }

    /**
     * Creates the side navigation drawer with the main menu items.
     */
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

    /**
     * Accepts a broadcast message, parses it, and shows an error dialog if the
     * message indicates a bad request or process failure. If the message
     * indicates an unauthorized access, it navigates to the login view.
     *
     * @param message the broadcast message as a JSON string
     */
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

    /**
     * Shows an error dialog if the broadcast message indicates a bad request or
     * process failure. If the message indicates an unauthorized access, it
     * navigates to the login view.
     *
     * @param broadcastMessage the broadcast message to process
     */
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

    /**
     * Displays an error dialog with the given message. The dialog is added to
     * the current UI and opened in a thread-safe manner.
     *
     * @param message the error message to display
     */
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