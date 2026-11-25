package com.fairdraw.api.payment;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 예매 한 건. 당첨자(draw) 또는 넘겨받은 사람(succession) */
@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID eventId;
    private UUID userId;
    private UUID sectionId;
    private UUID drawResultId;
    private UUID entryId;
    private String status;              // BookingStatus
    private Integer amount;             // 결제 열기 때 정해서 바꾸지 않음
    private OffsetDateTime payDueAt;
    private String source;              // BookingSource
    private OffsetDateTime paidAt;
    private OffsetDateTime createdAt = OffsetDateTime.now();
}