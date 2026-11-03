package com.fairdraw.api.stub;

import com.fairdraw.api.common.ApiException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events/{eventId}")
public class DrawController {

    /** 3주차: 응모 마감 (응모 중 → 마감) */
    @PostMapping("/close")
    public CloseResponse close(@PathVariable UUID eventId) {
        throw ApiException.notImplemented();
    }

    /** 3주차: 추첨 실행 */
    @PostMapping("/draw")
    public DrawResponse draw(@PathVariable UUID eventId) {
        throw ApiException.notImplemented();
    }

    /** 3주차: 추첨 결과 보기 */
    @GetMapping("/draw/results")
    public List<DrawResultResponse> results(@PathVariable UUID eventId) {
        throw ApiException.notImplemented();
    }

    public record CloseResponse(UUID eventId, String status, int entryCount) {}
    public record DrawResponse(UUID drawId, UUID eventId, String seed, OffsetDateTime executedAt, int winnerCount) {}
    public record DrawResultResponse(UUID entryId, UUID sectionId, int rank) {}
}