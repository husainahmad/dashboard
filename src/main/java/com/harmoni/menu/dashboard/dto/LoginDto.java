package com.harmoni.menu.dashboard.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Credentials sent to authenticate a dashboard user.
 *
 * <p>Serialized as the request body of the {@code /api/v1/auth/login}
 * endpoint; the username is also used to look up the logged-in user's detail.
 */

@Data
public class LoginDto implements Serializable {

    /** The username of the account being authenticated. */

    private String username;

    /** The plain-text password of the account being authenticated. */

    private String password;
}
