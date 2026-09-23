package com.harmoni.menu.dashboard.event;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.harmoni.menu.dashboard.component.BroadcastMessage;
import com.harmoni.menu.dashboard.component.Broadcaster;
import com.harmoni.menu.dashboard.util.ObjectUtil;

/**
 * Contract for event listeners that must propagate a REST outcome to every
 * open session: serialises the result into a {@link BroadcastMessage} and pushes
 * it through {@link Broadcaster}.
 */
public interface BroadcastMessageService {

    /**
     * Serialises {@code data} into a {@link BroadcastMessage} of the given type
     * and broadcasts it to all active sessions so their views refresh.
     *
     * @param type the broadcast message type (see {@link BroadcastMessage})
     * @param data the payload to broadcast
     * @throws IllegalArgumentException if the payload cannot be serialised
     */
    default void broadcastMessage(String type, Object data) {
        try {
            Broadcaster.broadcast(ObjectUtil.objectToJsonString(BroadcastMessage.builder()
                    .type(type)
                    .data(data).build()));
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
