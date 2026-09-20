package com.harmoni.menu.dashboard.service;

import com.harmoni.menu.dashboard.dto.UserDto;
import com.harmoni.menu.dashboard.util.VaadinSessionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;

/**
 * Convenience accessor for the currently logged-in user, resolving the value
 * already stored in the Vaadin session.
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class AccessService implements Serializable {

    /**
     * Returns the logged-in user's details from the Vaadin session.
     *
     * @return the current {@link UserDto}, or {@code null} when not logged in
     */
    public UserDto getUserDetail() {
        return VaadinSessionUtil.getAttribute(VaadinSessionUtil.USER_DETAIL, UserDto.class);
    }
}
