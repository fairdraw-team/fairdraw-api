package com.fairdraw.api.stub;

import com.fairdraw.api.common.ApiException;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events/{eventId}/entries")
public class EntryController {

    /** 2주차: 응모하기. 구역별 제비를 몇 장씩 넣을지 보낸다. */
    @PostMapping
    public EntryResponse apply(@PathVariable UUID eventId, @RequestBody CreateEntryRequest req) {
        throw ApiException.notImplemented();
    }

    /** 2주차: 내 응모 보기 */
    @GetMapping("/me")
    public EntryResponse myEntry(@PathVariable UUID eventId) {
        throw ApiException.notImplemented();
    }

    public record BallotRequest(UUID sectionId, int count) {}
    public record CreateEntryRequest(UUID teamId, List<BallotRequest> ballots) {}
    public record EntryResponse(UUID id, UUID eventId, String status, List<BallotRequest> ballots) {}
}