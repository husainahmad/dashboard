package com.harmoni.menu.dashboard.layout.organization;

/**
 * Distinguishes the modes a form can be opened in: creating a new record
 * ({@code CREATE}), editing an existing one ({@code EDIT}) or changing only the
 * lifecycle state of an existing one ({@code STATUS}).
 */
public enum FormAction {
    CREATE, EDIT, STATUS
}
