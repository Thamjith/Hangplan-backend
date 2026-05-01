package com.hangplan.service;

import com.hangplan.dto.EventDtos;
import com.hangplan.entity.Event;
import com.hangplan.entity.EventStatus;
import com.hangplan.entity.Expense;
import com.hangplan.entity.Participant;
import com.hangplan.entity.ParticipantStatus;
import com.hangplan.entity.User;
import com.hangplan.repository.EventRepository;
import com.hangplan.repository.ExpenseRepository;
import com.hangplan.repository.ParticipantRepository;
import com.hangplan.repository.UserRepository;
import com.hangplan.security.HangplanUserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EventService {

    private final EventRepository eventRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;
    private final ExpenseRepository expenseRepository;

    @Transactional
    public EventDtos.EventResponse create(EventDtos.CreateEventRequest req, HangplanUserPrincipal auth) {
        User creator = userRepository.getReferenceById(auth.getId());
        Event e = Event.builder()
                .title(req.getTitle().trim())
                .maxParticipants(req.getMaxParticipants())
                .status(EventStatus.OPEN)
                .createdBy(creator)
                .createdAt(Instant.now())
                .build();
        e = eventRepository.save(e);
        participantRepository.save(Participant.builder()
                .event(e)
                .user(creator)
                .status(ParticipantStatus.ACCEPTED)
                .build());
        return toEventResponse(e.getId());
    }

    @Transactional(readOnly = true)
    public EventDtos.EventResponse get(UUID id) {
        return toEventResponse(id);
    }

    @Transactional(readOnly = true)
    public EventDtos.MyEventsResponse listMine(HangplanUserPrincipal auth) {
        UUID uid = auth.getId();
        List<EventDtos.MyEventSummary> events = eventRepository.findDistinctEventsWhereUserParticipates(uid).stream()
                .map(ev -> toMySummary(ev, uid))
                .toList();
        return EventDtos.MyEventsResponse.builder()
                .events(events)
                .build();
    }

    @Transactional
    public void decline(UUID eventId, HangplanUserPrincipal auth) {
        Event e = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        if (e.getCreatedBy().getId().equals(auth.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cannot decline your own event");
        }
        Optional<Participant> existing = participantRepository.findByEventIdAndUserId(eventId, auth.getId());
        if (existing.isPresent()) {
            Participant participant = existing.get();
            if (participant.getStatus() == ParticipantStatus.ACCEPTED) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Already joined this event");
            }
            return;
        }
        User user = userRepository.getReferenceById(auth.getId());
        participantRepository.save(Participant.builder()
                .event(e)
                .user(user)
                .status(ParticipantStatus.DECLINED)
                .build());
    }

    @Transactional
    public void join(UUID eventId, HangplanUserPrincipal auth) {
        Event e = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        if (e.getStatus() == EventStatus.CLOSED) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event is closed");
        }

        Optional<Participant> existingOpt = participantRepository.findByEventIdAndUserId(eventId, auth.getId());
        if (existingOpt.isPresent()) {
            Participant existing = existingOpt.get();
            if (existing.getStatus() == ParticipantStatus.ACCEPTED) {
                return;
            }
            int accepted = participantRepository.countByEventAndStatus(e, ParticipantStatus.ACCEPTED);
            if (accepted >= e.getMaxParticipants()) {
                e.setStatus(EventStatus.CLOSED);
                eventRepository.save(e);
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event is full");
            }
            existing.setStatus(ParticipantStatus.ACCEPTED);
            participantRepository.save(existing);
            int after = accepted + 1;
            if (after >= e.getMaxParticipants()) {
                e.setStatus(EventStatus.CLOSED);
                eventRepository.save(e);
            }
            return;
        }

        int accepted = participantRepository.countByEventAndStatus(e, ParticipantStatus.ACCEPTED);
        if (accepted >= e.getMaxParticipants()) {
            e.setStatus(EventStatus.CLOSED);
            eventRepository.save(e);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Event is full");
        }
        User user = userRepository.getReferenceById(auth.getId());
        participantRepository.save(Participant.builder()
                .event(e)
                .user(user)
                .status(ParticipantStatus.ACCEPTED)
                .build());
        int after = accepted + 1;
        if (after >= e.getMaxParticipants()) {
            e.setStatus(EventStatus.CLOSED);
            eventRepository.save(e);
        }
    }

    @Transactional
    public void addExpense(UUID eventId, EventDtos.CreateExpenseRequest req, HangplanUserPrincipal auth) {
        Event e = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        Participant payer = participantRepository.findByEventIdAndUserId(eventId, auth.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "Join the event first"));
        if (payer.getStatus() != ParticipantStatus.ACCEPTED) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not an active participant");
        }
        String desc = req.getDescription() == null ? "" : req.getDescription().trim();
        Expense ex = Expense.builder()
                .event(e)
                .paidBy(payer)
                .amount(req.getAmount().setScale(2, RoundingMode.HALF_UP))
                .description(desc)
                .build();
        expenseRepository.save(ex);
    }

    @Transactional(readOnly = true)
    public List<EventDtos.ExpenseView> listExpenses(UUID eventId) {
        eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        return expenseRepository.findByEventIdWithPayer(eventId).stream()
                .map(this::toExpenseView)
                .toList();
    }

    @Transactional(readOnly = true)
    public EventDtos.SummaryResponse summary(UUID eventId) {
        Event e = eventRepository.findById(eventId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        List<Participant> participants = participantRepository.findByEventIdWithUser(eventId);
        List<Participant> accepted = participants.stream()
                .filter(p -> p.getStatus() == ParticipantStatus.ACCEPTED)
                .toList();
        int n = accepted.size();
        List<Expense> expenses = expenseRepository.findByEventIdWithPayer(eventId);
        BigDecimal total = expenses.stream()
                .map(Expense::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (n == 0) {
            return EventDtos.SummaryResponse.builder()
                    .total(scale(total))
                    .participantCount(0)
                    .sharePerPerson(scale(BigDecimal.ZERO))
                    .balances(List.of())
                    .build();
        }
        BigDecimal share = total.divide(BigDecimal.valueOf(n), 2, RoundingMode.HALF_UP);
        Map<UUID, BigDecimal> paid = new HashMap<>();
        for (Participant p : accepted) {
            paid.put(p.getId(), BigDecimal.ZERO);
        }
        for (Expense ex : expenses) {
            if (ex.getPaidBy().getStatus() != ParticipantStatus.ACCEPTED) {
                continue;
            }
            paid.merge(ex.getPaidBy().getId(), ex.getAmount(), BigDecimal::add);
        }
        List<EventDtos.BalanceLine> lines = new ArrayList<>();
        for (Participant p : accepted) {
            BigDecimal pAmount = paid.getOrDefault(p.getId(), BigDecimal.ZERO);
            BigDecimal balance = pAmount.subtract(share);
            lines.add(EventDtos.BalanceLine.builder()
                    .userId(p.getUser().getId().toString())
                    .name(p.getUser().getName())
                    .paid(scale(pAmount))
                    .share(scale(share))
                    .balance(scale(balance))
                    .build());
        }
        return EventDtos.SummaryResponse.builder()
                .total(scale(total))
                .participantCount(n)
                .sharePerPerson(scale(share))
                .balances(lines)
                .build();
    }

    private String scale(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private EventDtos.ExpenseView toExpenseView(Expense ex) {
        return EventDtos.ExpenseView.builder()
                .id(ex.getId().toString())
                .amount(ex.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString())
                .description(ex.getDescription())
                .paidByParticipantId(ex.getPaidBy().getId().toString())
                .paidByName(ex.getPaidBy().getUser().getName())
                .build();
    }

    private EventDtos.MyEventSummary toMySummary(Event e, UUID viewerId) {
        return EventDtos.MyEventSummary.builder()
                .id(e.getId().toString())
                .title(e.getTitle())
                .status(e.getStatus())
                .createdAt(e.getCreatedAt().toString())
                .createdByName(e.getCreatedBy().getName())
                .createdByMe(e.getCreatedBy().getId().equals(viewerId))
                .build();
    }

    private EventDtos.EventResponse toEventResponse(UUID id) {
        Event e = eventRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Event not found"));
        User createdBy = e.getCreatedBy();
        List<Participant> list = participantRepository.findByEventIdWithUser(id);
        List<EventDtos.ParticipantView> pviews = list.stream()
                .map(p -> EventDtos.ParticipantView.builder()
                        .id(p.getId().toString())
                        .userId(p.getUser().getId().toString())
                        .name(p.getUser().getName())
                        .email(p.getUser().getEmail())
                        .status(p.getStatus())
                        .build())
                .collect(Collectors.toList());
        return EventDtos.EventResponse.builder()
                .id(e.getId().toString())
                .title(e.getTitle())
                .maxParticipants(e.getMaxParticipants())
                .status(e.getStatus())
                .createdById(createdBy.getId().toString())
                .createdByName(createdBy.getName())
                .createdAt(e.getCreatedAt().toString())
                .participants(pviews)
                .build();
    }
}
