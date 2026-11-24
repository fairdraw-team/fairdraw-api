package com.fairdraw.api.payment;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** 포트원 웹훅 서명 검사 (Standard Webhooks 방식, HMAC-SHA256) */
public final class WebhookVerifier {

    private WebhookVerifier() {}

    /**
     * @param secret          포트원 콘솔의 웹훅 시크릿 (whsec_로 시작해도 됨)
     * @param id              헤더 webhook-id
     * @param timestamp       헤더 webhook-timestamp (초 단위 숫자)
     * @param signatureHeader 헤더 webhook-signature ("v1,<base64> v1,<base64>")
     * @param body            본문 글자 그대로 (JSON으로 바꾸기 전)
     * @param now             지금 시각 (테스트에서 바꿔 넣으려고 인자로 받음)
     */
    public static boolean verify(String secret, String id, String timestamp,
                                 String signatureHeader, String body, Instant now) {
        try {
            // 5분 넘게 지난 웹훅은 거절 (옛 웹훅을 복사해 다시 보내는 것을 막음)
            long sent = Long.parseLong(timestamp);
            if (Math.abs(now.getEpochSecond() - sent) > 300) return false;

            String raw = secret.startsWith("whsec_") ? secret.substring(6) : secret;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(Base64.getDecoder().decode(raw), "HmacSHA256"));
            byte[] expected = mac.doFinal(
                    (id + "." + timestamp + "." + body).getBytes(StandardCharsets.UTF_8));

            // 헤더 모양: "v1,<base64> v1,<base64>" (공백으로 구분, 여러 개일 수 있음)
            for (String part : signatureHeader.split(" ")) {
                if (!part.startsWith("v1,")) continue;
                byte[] given = Base64.getDecoder().decode(part.substring(3));
                if (MessageDigest.isEqual(expected, given)) return true;
            }
            return false;
        } catch (Exception e) {
            return false;  // 헤더가 없거나 모양이 틀리면 전부 거절
        }
    }
}