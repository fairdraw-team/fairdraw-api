package com.fairdraw.api.ticket;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 스캔 기록. 엉뚱한 QR(invalid)이면 ticketId가 비어 있음 */
@Entity
@Table(name = "scan_logs")
@Getter
@Setter
@NoArgsConstructor
public class ScanLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID ticketId;
    private UUID staffId;
    private UUID eventId;
    private UUID scannedBy;
    private String result;              // ScanResult
    private OffsetDateTime scannedAt = OffsetDateTime.now();
}