package com.fairdraw.api.ticket;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class TotpTest {

    // RFC 6238 부록 B의 SHA1 시험용 비밀값 "12345678901234567890"
    static final byte[] SECRET = "12345678901234567890".getBytes(StandardCharsets.US_ASCII);
    static final String SECRET_HEX = "3132333435363738393031323334353637383930";

    @Test
    void RFC_공식_정답과_같다() {
        // RFC는 8자리 정답을 주므로, 뒤 6자리가 우리 값
        assertEquals("287082", Totp.codeAt(SECRET, 59L));          // 94287082
        assertEquals("081804", Totp.codeAt(SECRET, 1111111109L));  // 07081804
        assertEquals("005924", Totp.codeAt(SECRET, 1234567890L));  // 89005924
    }

    @Test
    void 앞뒤_30초까지는_통과하고_그_밖은_거절() {
        long now = 1234567890L;
        String code = Totp.codeAt(SECRET, now);
        assertTrue(Totp.matches(SECRET_HEX, code, now));
        assertTrue(Totp.matches(SECRET_HEX, code, now + 30));
        assertTrue(Totp.matches(SECRET_HEX, code, now - 30));
        assertFalse(Totp.matches(SECRET_HEX, code, now + 120));
    }

    @Test
    void 새_비밀값은_40글자_hex이고_매번_다르다() {
        String a = Totp.newSecretHex();
        String b = Totp.newSecretHex();
        assertEquals(40, a.length());
        assertTrue(a.matches("[0-9a-f]{40}"));
        assertNotEquals(a, b);
    }
}