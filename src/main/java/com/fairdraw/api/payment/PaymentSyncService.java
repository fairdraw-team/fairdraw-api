package com.fairdraw.api.payment;

import com.fairdraw.api.payment.PortOneClient.PortOnePayment;
import com.fairdraw.api.ticket.TicketService;
import java.time.OffsetDateTime;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 한 건을 포트원 기준으로 맞추는 곳. 결제 확인 API, 웹훅, 만료 처리가 같이 씀.
 * 에러를 던지지 않고 결과 값을 돌려줌 (던지면 취소 기록까지 되돌려지므로)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentSyncService {

    /** UNKNOWN: 포트원에 물어보다 실패함(네트워크·키 문제). 이번엔 판단하지 않음 */
    public enum Result { PAID, NOT_PAID, AMOUNT_MISMATCH, BOOKING_EXPIRED, NOT_FOUND, UNKNOWN }

    private final PaymentRepository paymentRepository;
    private final BookingRepository bookingRepository;
    private final PortOneClient portOneClient;
    private final TicketService ticketService;

    @Transactional
    public Result sync(String paymentId) {
        // ① 잠금과 함께 읽기
        Payment p = paymentRepository.findByPaymentIdForUpdate(paymentId).orElse(null);
        if (p == null) return Result.NOT_FOUND;

        // ② 이미 paid면 끝 (두 번 불러도 같은 결과 = 멱등)
        if (PaymentStatus.PAID.equals(p.getStatus())) return Result.PAID;

        // ③ 포트원에 직접 조회
        PortOnePayment po;
        try {
            po = portOneClient.get(paymentId);
        } catch (Exception e) {
            log.error("포트원 조회 실패 paymentId={}", paymentId, e);
            return Result.UNKNOWN;
        }
        if (po == null || !"PAID".equals(po.status())) {
            if (po != null && ("FAILED".equals(po.status()) || "CANCELLED".equals(po.status()))) {
                p.setStatus(PaymentStatus.FAILED);
            }
            return Result.NOT_PAID;
        }

        // ④ 금액이 다르면 취소
        if (po.amount() == null || po.amount().total() != p.getAmount()) {
            safeCancel(paymentId, "결제 금액 불일치");
            p.setStatus(PaymentStatus.CANCELLED);
            return Result.AMOUNT_MISMATCH;
        }

        // ⑤ 예매가 이미 결제 대기가 아니면(기한 지나 만료 등) 돈은 돌려주고 자리는 안 줌
        Booking b = bookingRepository.findByIdForUpdate(p.getBookingId()).orElse(null);
        if (b == null || !BookingStatus.PENDING.equals(b.getStatus())) {
            safeCancel(paymentId, "결제 기한 만료");
            p.setStatus(PaymentStatus.CANCELLED);
            return Result.BOOKING_EXPIRED;
        }

        // ⑥ 통과: 결제·예매 paid, 티켓 발급
        OffsetDateTime now = OffsetDateTime.now();
        p.setStatus(PaymentStatus.PAID);
        p.setPaidAt(now);
        b.setStatus(BookingStatus.PAID);
        b.setPaidAt(now);
        ticketService.issue(b);
        return Result.PAID;
    }

    /** 취소가 실패해도 기록은 남기고 로그로 알림 (콘솔에서 수동 취소) */
    private void safeCancel(String paymentId, String reason) {
        try {
            portOneClient.cancel(paymentId, reason);
        } catch (Exception e) {
            log.error("포트원 결제 취소 실패 paymentId={} reason={}", paymentId, reason, e);
        }
    }
}