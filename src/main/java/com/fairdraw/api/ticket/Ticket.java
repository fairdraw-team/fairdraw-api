package com.fairdraw.api.ticket;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 티켓. 예매 하나에 하나. code는 QR 6자리를 만드는 비밀값(40글자 hex) */
@Entity
@Table(name = "tickets")
@Getter
@Setter
@NoArgsConstructor
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID bookingId;
    private String code;
    private String status;              // TicketStatus
    private OffsetDateTime usedAt;
    private OffsetDateTime createdAt = OffsetDateTime.now();
}