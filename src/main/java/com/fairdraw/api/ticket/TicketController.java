package com.fairdraw.api.ticket;

import com.fairdraw.api.common.ApiException;
import com.fairdraw.api.ticket.TicketDtos.*;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
public class TicketController {

    /** 관객: 내 티켓 목록 (QR용 code 포함) */
    @GetMapping("/api/tickets/me")
    public List<TicketResponse> myTickets(@AuthenticationPrincipal Jwt jwt) {
        throw ApiException.notImplemented();
    }

    /** 주최자·스태프: 입장할 때 QR 스캔 */
    @PostMapping("/api/tickets/scan")
    public ScanResponse scan(@AuthenticationPrincipal Jwt jwt,
                             @RequestBody ScanRequest req) {
        throw ApiException.notImplemented();
    }

    /** 주최자·스태프: 스캔 화면용 요약 (권한 확인 겸함) */
    @GetMapping("/api/events/{eventId}/scan/summary")
    public ScanSummaryResponse scanSummary(@AuthenticationPrincipal Jwt jwt,
                                           @PathVariable UUID eventId) {
        throw ApiException.notImplemented();
    }
}