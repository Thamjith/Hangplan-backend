package com.hangplan.controller;

import com.hangplan.dto.EventDtos;
import com.hangplan.realtime.EventRealtimeService;
import com.hangplan.service.EventService;
import com.hangplan.security.HangplanUserPrincipal;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;
    private final EventRealtimeService eventRealtimeService;

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventDtos.EventResponse create(
            @Valid @RequestBody EventDtos.CreateEventRequest request,
            @AuthenticationPrincipal HangplanUserPrincipal auth
    ) {
        EventDtos.EventResponse response = eventService.create(request, auth);
        eventRealtimeService.publishEventUpdated(UUID.fromString(response.getId()));
        return response;
    }

    @GetMapping("/events/{id}")
    public EventDtos.EventResponse get(@PathVariable UUID id) {
        return eventService.get(id);
    }

    @GetMapping("/events/self")
    public EventDtos.MyEventsResponse mine(@AuthenticationPrincipal HangplanUserPrincipal auth) {
        return eventService.listMine(auth);
    }

    @PostMapping("/events/{id}/join")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void join(
            @PathVariable UUID id,
            @AuthenticationPrincipal HangplanUserPrincipal auth
    ) {
        eventService.join(id, auth);
        eventRealtimeService.publishEventUpdated(id);
    }

    @PostMapping("/events/{id}/decline")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void decline(
            @PathVariable UUID id,
            @AuthenticationPrincipal HangplanUserPrincipal auth
    ) {
        eventService.decline(id, auth);
        eventRealtimeService.publishEventUpdated(id);
    }

    @PostMapping("/events/{id}/expenses")
    @ResponseStatus(HttpStatus.CREATED)
    public void addExpense(
            @PathVariable UUID id,
            @Valid @RequestBody EventDtos.CreateExpenseRequest request,
            @AuthenticationPrincipal HangplanUserPrincipal auth
    ) {
        eventService.addExpense(id, request, auth);
        eventRealtimeService.publishEventUpdated(id);
    }

    @GetMapping("/events/{id}/expenses")
    public List<EventDtos.ExpenseView> listExpenses(@PathVariable UUID id) {
        return eventService.listExpenses(id);
    }

    @GetMapping("/events/{id}/summary")
    public EventDtos.SummaryResponse summary(@PathVariable UUID id) {
        return eventService.summary(id);
    }
}
