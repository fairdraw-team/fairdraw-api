package com.fairdraw.api.payment;

import com.fairdraw.api.common.ApiException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    /** 포트원이 결제 상태를 알려 주는 곳. 토큰 없이 들어오고 서명으로 검사한다 */
    @PostMapping("/portone")
    public void portone(@RequestBody String payload) {
        throw ApiException.notImplemented();
    }
}