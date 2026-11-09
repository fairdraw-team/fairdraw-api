package com.fairdraw.api.entry;

import com.fairdraw.api.entry.EntryDtos.*;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events/{eventId}/entries")
@RequiredArgsConstructor
public class EntryController {

    private final EntryService entryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MyEntryResponse create(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId,
                                  @Valid @RequestBody CreateEntryRequest req) {
        return entryService.create(jwt, eventId, req);
    }

    @GetMapping("/me")
    public MyEntryResponse mine(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId) {
        return entryService.getMine(jwt, eventId);
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId) {
        entryService.cancelMine(jwt, eventId);
    }

    /** 주최자용 명단: ?page=0&size=20 */
    @GetMapping
    public EntryPage list(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size) {
        return entryService.listForOrganizer(jwt, eventId, page, size);
    }
}