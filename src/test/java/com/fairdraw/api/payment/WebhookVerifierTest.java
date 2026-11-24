package com.fairdraw.api.payment;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;

class WebhookVerifierTest {

    // 테스트용 가짜 시크릿 (진짜 시크릿 아님)
    static final String RAW = Base64.getEncoder()
            .encodeToString("test-secret-1234567890".getBytes(StandardCharsets.UTF_8));
    static final String SECRET = "whsec_" + RAW;
    static final String ID = "msg_abc";
    static final String BODY = "{\"type\":\"Transaction.Paid\",\"data\":{\"paymentId\":\"fd123\"}}";
    static final Instant NOW = Instant.ofEpochSecond(1_800_000_000L);
    static final String TS = String.valueOf(NOW.getEpochSecond());

    /** 포트원이 하는 것과 같은 방법으로 서명을 만듦 */
    static String sign(String body, String ts) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(Base64.getDecoder().decode(RAW), "HmacSHA256"));
        byte[] sig = mac.doFinal((ID + "." + ts + "." + body).getBytes(StandardCharsets.UTF_8));
        return "v1," + Base64.getEncoder().encodeToString(sig);
    }

    @Test
    void 올바른_서명은_통과() throws Exception {
        assertTrue(WebhookVerifier.verify(SECRET, ID, TS, sign(BODY, TS), BODY, NOW));
    }

    @Test
    void 서명이_여러_개여도_하나만_맞으면_통과() throws Exception {
        String header = "v1,AAAA " + sign(BODY, TS);
        assertTrue(WebhookVerifier.verify(SECRET, ID, TS, header, BODY, NOW));
    }

    @Test
    void 본문이_바뀌면_거절() throws Exception {
        String other = BODY.replace("fd123", "fd999");
        assertFalse(WebhookVerifier.verify(SECRET, ID, TS, sign(BODY, TS), other, NOW));
    }

    @Test
    void 오분_넘게_지난_웹훅은_거절() throws Exception {
        String old = String.valueOf(NOW.getEpochSecond() - 301);
        assertFalse(WebhookVerifier.verify(SECRET, ID, old, sign(BODY, old), BODY, NOW));
    }

    @Test
    void 헤더가_없거나_이상하면_거절() {
        assertFalse(WebhookVerifier.verify(SECRET, ID, TS, null, BODY, NOW));
        assertFalse(WebhookVerifier.verify(SECRET, ID, "abc", "v1,xxx", BODY, NOW));
    }
}