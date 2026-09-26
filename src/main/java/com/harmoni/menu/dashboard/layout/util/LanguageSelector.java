package com.harmoni.menu.dashboard.layout.util;

import com.harmoni.menu.dashboard.util.Messages;
import com.harmoni.menu.dashboard.util.VaadinSessionUtil;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;

import java.util.Locale;

/**
 * Header control for switching the UI language between English and Indonesian.
 *
 * <p>The choice is stored per user session in {@link VaadinSessionUtil} rather
 * than in the JVM default locale, so one user's choice does not change the
 * language for everybody else signed in on the same server. Because the
 * translations are baked into components when they are built, the page is
 * reloaded after switching so every label is re-rendered in the new language.
 * </p>
 */
public final class LanguageSelector {

    /** English, used as the default when nothing was selected yet. */
    public static final Locale ENGLISH = Locale.ENGLISH;

    /** Bahasa Indonesia. */
    public static final Locale INDONESIAN = new Locale("id", "ID");

    /** The languages offered in the switcher. */
    private static final Locale[] SUPPORTED = {ENGLISH, INDONESIAN};

    private LanguageSelector() {
        throw new IllegalStateException("Utility class");
    }

    /**
     * Creates the language menu bar, marking the active language as checked.
     *
     * @return the configured menu bar
     */
    public static MenuBar create() {
        MenuBar menuBar = new MenuBar();
        menuBar.setThemeName(Css.TERTIARY_INLINE);
        MenuItem languageItem = menuBar.addItem(VaadinIcon.GLOBE.create());
        languageItem.setAriaLabel(Messages.get("nav.language"));

        for (Locale locale : SUPPORTED) {
            MenuItem option = languageItem.getSubMenu().addItem(label(locale),
                    event -> select(UI.getCurrent(), locale));
            option.setCheckable(true);
            option.setChecked(isActive(locale));
        }
        return menuBar;
    }

    /**
     * Returns the label of a supported language, always shown in that language
     * so it stays recognisable regardless of the active translation.
     *
     * @param locale the language to label
     * @return the language label
     */
    private static String label(Locale locale) {
        return Messages.get("id".equals(locale.getLanguage())
                ? "nav.language.id" : "nav.language.en");
    }

    /**
     * Tells whether the given language is the one currently in use. Falls back
     * to the JVM default locale when the user has not chosen one yet.
     *
     * @param locale the language to check
     * @return true when the language is active
     */
    private static boolean isActive(Locale locale) {
        Locale active = VaadinSessionUtil.getLocale();
        if (active == null) {
            active = Locale.getDefault();
        }
        return locale.getLanguage().equals(active.getLanguage());
    }

    /**
     * Stores the language for the session and reloads the page so all labels
     * are re-rendered in the newly selected language.
     *
     * @param ui     the current UI
     * @param locale the language to switch to
     */
    private static void select(UI ui, Locale locale) {
        if (ui == null) {
            return;
        }
        VaadinSessionUtil.setLocale(locale);
        ui.getPage().reload();
    }
}
