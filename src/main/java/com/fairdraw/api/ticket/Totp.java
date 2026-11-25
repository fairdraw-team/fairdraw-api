package com.fairdraw.api.ticket;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

/** 티켓 QR의 6자리 숫자 (RFC 6238: SHA1, 30초, 6자리) */
public final class Totp {

    private static final SecureRandom RANDOM = new SecureRandom();

    private Totp() {}

    /** 티켓 발급 때 쓸 새 비밀값: 0~9, a~f로 된 40글자 (tickets.code에 저장) */
    public static String newSecretHex() {
        byte[] secret = new byte[20];
        RANDOM.nextBytes(secret);
        return HexFormat.of().formatHex(secret);
    }

    /** 비밀값과 시각(초)으로 6자리 숫자를 계산 */
    public static String codeAt(byte[] secret, long epochSeconds) {
        try {
            byte[] counter = ByteBuffer.allocate(8).putLong(epochSeconds / 30).array();
            Mac mac = Mac.getInstance("HmacSHA1");
            mac.init(new SecretKeySpec(secret, "HmacSHA1"));
            byte[] h = mac.doFinal(counter);
            int o = h[h.length - 1] & 0x0f;
            int bin = ((h[o] & 0x7f) << 24) | ((h[o + 1] & 0xff) << 16)
                    | ((h[o + 2] & 0xff) << 8) | (h[o + 3] & 0xff);
            return String.format("%06d", bin % 1_000_000);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    /** 지금, 30초 전, 30초 뒤 중 하나와 같으면 통과 (휴대폰 시계 오차를 봐줌) */
    public static boolean matches(String secretHex, String token, long nowSeconds) {
        byte[] secret = HexFormat.of().parseHex(secretHex);
        for (long t : new long[] {nowSeconds - 30, nowSeconds, nowSeconds + 30}) {
            if (codeAt(secret, t).equals(token)) return true;
        }
        return false;
    }
}