package com.fairdraw.api.payment;

import com.fairdraw.api.common.ApiException;
import java.time.Instant;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {

    /** 본문에서 "paymentId": "..." 만 꺼냄 (본문은 글자 그대로 둬야 서명이 맞으므로 파싱하지 않음) */
    private static final Pattern PAYMENT_ID = Pattern.compile("\"paymentId\"\\s*:\\s*\"([^\"]+)\"");

    private final PaymentSyncService paymentSyncService;
    private final String webhookSecret;

    public WebhookController(PaymentSyncService paymentSyncService,
                             @Value("${app.portone.webhook-secret:}") String webhookSecret) {
        this.paymentSyncService = paymentSyncService;
        this.webhookSecret = webhookSecret;
    }

    /** 포트원이 결제 상태를 알려 주는 곳. 토큰 없이 들어오고 서명으로 검사한다 */
    @PostMapping("/portone")
    public ResponseEntity<Void> portone(
            @RequestBody String payload,
            @RequestHeader(value = "webhook-id", required = false) String id,
            @RequestHeader(value = "webhook-timestamp", required = false) String timestamp,
            @RequestHeader(value = "webhook-signature", required = false) String signature) {

        // ① 서명 검사 (시크릿이 비어 있으면 무조건 거절)
        if (webhookSecret.isBlank()
                || !WebhookVerifier.verify(webhookSecret, id, timestamp, signature, payload, Instant.now())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_SIGNATURE", "웹훅 서명이 맞지 않습니다");
        }

        // ② paymentId가 없으면(모르는 type 등) 그냥 200
        Matcher m = PAYMENT_ID.matcher(payload);
        if (!m.find()) {
            log.info("paymentId 없는 웹훅 id={}", id);
            return ResponseEntity.ok().build();
        }

        // ③ 결제 확인. 결과가 무엇이든 200 (아니면 포트원이 다시 보냄)
        String paymentId = m.group(1);
        PaymentSyncService.Result r = paymentSyncService.sync(paymentId);
        log.info("웹훅 처리 paymentId={} 결과={}", paymentId, r);
        return ResponseEntity.ok().build();
    }
}