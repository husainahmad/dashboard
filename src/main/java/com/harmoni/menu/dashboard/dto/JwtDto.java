package com.harmoni.menu.dashboard.dto;

import lombok.Data;

import java.io.Serializable;

/**
 * Authentication result returned after a successful login or token refresh.
 *
 * <p>Deserialized from the {@code /api/v1/auth/login} and
 * {@code /api/v1/auth/refresh-token} responses; the tokens are kept in the
 * Vaadin session and sent as the {@code Authorization} header on later calls.
 */

@Data
public class JwtDto implements Serializable {

    /** The bearer token used to authorize subsequent API calls. */

    private String accessToken;

    /** The token used to obtain a new {@link #accessToken} when it expires. */

    private String refreshToken;
}
