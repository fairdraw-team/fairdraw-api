package com.fairdraw.api.draw;

import com.fairdraw.api.draw.DrawDtos.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class DrawController {

    private final DrawService drawService;

    // ===================== 2주차: 마감·봉인·현황 =====================

    @PostMapping("/api/events/{eventId}/close")
    public SealResponse close(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId) {
        return drawService.seal(jwt, eventId);
    }

    @GetMapping("/api/events/{eventId}/stats")
    public StatsResponse stats(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId) {
        return drawService.stats(jwt, eventId);
    }

    @GetMapping("/api/public/events/{eventId}/seal")
    public SealResponse seal(@PathVariable UUID eventId) {
        return drawService.publicSeal(eventId);
    }

    // ===================== 3주차: 추첨 =====================

    /** 주최자: 추첨 실행 → 공연 drawn. 응답은 공개 추첨 정보와 같은 모양 */
    @PostMapping("/api/events/{eventId}/draw")
    public DrawInfoResponse draw(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId) {
        return drawService.draw(jwt, eventId);
    }

    /** 관객: 내 결과 */
    @GetMapping("/api/events/{eventId}/draw/me")
    public MyDrawResult myResult(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId) {
        return drawService.myResult(jwt, eventId);
    }

    /** 주최자: 결과 명단. result를 빼면 전부 (won / waitlisted / lost) */
    @GetMapping("/api/events/{eventId}/draw/results")
    public DrawResultPage results(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId,
                                  @RequestParam(required = false) String result,
                                  @RequestParam(defaultValue = "0") int page,
                                  @RequestParam(defaultValue = "20") int size) {
        return drawService.results(jwt, eventId, result, page, size);
    }

    /** 누구나: 공개 추첨 정보 */
    @GetMapping("/api/public/events/{eventId}/draw")
    public DrawInfoResponse publicDraw(@PathVariable UUID eventId) {
        return drawService.publicDraw(eventId);
    }

    /** 누구나: 추첨 순서대로 줄 선 제비 목록 */
    @GetMapping("/api/public/events/{eventId}/draw/ballots")
    public PublicBallotsResponse publicBallots(@PathVariable UUID eventId) {
        return drawService.publicBallots(eventId);
    }
}