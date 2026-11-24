package com.fairdraw.api.ticket;

import java.time.OffsetDateTime;
import java.util.UUID;

public final class TicketDtos {

    private TicketDtos() {}

    /** ★ usedAt 추가 */
    public record TicketResponse(UUID id, UUID eventId, String eventTitle, String sectionName,
                                 OffsetDateTime startsAt, String code, String status,
                                 OffsetDateTime usedAt) {}

    public record ScanRequest(UUID eventId, String code) {}

    /** ★ holderName, message 추가 */
    public record ScanResponse(String result, String sectionName, OffsetDateTime scannedAt,
                               String holderName, String message) {}

    public record ScanSummaryResponse(UUID eventId, String eventTitle, long issued, long entered) {}
}