package com.harmoni.menu.dashboard.layout.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the permission(s) a route requires. The main layout checks it before
 * entering the view and forwards to the access-denied page when none is held.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface RequiresPermission {

    /**
     * @return the accepted permission names; any one is enough
     */
    String[] value();
}
