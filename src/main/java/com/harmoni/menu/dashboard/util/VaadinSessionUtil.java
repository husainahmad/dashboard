package com.harmoni.menu.dashboard.util;

import com.vaadin.flow.server.VaadinSession;

import java.util.Locale;

/**
 * Type-safe accessors for the current {@link VaadinSession}.
 *
 * <p>Holds the authentication material (JWT and refresh tokens), the signed-in
 * user details and the selected UI locale under the keys declared below. All
 * methods tolerate a missing session and return {@code null} then.
 */
public class VaadinSessionUtil {

    public static final String JWT_TOKEN = "JWT_TOKEN";
    public static final String REFRESH_TOKEN = "REFRESH_TOKEN";
    public static final String USER_DETAIL = "USER_DETAIL";
    public static final String LOCALE = "LOCALE";

    private VaadinSessionUtil() {
        throw new IllegalStateException("Util class");
    }

    public static void setAttribute(String key, Object value) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(key, value);
        }
    }

    public static void close() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.close();
        }
    }

    public static <T> T getAttribute(String key, Class<T> type) {
        VaadinSession session = VaadinSession.getCurrent();
        return session != null ? type.cast(session.getAttribute(key)) : null;
    }

    /**
     * Stores the UI language for the current session. The choice is per user
     * session rather than JVM-wide, so switching language on one browser does
     * not affect the other signed-in users.
     *
     * @param locale the locale to use for this session
     */
    public static void setLocale(Locale locale) {
        setAttribute(LOCALE, locale);
    }

    /**
     * Returns the UI language selected for the current session.
     *
     * @return the session locale, or {@code null} when the user has not chosen
     *         one yet
     */
    public static Locale getLocale() {
        return getAttribute(LOCALE, Locale.class);
    }

}
