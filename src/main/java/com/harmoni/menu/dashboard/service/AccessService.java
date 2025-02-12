package com.harmoni.menu.dashboard.service;

import com.harmoni.menu.dashboard.dto.UserDto;
import com.harmoni.menu.dashboard.util.VaadinSessionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.Serializable;

@RequiredArgsConstructor
@Service
@Slf4j
public class AccessService implements Serializable {

    public UserDto getUserDetail() {
        return VaadinSessionUtil.getAttribute(VaadinSessionUtil.USER_DETAIL, UserDto.class);
    }
}
