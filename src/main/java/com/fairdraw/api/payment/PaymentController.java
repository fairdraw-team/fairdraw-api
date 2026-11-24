package com.fairdraw.api.payment;

import com.fairdraw.api.common.ApiException;
import com.fairdraw.api.payment.PaymentDtos.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    /** 결제창 열기 전 준비: 화면은 받은 값으로 포트원 결제창을 연다 */
    @PostMapping("/prepare")
    public PrepareResponse prepare(@AuthenticationPrincipal Jwt jwt,
                                   @RequestBody PrepareRequest req) {
        throw ApiException.notImplemented();
    }

    /** 결제창이 닫힌 뒤: 백엔드가 포트원에 실제 결제 여부를 확인 */
    @PostMapping("/complete")
    public CompleteResponse complete(@AuthenticationPrincipal Jwt jwt,
                                     @RequestBody CompleteRequest req) {
        throw ApiException.notImplemented();
    }
}