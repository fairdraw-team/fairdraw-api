package com.fairdraw.api.stub;

import com.fairdraw.api.common.ApiException;
import java.util.UUID;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    /** 4주차: 결제창 열기 전 준비. 화면은 받은 값으로 포트원 결제창을 연다. */
    @PostMapping("/prepare")
    public PrepareResponse prepare(@RequestBody PrepareRequest req) {
        throw ApiException.notImplemented();
    }

    /** 4주차: 결제창이 끝난 뒤 백엔드가 포트원에 실제 결제 여부를 확인 */
    @PostMapping("/complete")
    public CompleteResponse complete(@RequestBody CompleteRequest req) {
        throw ApiException.notImplemented();
    }

    public record PrepareRequest(UUID bookingId) {}
    public record PrepareResponse(String paymentId, int amount, String orderName, String storeId, String channelKey) {}
    public record CompleteRequest(String paymentId) {}
    public record CompleteResponse(String paymentId, String status, UUID bookingId) {}
}