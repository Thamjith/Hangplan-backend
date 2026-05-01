package com.hangplan.realtime;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EventRealtimeService {

    private final SimpMessagingTemplate messagingTemplate;

    public void publishEventUpdated(UUID eventId) {
        messagingTemplate.convertAndSend("/topic/events/" + eventId, Map.of("eventId", eventId.toString()));
    }
}
