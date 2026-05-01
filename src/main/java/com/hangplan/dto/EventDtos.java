package com.hangplan.dto;

import com.hangplan.entity.EventStatus;
import com.hangplan.entity.ParticipantStatus;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

public final class EventDtos {

    private EventDtos() {
    }

    @Data
    public static class CreateEventRequest {
        @NotBlank
        private String title;
        @Min(1)
        private int maxParticipants;
    }

    @Data
    @Builder
    public static class EventResponse {
        private String id;
        private String title;
        private int maxParticipants;
        private EventStatus status;
        private String createdById;
        private String createdByName;
        private String createdAt;
        private List<ParticipantView> participants;
    }

    @Data
    @Builder
    public static class ParticipantView {
        private String id;
        private String userId;
        private String name;
        private String email;
        private ParticipantStatus status;
    }

    @Data
    public static class CreateExpenseRequest {
        private String description;
        @NotNull
        @DecimalMin("0.01")
        private BigDecimal amount;
    }

    @Data
    @Builder
    public static class ExpenseView {
        private String id;
        private String amount;
        private String description;
        private String paidByParticipantId;
        private String paidByName;
    }

    @Data
    @Builder
    public static class SummaryResponse {
        private String total;
        private int participantCount;
        private String sharePerPerson;
        private List<BalanceLine> balances;
    }

    @Data
    @Builder
    public static class BalanceLine {
        private String userId;
        private String name;
        private String paid;
        private String share;
        private String balance;
    }

    @Data
    @Builder
    public static class MyEventSummary {
        private String id;
        private String title;
        private EventStatus status;
        private String createdAt;
        private String createdByName;
        private boolean createdByMe;
    }

    @Data
    @Builder
    public static class MyEventsResponse {
        private List<MyEventSummary> events;
    }
}
