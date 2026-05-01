package com.hangplan.realtime;

import com.hangplan.entity.User;
import com.hangplan.repository.UserRepository;
import com.hangplan.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class WebSocketAuthChannelInterceptor implements ChannelInterceptor {

    private static final String EVENT_TOPIC_PREFIX = "/topic/events/";

    private final JwtService jwtService;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            User user = resolveUserFromAuthHeader(accessor)
                    .orElseThrow(() -> new AccessDeniedException("WebSocket auth required"));
            accessor.setUser(new HangplanStompPrincipal(user.getId(), user.getEmail()));
            return message;
        }

        if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            String destination = accessor.getDestination();
            if (destination != null && destination.startsWith(EVENT_TOPIC_PREFIX)) {
                User user = resolveUserFromPrincipal(accessor)
                        .orElseThrow(() -> new AccessDeniedException("Unauthorized subscription"));
                if (!user.isPremium()) {
                    throw new AccessDeniedException("Premium subscription required");
                }
            }
        }
        return message;
    }

    private Optional<User> resolveUserFromPrincipal(StompHeaderAccessor accessor) {
        if (!(accessor.getUser() instanceof HangplanStompPrincipal principal)) {
            return Optional.empty();
        }
        return userRepository.findById(principal.userId());
    }

    private Optional<User> resolveUserFromAuthHeader(StompHeaderAccessor accessor) {
        List<String> authHeaders = accessor.getNativeHeader("Authorization");
        if (authHeaders == null || authHeaders.isEmpty()) {
            return Optional.empty();
        }
        String header = authHeaders.getFirst();
        if (header == null || !header.startsWith("Bearer ")) {
            return Optional.empty();
        }

        String token = header.substring(7).trim();
        if (token.isEmpty()) {
            return Optional.empty();
        }

        try {
            UUID userId = jwtService.parseUserId(token);
            return userRepository.findById(userId);
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }
}
