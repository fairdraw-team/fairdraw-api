package com.fairdraw.api.draw;

import com.fairdraw.api.common.ApiException;
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

    /** 주최자: 응모 마감 + 명단 봉인 */
    @PostMapping("/api/events/{eventId}/close")
    public SealResponse close(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId) {
        return drawService.seal(jwt, eventId);
    }

    /** 주최자: 응모 현황 */
    @GetMapping("/api/events/{eventId}/stats")
    public StatsResponse stats(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID eventId) {
        return drawService.stats(jwt, eventId);
    }

    /** 누구나(로그인 없이): 봉인 정보 */
    @GetMapping("/api/public/events/{eventId}/seal")
    public SealResponse seal(@PathVariable UUID eventId) {
        return drawService.publicSeal(eventId);
    }

    /** 3주차: 추첨 */
    @PostMapping("/api/events/{eventId}/draw")
    public void draw(@PathVariable UUID eventId) {
        throw ApiException.notImplemented();
    }
}