package com.fairdraw.api.event;

import com.fairdraw.api.event.EventDtos.*;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class EventController {

    private final EventService eventService;

    @PostMapping("/api/events")
    @ResponseStatus(HttpStatus.CREATED)
    public EventResponse create(@AuthenticationPrincipal Jwt jwt,
                                @Valid @RequestBody CreateEventRequest req) {
        return eventService.create(jwt, req);
    }

    @GetMapping("/api/events")
    public List<EventResponse> myEvents(@AuthenticationPrincipal Jwt jwt) {
        return eventService.myEvents(jwt);
    }

    @GetMapping("/api/public/events")
    public List<EventResponse> openEvents() {
        return eventService.openEvents();
    }

    @GetMapping("/api/events/{id}")
    public EventResponse get(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        return eventService.get(jwt, id);
    }

    @PatchMapping("/api/events/{id}")
    public EventResponse update(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                @Valid @RequestBody UpdateEventRequest req) {
        return eventService.update(jwt, id, req);
    }

    @PostMapping("/api/events/{id}/sections")
    @ResponseStatus(HttpStatus.CREATED)
    public SectionResponse addSection(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                      @Valid @RequestBody CreateSectionRequest req) {
        return eventService.addSection(jwt, id, req);
    }

    @PostMapping("/api/events/{id}/staff")
    @ResponseStatus(HttpStatus.CREATED)
    public StaffResponse addStaff(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id,
                                  @Valid @RequestBody AddStaffRequest req) {
        return eventService.addStaff(jwt, id, req);
    }
}