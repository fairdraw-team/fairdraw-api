package com.fairdraw.api.payment;

import com.fairdraw.api.common.ApiException;
import com.fairdraw.api.payment.PaymentDtos.*;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/events/{eventId}")
@RequiredArgsConstructor
public class BookingController {

    private final PaymentService paymentService;

    /** 관객: 이 공연에서 내 예매 */
    @GetMapping("/booking/me")
    public MyBookingResponse myBooking(@AuthenticationPrincipal Jwt jwt,
                                       @PathVariable UUID eventId) {
        return paymentService.myBooking(jwt, eventId);
    }

    /** 주최자: 결제 열기. 당첨자마다 예매가 생기고 공연이 paying */
    @PostMapping("/payment/open")
    public PaymentStatsResponse open(@AuthenticationPrincipal Jwt jwt,
                                     @PathVariable UUID eventId) {
        return paymentService.open(jwt, eventId);
    }

    /** 주최자: 구역별 결제 현황 */
    @GetMapping("/payment/stats")
    public PaymentStatsResponse stats(@AuthenticationPrincipal Jwt jwt,
                                      @PathVariable UUID eventId) {
        return paymentService.stats(jwt, eventId);
    }

    /** 주최자: 예매 명단. status를 비우면 전부 */
    @GetMapping("/bookings")
    public BookingPage bookings(@AuthenticationPrincipal Jwt jwt,
                                @PathVariable UUID eventId,
                                @RequestParam(required = false) String status,
                                @RequestParam(defaultValue = "0") int page,
                                @RequestParam(defaultValue = "20") int size) {
        throw ApiException.notImplemented();
    }

    /** 주최자: 미결제 지금 처리 (만료·승계) */
    @PostMapping("/payment/process")
    public PaymentStatsResponse process(@AuthenticationPrincipal Jwt jwt,
                                        @PathVariable UUID eventId) {
        throw ApiException.notImplemented();
    }
}