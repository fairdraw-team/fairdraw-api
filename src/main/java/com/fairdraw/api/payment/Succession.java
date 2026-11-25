package com.fairdraw.api.payment;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 자리 넘기기 기록: 만료된 예매 → 대기자 응모 → 새 예매 */
@Entity
@Table(name = "successions")
@Getter
@Setter
@NoArgsConstructor
public class Succession {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID fromBookingId;
    private UUID toEntryId;
    private UUID toBookingId;
    private OffsetDateTime createdAt = OffsetDateTime.now();
}