package com.fairdraw.api.payment;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public final class PaymentDtos {

    private PaymentDtos() {}

    // ===== 결제 준비 / 결제 확인 (기존 주소, ★ eventId 추가) =====
    public record PrepareRequest(UUID bookingId) {}

    public record PrepareResponse(String paymentId, int amount, String orderName,
                                  String storeId, String channelKey) {}

    public record CompleteRequest(String paymentId) {}

    public record CompleteResponse(String paymentId, String status, UUID bookingId, UUID eventId) {}

    // ===== 내 예매 (R-3) =====
    public record MyBookingResponse(UUID bookingId, UUID eventId, String status, String sectionName,
                                    int amount, OffsetDateTime payDueAt, String source,
                                    OffsetDateTime paidAt) {}

    // ===== 주최자 결제 현황 =====
    public record SectionPaymentStat(UUID sectionId, String name, int capacity,
                                     long paid, long pending, long expired,
                                     long succeeded, long waitlistLeft) {}

    public record PaymentStatsResponse(UUID eventId, String status, OffsetDateTime paymentDueAt,
                                       long issued, long entered,
                                       List<SectionPaymentStat> sections) {}

    // ===== 주최자 예매 명단 =====
    public record BookingRow(UUID bookingId, String realName, String phone, String sectionName,
                             String status, int amount, OffsetDateTime payDueAt,
                             String source, OffsetDateTime paidAt) {}

    public record BookingPage(List<BookingRow> content, int page, int size,
                              long totalElements, int totalPages) {}

    // ===== Cron tick =====
    public record TickResponse(int events, int expired, int succeeded) {}
}