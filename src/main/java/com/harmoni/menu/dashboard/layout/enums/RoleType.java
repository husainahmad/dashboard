package com.harmoni.menu.dashboard.layout.enums;

import lombok.Getter;

/**
 * User roles that determine dashboard access levels.
 */
@Getter
public enum RoleType {
    /** Administrative access with full control. */
    ADMIN(1),
    /** Manager-level access. */
    MANAGER(2),
    /** Regular user access. */
    USER(3);

    private final int id;

    RoleType(int id) {
        this.id = id;
    }
}

