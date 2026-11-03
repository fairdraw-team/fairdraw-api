package com.fairdraw.api.stub;

import com.fairdraw.api.common.ApiException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/tickets")
public class TicketController {

    /** 5주차: 내 티켓 목록 (QR 코드 포함) */
    @GetMapping("/me")
    public List<TicketResponse> myTickets() {
        throw ApiException.notImplemented();
    }

    /** 5주차: 스태프가 입장할 때 QR 스캔 */
    @PostMapping("/scan")
    public ScanResponse scan(@RequestBody ScanRequest req) {
        throw ApiException.notImplemented();
    }

    public record TicketResponse(UUID id, UUID eventId, String eventTitle, String sectionName,
                                 OffsetDateTime startsAt, String code, String status) {}
    public record ScanRequest(UUID eventId, String code) {}
    public record ScanResponse(String result, String sectionName, OffsetDateTime scannedAt) {}
}