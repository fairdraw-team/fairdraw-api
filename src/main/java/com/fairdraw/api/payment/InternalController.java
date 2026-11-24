package com.fairdraw.api.payment;

import com.fairdraw.api.common.ApiException;
import com.fairdraw.api.payment.PaymentDtos.TickResponse;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal")
public class InternalController {

    /** Supabase Cron이 5분마다 부름. X-Cron-Secret이 틀리면 401 */
    @PostMapping("/tick")
    public TickResponse tick(@RequestHeader(value = "X-Cron-Secret", required = false) String secret) {
        throw ApiException.notImplemented();
    }
}