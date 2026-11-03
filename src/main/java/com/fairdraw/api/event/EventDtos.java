package com.fairdraw.api.event;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class EventDtos {

    public record CreateEventRequest(
            @NotBlank @Size(max = 100) String title,
            @NotBlank @Size(max = 100) String venue,
            @NotNull OffsetDateTime startsAt,
            @NotNull OffsetDateTime entryOpensAt,
            @NotNull OffsetDateTime entryClosesAt,
            @NotNull OffsetDateTime paymentDueAt,
            @NotNull @Min(0) Integer price,
            @Min(1) @Max(10) Integer ballotsPerUser,
            @Min(1) @Max(4) Integer maxTeamSize) {}

    /** 바꾸고 싶은 칸만 보낸다. 안 보낸 칸(null)은 그대로 둔다. */
    public record UpdateEventRequest(
            @Size(max = 100) String title,
            @Size(max = 100) String venue,
            OffsetDateTime startsAt,
            OffsetDateTime entryOpensAt,
            OffsetDateTime entryClosesAt,
            OffsetDateTime paymentDueAt,
            @Min(0) Integer price,
            @Min(1) @Max(10) Integer ballotsPerUser,
            @Min(1) @Max(4) Integer maxTeamSize,
            String status) {}

    public record CreateSectionRequest(
            @NotBlank @Size(max = 10) String name,
            @NotNull @Min(1) Integer capacity,
            @Min(0) Integer price) {}

    public record AddStaffRequest(@NotBlank String phone) {}

    public record SectionResponse(UUID id, String name, Integer capacity, Integer price) {
        static SectionResponse from(Section s) {
            return new SectionResponse(s.getId(), s.getName(), s.getCapacity(), s.getPrice());
        }
    }

    public record EventResponse(
            UUID id, UUID organizerId, String title, String venue,
            OffsetDateTime startsAt, OffsetDateTime entryOpensAt,
            OffsetDateTime entryClosesAt, OffsetDateTime paymentDueAt,
            Integer price, Integer ballotsPerUser, Integer maxTeamSize,
            String status, List<SectionResponse> sections) {

        static EventResponse from(Event e, List<Section> sections) {
            return new EventResponse(
                    e.getId(), e.getOrganizerId(), e.getTitle(), e.getVenue(),
                    e.getStartsAt(), e.getEntryOpensAt(), e.getEntryClosesAt(), e.getPaymentDueAt(),
                    e.getPrice(), e.getBallotsPerUser(), e.getMaxTeamSize(), e.getStatus(),
                    sections.stream().map(SectionResponse::from).toList());
        }
    }

    public record StaffResponse(UUID id, String phone, boolean registered) {}
}