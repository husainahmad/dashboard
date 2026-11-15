package com.harmoni.menu.dashboard.service;

import com.harmoni.menu.dashboard.dto.StoreDto;
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

    /**
     * Returns the store the logged-in user belongs to.
     *
     * @return the session {@link StoreDto}, or {@code null} when not logged in
     */
    public StoreDto getStore() {
        UserDto user = getUserDetail();
        return user == null ? null : user.getStoreDto();
    }

    /**
     * Returns the id of the store the logged-in user belongs to.
     *
     * @return the session store id, or {@code null} when not logged in
     */
    public Integer getStoreId() {
        StoreDto store = getStore();
        return store == null ? null : store.getId();
    }

    /**
     * Returns the id of the chain the logged-in user's store belongs to.
     *
     * @return the session chain id, or {@code null} when not logged in
     */
    public Integer getChainId() {
        StoreDto store = getStore();
        return store == null || store.getChainDto() == null ? null : store.getChainDto().getId();
    }

    /**
     * Returns the id of the brand the logged-in user's store belongs to.
     *
     * @return the session brand id, or {@code null} when not logged in
     */
    public Integer getBrandId() {
        StoreDto store = getStore();
        return store == null || store.getChainDto() == null ? null : store.getChainDto().getBrandId();
    }
}
