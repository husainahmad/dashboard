package com.harmoni.menu.dashboard.dto.growth;

import java.time.Instant;

public record ChatMessageDto(Long id, Long sessionId, String role, String message, Instant createdAt) {
}
