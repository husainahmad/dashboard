package com.harmoni.menu.dashboard.util;

import com.vaadin.flow.server.VaadinSession;

public class VaadinSessionUtil {

    private VaadinSessionUtil() {
        throw new IllegalStateException("Util class");
    }

    public static final String JWT_TOKEN = "JWT_TOKEN";

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

}
