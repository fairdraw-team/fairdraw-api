package com.fairdraw.api.payment;

import com.fairdraw.api.common.ApiException;
import com.fairdraw.api.payment.PaymentDtos.TickResponse;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal")
public class InternalController {

    private final TickService tickService;
    private final String cronSecret;

    public InternalController(TickService tickService,
                              @Value("${app.cron-secret:}") String cronSecret) {
        this.tickService = tickService;
        this.cronSecret = cronSecret;
    }

    /** Supabase Cron이 5분마다 부름. X-Cron-Secret이 틀리면 401 */
    @PostMapping("/tick")
    public TickResponse tick(@RequestHeader(value = "X-Cron-Secret", required = false) String secret) {
        if (cronSecret.isBlank() || secret == null
                || !MessageDigest.isEqual(cronSecret.getBytes(StandardCharsets.UTF_8),
                                          secret.getBytes(StandardCharsets.UTF_8))) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "잘못된 요청입니다");
        }
        return tickService.tick();
    }
}