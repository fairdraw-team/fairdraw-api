package com.fairdraw.api.stub;

import com.fairdraw.api.common.ApiException;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    /** 4주차: 포트원이 "결제 됐어요"라고 알려 주는 곳. 4주차에 출입증 없이 열고 서명을 검사한다. */
    @PostMapping("/portone")
    public void portone(@RequestBody String payload) {
        throw ApiException.notImplemented();
    }
}