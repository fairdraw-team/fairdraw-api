package com.fairdraw.api.common;

import org.springframework.http.HttpStatus;

public final class Phones {

    private Phones() {}

    /** 010-1234-5678, +82 10-1234-5678, 821012345678 → 모두 821012345678 */
    public static String normalize(String raw) {
        if (raw == null) return null;
        String digits = raw.replaceAll("[^0-9]", "");
        if (digits.startsWith("0")) {
            digits = "82" + digits.substring(1);
        }
        if (!digits.matches("^82\\d{9,10}$")) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_PHONE", "휴대폰 번호 형식이 올바르지 않습니다");
        }
        return digits;
    }
}