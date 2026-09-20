package com.harmoni.menu.dashboard.configuration;

import lombok.Data;

import java.io.Serializable;

/**
 * Holds the REST URLs of the auth endpoints.
 *
 * <p>
 * Binds the {@code auth.url.login} and {@code auth.url.refresh-token} keys;
 * consumed as the {@code url} subtree of {@link AuthProperties}.
 * </p>
 */

@Data
public class AuthUrlProperties implements Serializable {
    private String login;
    private String refreshToken;
}
