package com.fairdraw.api.payment;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 결제 시도 한 건. paymentId는 포트원에 넘기는 우리 결제 번호 */
@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID bookingId;
    private String paymentId;
    private Integer amount;
    private String status;              // PaymentStatus
    private OffsetDateTime paidAt;
    private OffsetDateTime createdAt = OffsetDateTime.now();
}