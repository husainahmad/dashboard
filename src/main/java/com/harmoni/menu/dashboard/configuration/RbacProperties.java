package com.harmoni.menu.dashboard.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.io.Serializable;

/**
 * Binds the {@code rbac.*} keys: the access-control endpoints exposed by the
 * authentication service through the gateway.
 */
@Configuration
@Data
@ConfigurationProperties("rbac")
public class RbacProperties implements Serializable {

    /** Whether the access-control module is shown in the dashboard. */
    private boolean enabled = true;

    private transient Url url;

    @Data
    public static class Url implements Serializable {
        private String roles;
        private String roleById;
        private String rolePermissions;
        private String permissions;
        private String permissionById;
        private String userRoles;
        private String assignmentsBatch;
        private String roleDuplicate;
        private String me;
    }
}
