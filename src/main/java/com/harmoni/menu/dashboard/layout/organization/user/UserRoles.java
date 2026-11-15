package com.harmoni.menu.dashboard.layout.organization.user;

import com.harmoni.menu.dashboard.dto.UserDto;
import com.harmoni.menu.dashboard.layout.enums.RoleType;
import com.harmoni.menu.dashboard.util.Messages;

/**
 * Small presentation helpers that keep the role badge, label and avatar
 * initials consistent across the user list, detail card and dialog.
 */
final class UserRoles {

    private UserRoles() {
        throw new IllegalStateException("Utility class");
    }

    static RoleType roleOf(Integer authId) {
        if (authId == null) {
            return RoleType.USER;
        }
        return switch (authId) {
            case 1 -> RoleType.ADMIN;
            case 2 -> RoleType.MANAGER;
            default -> RoleType.USER;
        };
    }

    static String label(Integer authId) {
        return Messages.get("user.role." + roleOf(authId).name().toLowerCase());
    }

    static String cssClass(Integer authId) {
        return "user-role-" + roleOf(authId).name().toLowerCase();
    }

    static String initials(UserDto user) {
        String name = user == null || user.getUsername() == null ? "" : user.getUsername().trim();
        if (name.isEmpty()) {
            return "?";
        }
        String[] parts = name.split("[\\s._-]+");
        if (parts.length > 1 && !parts[0].isEmpty() && !parts[1].isEmpty()) {
            return (parts[0].charAt(0) + "" + parts[1].charAt(0)).toUpperCase();
        }
        return name.length() > 1 ? name.substring(0, 2).toUpperCase() : name.toUpperCase();
    }
}
